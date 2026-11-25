package br.kerberos.portal.provisioning.adapter.out.keycloak;

import br.kerberos.portal.provisioning.application.ProvisioningFailedException;
import br.kerberos.portal.provisioning.application.port.out.IdentityProvisioningPort;
import br.kerberos.portal.provisioning.domain.LoginMethod;
import br.kerberos.portal.provisioning.domain.SystemSpec;
import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.web.client.RestClientResponseException;

/**
 * Provisiona pela Admin REST API do Keycloak (ADR 0004). Cada passo verifica o estado atual antes de agir:
 * rodar de novo cria o que falta e corrige o que divergir, sem duplicar.
 */
public class KeycloakIdentityProvisioningAdapter implements IdentityProvisioningPort {

    static final String LDAP_MAPPER_PROVIDER = "organization-membership-ldap-mapper";
    private static final String MASKED_SECRET = "**********";

    private final KeycloakAdminClient keycloak;
    private final String loginTheme;
    private final Map<String, String> smtp;

    public KeycloakIdentityProvisioningAdapter(KeycloakAdminClient keycloak, String loginTheme, Map<String, String> smtp) {
        this.keycloak = keycloak;
        this.loginTheme = loginTheme;
        this.smtp = smtp;
    }

    // ------------------------------------------------------------------ sistema

    @Override
    public void ensureSystem(SystemSpec system) {
        String realm = system.realmName();
        try {
            ensureRealm(system);
            for (String role : system.allRoles()) {
                ensureRealmRole(realm, role);
            }
            ensureDefaultRole(realm);
            ensureSpaClient(system);
        } catch (RestClientResponseException e) {
            throw failure("criar o sistema " + realm, e);
        }
    }

    private void ensureRealm(SystemSpec system) {
        Map<String, Object> settings = GoldenRealmTemplate.settings(system.realmName(), system.displayName(), loginTheme, smtp);
        if (keycloak.get("/realms/{realm}", system.realmName()).isEmpty()) {
            keycloak.post("/realms", settings);
        } else {
            keycloak.put("/realms/{realm}", settings, system.realmName());
        }
    }

    private void ensureRealmRole(String realm, String role) {
        if (keycloak.get("/realms/{realm}/roles/{role}", realm, role).isEmpty()) {
            keycloak.post("/realms/{realm}/roles", Map.of("name", role, "description", describe(role)), realm);
        }
    }

    private static String describe(String role) {
        return switch (role) {
            case "admin" -> "Administra o sistema e os usuários da organização";
            case "gestor" -> "Gerencia dados e operações da organização";
            case "usuario" -> "Acesso básico ao sistema";
            default -> "Role do sistema (criada pelo portal)";
        };
    }

    /** Roles padrão de todo usuário novo: offline_access, uma_authorization e usuario — igual ao Terraform. */
    private void ensureDefaultRole(String realm) {
        String defaultRoles = "default-roles-" + realm;
        Map<String, Object> role = keycloak.get("/realms/{realm}/roles/{role}", realm, GoldenRealmTemplate.DEFAULT_ROLE).orElseThrow();
        // Adicionar uma composta que já existe não tem efeito: idempotente
        keycloak.post("/realms/{realm}/roles/{role}/composites", List.of(role), realm, defaultRoles);

        List<Map<String, Object>> extra = keycloak.list("/realms/{realm}/roles/{role}/composites", realm, defaultRoles).stream()
                .filter(r -> !GoldenRealmTemplate.DEFAULT_ROLES.contains(String.valueOf(r.get("name"))) || Boolean.TRUE.equals(r.get("clientRole")))
                .toList();
        if (!extra.isEmpty()) {
            keycloak.deleteWithBody("/realms/{realm}/roles/{role}/composites", extra, realm, defaultRoles);
        }
    }

    private void ensureSpaClient(SystemSpec system) {
        String realm = system.realmName();
        String base = system.webBaseUrl().toString();
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("clientId", system.webClientId());
        client.put("name", system.displayName());
        client.put("protocol", "openid-connect");
        client.put("publicClient", true);
        client.put("standardFlowEnabled", true);
        client.put("implicitFlowEnabled", false);
        client.put("directAccessGrantsEnabled", false);
        client.put("serviceAccountsEnabled", false);
        client.put("rootUrl", base);
        client.put("baseUrl", base);
        client.put("redirectUris", List.of(base + "/*"));
        client.put("webOrigins", List.of(base));
        client.put("attributes", Map.of(
                "pkce.code.challenge.method", "S256",
                "post.logout.redirect.uris", base + "/*"));

        Optional<Map<String, Object>> existing = findClient(realm, system.webClientId());
        String clientUuid;
        if (existing.isEmpty()) {
            client.put("defaultClientScopes", GoldenRealmTemplate.SPA_DEFAULT_SCOPES);
            client.put("optionalClientScopes", GoldenRealmTemplate.SPA_OPTIONAL_SCOPES);
            client.put("protocolMappers", List.of(audienceMapper(system.apiAudience())));
            clientUuid = keycloak.post("/realms/{realm}/clients", client, realm)
                    .orElseGet(() -> (String) findClient(realm, system.webClientId()).orElseThrow().get("id"));
        } else {
            clientUuid = (String) existing.get().get("id");
            keycloak.put("/realms/{realm}/clients/{id}", client, realm, clientUuid);
        }
        ensureClientScopes(realm, clientUuid);
        ensureAudienceMapper(realm, clientUuid, system.apiAudience());
    }

    private Optional<Map<String, Object>> findClient(String realm, String clientId) {
        return keycloak.list("/realms/{realm}/clients?clientId={clientId}", realm, clientId).stream().findFirst();
    }

    private void ensureClientScopes(String realm, String clientUuid) {
        Map<String, String> scopeIds = new HashMap<>();
        for (Map<String, Object> scope : keycloak.list("/realms/{realm}/client-scopes", realm)) {
            scopeIds.put((String) scope.get("name"), (String) scope.get("id"));
        }
        List<String> currentOptional = names(keycloak.list("/realms/{realm}/clients/{id}/optional-client-scopes", realm, clientUuid));
        List<String> currentDefault = names(keycloak.list("/realms/{realm}/clients/{id}/default-client-scopes", realm, clientUuid));

        for (String scope : GoldenRealmTemplate.SPA_DEFAULT_SCOPES) {
            String id = Objects.requireNonNull(scopeIds.get(scope), "client scope ausente no realm: " + scope);
            if (currentOptional.contains(scope)) {
                keycloak.delete("/realms/{realm}/clients/{id}/optional-client-scopes/{scope}", realm, clientUuid, id);
            }
            if (!currentDefault.contains(scope)) {
                keycloak.put("/realms/{realm}/clients/{id}/default-client-scopes/{scope}", Map.of(), realm, clientUuid, id);
            }
        }
    }

    private static List<String> names(List<Map<String, Object>> scopes) {
        return scopes.stream().map(s -> (String) s.get("name")).toList();
    }

    private void ensureAudienceMapper(String realm, String clientUuid, String audience) {
        boolean present = keycloak.list("/realms/{realm}/clients/{id}/protocol-mappers/models", realm, clientUuid).stream()
                .anyMatch(m -> ("audience-" + audience).equals(m.get("name")));
        if (!present) {
            keycloak.post("/realms/{realm}/clients/{id}/protocol-mappers/models", audienceMapper(audience), realm, clientUuid);
        }
    }

    private static Map<String, Object> audienceMapper(String audience) {
        return Map.of(
                "name", "audience-" + audience,
                "protocol", "openid-connect",
                "protocolMapper", "oidc-audience-mapper",
                "config", Map.of(
                        "included.custom.audience", audience,
                        "access.token.claim", "true",
                        "id.token.claim", "false",
                        "introspection.token.claim", "true"));
    }

    // ------------------------------------------------------------------ cliente (tenant)

    @Override
    public void ensureTenant(SystemSpec system, TenantSpec tenant, Optional<String> secret) {
        String realm = system.realmName();
        try {
            String orgId = ensureOrganization(realm, tenant);
            switch (tenant.loginMethod()) {
                case LoginMethod.Oidc oidc -> ensureOidcProvider(realm, orgId, tenant, oidc, secret);
                case LoginMethod.Ldap ldap -> ensureLdap(realm, tenant, ldap, secret);
                case LoginMethod.LocalUsers local -> { /* nada além da organização */ }
            }
        } catch (RestClientResponseException e) {
            throw failure("cadastrar o cliente " + tenant.alias() + " em " + realm, e);
        }
    }

    private String ensureOrganization(String realm, TenantSpec tenant) {
        Optional<Map<String, Object>> existing = findOrganization(realm, tenant.alias().value());
        if (existing.isEmpty()) {
            Map<String, Object> org = new LinkedHashMap<>();
            org.put("name", tenant.name());
            org.put("alias", tenant.alias().value());
            org.put("enabled", true);
            org.put("domains", tenant.domains().stream().map(d -> Map.of("name", d, "verified", true)).toList());
            return keycloak.post("/realms/{realm}/organizations", org, realm)
                    .orElseGet(() -> (String) findOrganization(realm, tenant.alias().value()).orElseThrow().get("id"));
        }

        Map<String, Object> org = new LinkedHashMap<>(existing.get());
        org.put("name", tenant.name());
        org.put("enabled", true);
        // Mantém o roteamento de IdP dos domínios que continuam; adiciona os novos
        Map<String, Map<String, Object>> current = new HashMap<>();
        for (Object d : (List<?>) org.getOrDefault("domains", List.of())) {
            @SuppressWarnings("unchecked") Map<String, Object> domain = (Map<String, Object>) d;
            current.put(String.valueOf(domain.get("name")), domain);
        }
        List<Map<String, Object>> domains = new ArrayList<>();
        for (String name : tenant.domains()) {
            Map<String, Object> domain = new LinkedHashMap<>(current.getOrDefault(name, Map.of("name", name)));
            domain.put("verified", true);
            domains.add(domain);
        }
        org.put("domains", domains);
        String id = (String) org.get("id");
        keycloak.put("/realms/{realm}/organizations/{id}", org, realm, id);
        return id;
    }

    private Optional<Map<String, Object>> findOrganization(String realm, String alias) {
        return keycloak.list("/realms/{realm}/organizations?briefRepresentation=false&max=1000", realm).stream()
                .filter(o -> alias.equals(o.get("alias")))
                .findFirst();
    }

    private void ensureOidcProvider(String realm, String orgId, TenantSpec tenant, LoginMethod.Oidc oidc, Optional<String> secret) {
        String alias = tenant.alias().identityProviderAlias();

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("issuer", str(oidc.issuer()));
        config.put("authorizationUrl", str(oidc.authorizationUrl()));
        config.put("tokenUrl", str(oidc.tokenUrl()));
        config.put("jwksUrl", str(oidc.jwksUrl()));
        config.put("useJwksUrl", "true");
        config.put("validateSignature", "true");
        if (oidc.userInfoUrl() != null) {
            config.put("userInfoUrl", str(oidc.userInfoUrl()));
        }
        if (oidc.logoutUrl() != null) {
            config.put("logoutUrl", str(oidc.logoutUrl()));
        }
        config.put("clientId", oidc.clientId());
        // Sem segredo novo, o valor mascarado mantém o segredo atual no Keycloak
        config.put("clientSecret", secret.orElse(MASKED_SECRET));
        config.put("clientAuthMethod", "client_secret_post");
        config.put("defaultScope", "openid profile email");
        config.put("syncMode", "FORCE");

        Map<String, Object> idp = new LinkedHashMap<>();
        idp.put("alias", alias);
        idp.put("displayName", tenant.name());
        idp.put("providerId", "oidc");
        idp.put("enabled", true);
        idp.put("trustEmail", true);
        idp.put("hideOnLogin", true);
        idp.put("storeToken", false);
        idp.put("firstBrokerLoginFlowAlias", "first broker login");
        idp.put("config", config);

        if (keycloak.get("/realms/{realm}/identity-provider/instances/{alias}", realm, alias).isEmpty()) {
            keycloak.post("/realms/{realm}/identity-provider/instances", idp, realm);
        } else {
            keycloak.put("/realms/{realm}/identity-provider/instances/{alias}", idp, realm, alias);
        }

        // Liga o IdP à organização e direciona o domínio principal para ele
        boolean linked = keycloak.list("/realms/{realm}/organizations/{id}/identity-providers", realm, orgId).stream()
                .anyMatch(p -> alias.equals(p.get("alias")));
        if (!linked) {
            keycloak.postIgnoringConflict("/realms/{realm}/organizations/{id}/identity-providers", alias, realm, orgId);
        }
        routePrimaryDomain(realm, orgId, tenant.primaryDomain(), alias);
    }

    private void routePrimaryDomain(String realm, String orgId, String domainName, String idpAlias) {
        Map<String, Object> org = new LinkedHashMap<>(keycloak.get("/realms/{realm}/organizations/{id}", realm, orgId).orElseThrow());
        List<Map<String, Object>> domains = new ArrayList<>();
        boolean changed = false;
        for (Object d : (List<?>) org.getOrDefault("domains", List.of())) {
            @SuppressWarnings("unchecked") Map<String, Object> domain = new LinkedHashMap<>((Map<String, Object>) d);
            if (domainName.equals(domain.get("name"))
                    && (!idpAlias.equals(domain.get("identityProviderAlias")) || !Boolean.TRUE.equals(domain.get("autoRedirect")))) {
                domain.put("identityProviderAlias", idpAlias);
                domain.put("autoRedirect", true);
                changed = true;
            }
            domains.add(domain);
        }
        if (changed) {
            org.put("domains", domains);
            keycloak.put("/realms/{realm}/organizations/{id}", org, realm, orgId);
        }
    }

    private void ensureLdap(String realm, TenantSpec tenant, LoginMethod.Ldap ldap, Optional<String> secret) {
        String name = "ldap-" + tenant.alias();
        String realmId = (String) keycloak.get("/realms/{realm}", realm).orElseThrow().get("id");
        boolean ad = ldap.vendor() == LoginMethod.Ldap.Vendor.AD;

        Map<String, List<String>> config = new LinkedHashMap<>();
        config.put("enabled", List.of("true"));
        config.put("priority", List.of("0"));
        config.put("vendor", List.of(ad ? "ad" : "other"));
        config.put("connectionUrl", List.of(ldap.connectionUrl().toString()));
        config.put("usersDn", List.of(ldap.usersDn()));
        config.put("bindDn", List.of(ldap.bindDn()));
        config.put("bindCredential", List.of(secret.orElse(MASKED_SECRET)));
        config.put("authType", List.of("simple"));
        config.put("editMode", List.of("READ_ONLY"));
        config.put("importEnabled", List.of("true"));
        config.put("syncRegistrations", List.of("false"));
        config.put("trustEmail", List.of("true"));
        config.put("usernameLDAPAttribute", List.of(ad ? "sAMAccountName" : "uid"));
        config.put("rdnLDAPAttribute", List.of(ad ? "cn" : "uid"));
        config.put("uuidLDAPAttribute", List.of(ad ? "objectGUID" : "entryUUID"));
        config.put("userObjectClasses", List.of(ad ? "person, organizationalPerson, user" : "inetOrgPerson, organizationalPerson"));
        config.put("searchScope", List.of("1"));
        config.put("pagination", List.of("false"));
        config.put("connectionTimeout", List.of("5000"));
        config.put("readTimeout", List.of("10000"));
        config.put("cachePolicy", List.of("DEFAULT"));

        Map<String, Object> component = new LinkedHashMap<>();
        component.put("name", name);
        component.put("providerId", "ldap");
        component.put("providerType", "org.keycloak.storage.UserStorageProvider");
        component.put("parentId", realmId);
        component.put("config", config);

        Optional<Map<String, Object>> existing = keycloak
                .list("/realms/{realm}/components?type=org.keycloak.storage.UserStorageProvider&name={name}", realm, name)
                .stream().findFirst();
        String ldapId;
        if (existing.isEmpty()) {
            ldapId = keycloak.post("/realms/{realm}/components", component, realm).orElseThrow();
        } else {
            ldapId = (String) existing.get().get("id");
            component.put("id", ldapId);
            keycloak.put("/realms/{realm}/components/{id}", component, realm, ldapId);
        }

        boolean mapperPresent = keycloak.list("/realms/{realm}/components?parent={parent}", realm, ldapId).stream()
                .anyMatch(m -> LDAP_MAPPER_PROVIDER.equals(m.get("providerId")));
        if (!mapperPresent) {
            keycloak.post("/realms/{realm}/components", Map.of(
                    "name", "organization-membership",
                    "providerId", LDAP_MAPPER_PROVIDER,
                    "providerType", "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
                    "parentId", ldapId,
                    "config", Map.of("organization.alias", List.of(tenant.alias().value()))), realm);
        }
    }

    private static String str(URI uri) {
        return uri == null ? null : uri.toString();
    }

    private static ProvisioningFailedException failure(String what, RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        String detail = body == null || body.isBlank() ? "" : " — " + body.substring(0, Math.min(300, body.length()));
        return new ProvisioningFailedException(
                "Keycloak recusou ao " + what + " (HTTP " + e.getStatusCode().value() + ")" + detail, e);
    }
}

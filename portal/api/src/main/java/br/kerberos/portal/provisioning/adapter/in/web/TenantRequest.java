package br.kerberos.portal.provisioning.adapter.in.web;

import br.kerberos.portal.provisioning.domain.InvalidSpecException;
import br.kerberos.portal.provisioning.domain.LoginMethod;
import br.kerberos.portal.provisioning.domain.TenantAlias;
import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.util.List;

/**
 * Cadastro de cliente. {@code secret} é o client secret do IdP (OIDC) ou a senha de bind (LDAP):
 * segue direto para o Keycloak e não é gravado pelo portal.
 */
public record TenantRequest(
        String alias, String name, List<String> domains, String loginMethod, Oidc oidc, Ldap ldap, String secret) {

    public record Oidc(
            String issuer, String authorizationUrl, String tokenUrl, String jwksUrl, String userInfoUrl, String logoutUrl, String clientId) {}

    public record Ldap(String connectionUrl, String usersDn, String bindDn, String vendor) {}

    TenantSpec toSpec() {
        LoginMethod method = switch (loginMethod == null ? "local" : loginMethod) {
            case "local" -> new LoginMethod.LocalUsers();
            case "oidc" -> {
                if (oidc == null) throw new InvalidSpecException("Informe os dados do IdP OIDC do cliente");
                yield new LoginMethod.Oidc(
                        SystemRequest.uri(oidc.issuer()),
                        SystemRequest.uri(oidc.authorizationUrl()),
                        SystemRequest.uri(oidc.tokenUrl()),
                        SystemRequest.uri(oidc.jwksUrl()),
                        SystemRequest.uri(oidc.userInfoUrl()),
                        SystemRequest.uri(oidc.logoutUrl()),
                        oidc.clientId());
            }
            case "ldap" -> {
                if (ldap == null) throw new InvalidSpecException("Informe os dados do LDAP do cliente");
                yield new LoginMethod.Ldap(
                        SystemRequest.uri(ldap.connectionUrl()),
                        ldap.usersDn(),
                        ldap.bindDn(),
                        "ad".equalsIgnoreCase(ldap.vendor()) ? LoginMethod.Ldap.Vendor.AD : LoginMethod.Ldap.Vendor.OPENLDAP);
            }
            default -> throw new InvalidSpecException("Forma de login desconhecida: " + loginMethod + " (use local, oidc ou ldap)");
        };
        return new TenantSpec(new TenantAlias(alias), name, domains, method);
    }
}

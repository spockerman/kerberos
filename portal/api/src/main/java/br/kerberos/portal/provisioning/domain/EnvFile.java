package br.kerberos.portal.provisioning.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuração pronta para o projeto do sistema: o time copia para o .env e começa pelas telas.
 * Só dados públicos — o client web é público (PKCE) e a API valida tokens pela chave pública.
 */
public record EnvFile(Map<String, String> variables) {

    public static EnvFile forSystem(SystemSpec system, String keycloakPublicUrl) {
        String issuer = keycloakPublicUrl + "/realms/" + system.realmName();
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("KEYCLOAK_URL", keycloakPublicUrl);
        vars.put("KEYCLOAK_REALM", system.realmName());
        vars.put("KEYCLOAK_CLIENT_ID", system.webClientId());
        vars.put("KEYCLOAK_SCOPE", system.multiTenant() ? "openid profile email organization" : "openid profile email");
        vars.put("KEYCLOAK_ISSUER", issuer);
        vars.put("KEYCLOAK_JWK_SET_URI", issuer + "/protocol/openid-connect/certs");
        vars.put("API_AUDIENCE", system.apiAudience());
        vars.put("CORS_ALLOWED_ORIGINS", system.webBaseUrl().toString());
        return new EnvFile(java.util.Collections.unmodifiableMap(vars));
    }

    public String render(String header) {
        return "# " + header + "\n"
                + variables.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n"))
                + "\n";
    }
}

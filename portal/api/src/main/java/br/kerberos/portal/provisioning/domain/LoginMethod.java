package br.kerberos.portal.provisioning.domain;

import java.net.URI;

/**
 * Como os usuários de um cliente entram. Ordem de preferência da plataforma:
 * IdP OIDC do cliente → LDAP do cliente → usuários locais. Segredos não fazem parte daqui.
 */
public sealed interface LoginMethod {

    String code();

    /** Usuários criados no próprio Keycloak. */
    record LocalUsers() implements LoginMethod {
        @Override
        public String code() {
            return "local";
        }
    }

    /** IdP OIDC do cliente (Entra ID, Google Workspace, Okta...). */
    record Oidc(URI issuer, URI authorizationUrl, URI tokenUrl, URI jwksUrl, URI userInfoUrl, URI logoutUrl, String clientId)
            implements LoginMethod {

        public Oidc {
            require(issuer, "issuer");
            require(authorizationUrl, "URL de autorização");
            require(tokenUrl, "URL de token");
            require(jwksUrl, "URL das chaves (JWKS)");
            if (clientId == null || clientId.isBlank()) {
                throw new InvalidSpecException("client_id do IdP do cliente obrigatório");
            }
        }

        private static void require(URI url, String name) {
            if (url == null || url.getScheme() == null || !url.getScheme().startsWith("http")) {
                throw new InvalidSpecException("IdP OIDC: " + name + " obrigatória e precisa ser http(s)");
            }
        }

        @Override
        public String code() {
            return "oidc";
        }
    }

    /** LDAP/AD do cliente, somente leitura. */
    record Ldap(URI connectionUrl, String usersDn, String bindDn, Vendor vendor) implements LoginMethod {

        public enum Vendor { OPENLDAP, AD }

        public Ldap {
            if (connectionUrl == null || !("ldap".equals(connectionUrl.getScheme()) || "ldaps".equals(connectionUrl.getScheme()))) {
                throw new InvalidSpecException("LDAP: URL de conexão precisa ser ldap:// ou ldaps://");
            }
            if (usersDn == null || usersDn.isBlank() || bindDn == null || bindDn.isBlank()) {
                throw new InvalidSpecException("LDAP: DN dos usuários e DN de bind são obrigatórios");
            }
            vendor = vendor == null ? Vendor.OPENLDAP : vendor;
        }

        @Override
        public String code() {
            return "ldap";
        }
    }
}

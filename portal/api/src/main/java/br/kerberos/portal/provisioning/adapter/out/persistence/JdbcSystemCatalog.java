package br.kerberos.portal.provisioning.adapter.out.persistence;

import br.kerberos.portal.provisioning.application.port.out.SystemCatalogPort;
import br.kerberos.portal.provisioning.domain.LoginMethod;
import br.kerberos.portal.provisioning.domain.SystemSlug;
import br.kerberos.portal.provisioning.domain.SystemSpec;
import br.kerberos.portal.provisioning.domain.TenantAlias;
import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Catálogo no Postgres do portal (tabelas criadas pelo Flyway). */
public class JdbcSystemCatalog implements SystemCatalogPort {

    private final JdbcClient jdbc;

    public JdbcSystemCatalog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Stamped<SystemSpec>> findSystem(SystemSlug slug) {
        return jdbc.sql("SELECT * FROM systems WHERE slug = :slug").param("slug", slug.value()).query(this::system).optional();
    }

    @Override
    public List<Stamped<SystemSpec>> listSystems() {
        return jdbc.sql("SELECT * FROM systems ORDER BY display_name").query(this::system).list();
    }

    @Override
    public void saveSystem(SystemSpec s, Instant at, String actor) {
        jdbc.sql("""
                INSERT INTO systems (slug, display_name, web_base_url, extra_roles, multi_tenant, created_at, created_by, updated_at, updated_by)
                VALUES (:slug, :name, :url, :roles, :mt, :at, :actor, :at, :actor)
                ON CONFLICT (slug) DO UPDATE SET display_name = :name, web_base_url = :url, extra_roles = :roles,
                    multi_tenant = :mt, updated_at = :at, updated_by = :actor
                """)
                .param("slug", s.slug().value())
                .param("name", s.displayName())
                .param("url", s.webBaseUrl().toString())
                .param("roles", String.join(",", s.extraRoles()))
                .param("mt", s.multiTenant())
                .param("at", Timestamp.from(at))
                .param("actor", actor)
                .update();
    }

    @Override
    public List<Stamped<TenantSpec>> listTenants(SystemSlug slug) {
        return jdbc.sql("SELECT * FROM tenants WHERE system_slug = :slug ORDER BY name")
                .param("slug", slug.value())
                .query(this::tenant)
                .list();
    }

    @Override
    public void saveTenant(SystemSlug slug, TenantSpec t, Instant at, String actor) {
        LoginMethod.Oidc oidc = t.loginMethod() instanceof LoginMethod.Oidc o ? o : null;
        LoginMethod.Ldap ldap = t.loginMethod() instanceof LoginMethod.Ldap l ? l : null;
        jdbc.sql("""
                INSERT INTO tenants (system_slug, alias, name, domains, login_method,
                    oidc_issuer, oidc_authorization_url, oidc_token_url, oidc_jwks_url, oidc_user_info_url, oidc_logout_url, oidc_client_id,
                    ldap_connection_url, ldap_users_dn, ldap_bind_dn, ldap_vendor,
                    created_at, created_by, updated_at, updated_by)
                VALUES (:system, :alias, :name, :domains, :method,
                    :issuer, :authz, :token, :jwks, :userinfo, :logout, :clientId,
                    :ldapUrl, :usersDn, :bindDn, :vendor,
                    :at, :actor, :at, :actor)
                ON CONFLICT (system_slug, alias) DO UPDATE SET name = :name, domains = :domains, login_method = :method,
                    oidc_issuer = :issuer, oidc_authorization_url = :authz, oidc_token_url = :token, oidc_jwks_url = :jwks,
                    oidc_user_info_url = :userinfo, oidc_logout_url = :logout, oidc_client_id = :clientId,
                    ldap_connection_url = :ldapUrl, ldap_users_dn = :usersDn, ldap_bind_dn = :bindDn, ldap_vendor = :vendor,
                    updated_at = :at, updated_by = :actor
                """)
                .param("system", slug.value())
                .param("alias", t.alias().value())
                .param("name", t.name())
                .param("domains", String.join(",", t.domains()))
                .param("method", t.loginMethod().code())
                .param("issuer", oidc == null ? null : str(oidc.issuer()))
                .param("authz", oidc == null ? null : str(oidc.authorizationUrl()))
                .param("token", oidc == null ? null : str(oidc.tokenUrl()))
                .param("jwks", oidc == null ? null : str(oidc.jwksUrl()))
                .param("userinfo", oidc == null ? null : str(oidc.userInfoUrl()))
                .param("logout", oidc == null ? null : str(oidc.logoutUrl()))
                .param("clientId", oidc == null ? null : oidc.clientId())
                .param("ldapUrl", ldap == null ? null : str(ldap.connectionUrl()))
                .param("usersDn", ldap == null ? null : ldap.usersDn())
                .param("bindDn", ldap == null ? null : ldap.bindDn())
                .param("vendor", ldap == null ? null : ldap.vendor().name())
                .param("at", Timestamp.from(at))
                .param("actor", actor)
                .update();
    }

    private Stamped<SystemSpec> system(ResultSet rs, int row) throws SQLException {
        String roles = rs.getString("extra_roles");
        SystemSpec spec = new SystemSpec(
                new SystemSlug(rs.getString("slug")),
                rs.getString("display_name"),
                URI.create(rs.getString("web_base_url")),
                roles == null || roles.isBlank() ? List.of() : Arrays.asList(roles.split(",")),
                rs.getBoolean("multi_tenant"));
        return new Stamped<>(spec, rs.getTimestamp("updated_at").toInstant(), rs.getString("updated_by"));
    }

    private Stamped<TenantSpec> tenant(ResultSet rs, int row) throws SQLException {
        LoginMethod method = switch (rs.getString("login_method")) {
            case "oidc" -> new LoginMethod.Oidc(
                    uri(rs.getString("oidc_issuer")),
                    uri(rs.getString("oidc_authorization_url")),
                    uri(rs.getString("oidc_token_url")),
                    uri(rs.getString("oidc_jwks_url")),
                    uri(rs.getString("oidc_user_info_url")),
                    uri(rs.getString("oidc_logout_url")),
                    rs.getString("oidc_client_id"));
            case "ldap" -> new LoginMethod.Ldap(
                    uri(rs.getString("ldap_connection_url")),
                    rs.getString("ldap_users_dn"),
                    rs.getString("ldap_bind_dn"),
                    LoginMethod.Ldap.Vendor.valueOf(rs.getString("ldap_vendor")));
            default -> new LoginMethod.LocalUsers();
        };
        TenantSpec spec = new TenantSpec(
                new TenantAlias(rs.getString("alias")),
                rs.getString("name"),
                Arrays.asList(rs.getString("domains").split(",")),
                method);
        return new Stamped<>(spec, rs.getTimestamp("updated_at").toInstant(), rs.getString("updated_by"));
    }

    private static String str(URI uri) {
        return uri == null ? null : uri.toString();
    }

    private static URI uri(String value) {
        return value == null ? null : URI.create(value);
    }
}

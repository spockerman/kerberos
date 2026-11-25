-- Catálogo dos sistemas e clientes criados pelo portal, e a trilha de auditoria.
-- Segredos (client secret de IdP, senha de LDAP) NUNCA são gravados aqui.

CREATE TABLE systems (
    slug            VARCHAR(41)  PRIMARY KEY,
    display_name    VARCHAR(120) NOT NULL,
    web_base_url    VARCHAR(500) NOT NULL,
    extra_roles     VARCHAR(1000) NOT NULL DEFAULT '',
    multi_tenant    BOOLEAN      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(120) NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    updated_by      VARCHAR(120) NOT NULL
);

CREATE TABLE tenants (
    system_slug             VARCHAR(41)  NOT NULL REFERENCES systems (slug),
    alias                   VARCHAR(63)  NOT NULL,
    name                    VARCHAR(120) NOT NULL,
    domains                 VARCHAR(1000) NOT NULL,
    login_method            VARCHAR(10)  NOT NULL,
    oidc_issuer             VARCHAR(500),
    oidc_authorization_url  VARCHAR(500),
    oidc_token_url          VARCHAR(500),
    oidc_jwks_url           VARCHAR(500),
    oidc_user_info_url      VARCHAR(500),
    oidc_logout_url         VARCHAR(500),
    oidc_client_id          VARCHAR(200),
    ldap_connection_url     VARCHAR(500),
    ldap_users_dn           VARCHAR(500),
    ldap_bind_dn            VARCHAR(500),
    ldap_vendor             VARCHAR(10),
    created_at              TIMESTAMPTZ  NOT NULL,
    created_by              VARCHAR(120) NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    updated_by              VARCHAR(120) NOT NULL,
    PRIMARY KEY (system_slug, alias)
);

CREATE TABLE audit_trail (
    id          BIGSERIAL    PRIMARY KEY,
    at          TIMESTAMPTZ  NOT NULL,
    actor       VARCHAR(120) NOT NULL,
    action      VARCHAR(40)  NOT NULL,
    target      VARCHAR(200) NOT NULL,
    outcome     VARCHAR(10)  NOT NULL,
    detail      VARCHAR(2000)
);

CREATE INDEX audit_trail_at ON audit_trail (at DESC);

#!/usr/bin/env python3
"""
Teste de fumaça ponta a ponta do ambiente local (docker compose up).

Faz o login real pelo tema (Authorization Code + PKCE, como o navegador faria), troca o código
por token e verifica: tema aplicado, claim `organization`, audience, roles e o isolamento de tenant na API.

Uso: python3 scripts/smoke-test.py   (só biblioteca padrão do Python 3)
"""
import base64
import hashlib
import html
import json
import os
import re
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request

KEYCLOAK = os.environ.get("KEYCLOAK_URL", "http://localhost:8080")
REALM = os.environ.get("REALM", "demo")
CLIENT_ID = os.environ.get("CLIENT_ID", "demo-web")
REDIRECT_URI = os.environ.get("REDIRECT_URI", "http://localhost:3000/")
API = os.environ.get("API_URL", "http://localhost:8081")
WEB = os.environ.get("WEB_URL", "http://localhost:3000")
PASSWORD = os.environ.get("DEMO_PASSWORD", "Senha@Demo2026!")
VERBOSE = os.environ.get("SMOKE_VERBOSE") == "1"

failures = []


def check(condition, message):
    print(("  ok   " if condition else "  FALHA ") + message)
    if not condition:
        failures.append(message)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Session:
    """Cliente HTTP mínimo com cookies por caminho. O http.cookiejar do Python recusa cookies de "localhost"
    (e os marcados Secure em HTTP), então guardamos os cookies aqui, como um navegador em dev faria.
    O caminho importa: cada realm do Keycloak usa cookies com o mesmo nome em /realms/<realm>/."""

    def __init__(self):
        self.opener = urllib.request.build_opener(NoRedirect())
        self.cookies = {}  # (nome, caminho) -> valor

    def open(self, req, timeout):
        path = urllib.parse.urlparse(req.full_url).path or "/"
        matching = sorted(
            ((name, cpath, value) for (name, cpath), value in self.cookies.items() if path.startswith(cpath)),
            key=lambda c: -len(c[1]),  # caminho mais específico primeiro, como no navegador
        )
        if matching:
            req.add_header("Cookie", "; ".join(f"{name}={value}" for name, _, value in matching))
        try:
            resp = self.opener.open(req, timeout=timeout)
        except urllib.error.HTTPError as e:
            self._store(e.headers, path)
            raise
        self._store(resp.headers, path)
        return resp

    def _store(self, headers, request_path):
        for header in headers.get_all("Set-Cookie") or []:
            parts = [p.strip() for p in header.split(";")]
            name, _, value = parts[0].partition("=")
            attrs = {k.lower(): v for k, _, v in (p.partition("=") for p in parts[1:])}
            cpath = attrs.get("path") or request_path.rsplit("/", 1)[0] or "/"
            expired = "max-age" in attrs and attrs["max-age"].lstrip("-").isdigit() and int(attrs["max-age"]) <= 0
            if value and not expired:
                self.cookies[(name, cpath)] = value
            else:
                self.cookies.pop((name, cpath), None)


def new_opener():
    return Session()


def request(opener, url, data=None, headers=None):
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers or {})
    if VERBOSE:
        print(f"       {'POST' if body else 'GET '} {urllib.parse.urlparse(url).path} {sorted(data) if data else ''}")
    try:
        resp = opener.open(req, timeout=20)
        with resp:
            return resp.status, dict(resp.headers), resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read().decode(errors="replace")


def kc_context_value(page, key):
    """Lê um valor do kcContext que o Keycloak injeta na página do tema (Keycloakify)."""
    if key == "pageId":
        match = re.search(r'kcContext\.pageId\s*=\s*"([^"]+)"', page)
    else:
        match = re.search(r'"%s"\s*:\s*"([^"]+)"' % re.escape(key), page)
    if not match:
        return None
    return html.unescape(match.group(1).replace("\\/", "/"))


def decode_jwt(token):
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload))


def form_action(page):
    """Ação do formulário da página: kcContext (tema kerberos) ou <form> HTML (tema padrão, ex.: IdP do cliente)."""
    action = kc_context_value(page, "loginAction")
    if action:
        return action
    match = re.search(r'<form[^>]*action="([^"]+)"', page)
    return html.unescape(match.group(1)) if match else None


def login(username, organization=None, expect_idp_host=None, realm=REALM, client_id=CLIENT_ID,
          redirect_uri=REDIRECT_URI, scope="openid profile email organization"):
    """Login como um navegador faria. Segue redirecionamentos (inclusive para o IdP do cliente).
    Retorna o access token."""
    opener = new_opener()
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    params = {
        "client_id": client_id,
        "redirect_uri": redirect_uri,
        "response_type": "code",
        "scope": scope,
        "state": secrets.token_urlsafe(8),
        "code_challenge": challenge,
        "code_challenge_method": "S256",
    }
    url = f"{KEYCLOAK}/realms/{realm}/protocol/openid-connect/auth?" + urllib.parse.urlencode(params)
    status, headers, page = request(opener, url)
    check(status == 200, f"[{username}] página de login abre (HTTP {status})")
    check("/login/kerberos/" in page, f"[{username}] página usa o tema 'kerberos'")

    code = None
    visited_idp = False
    last_message = None
    for _ in range(12):
        location = headers.get("Location") or headers.get("location")
        if status in (301, 302, 303) and location:
            url = urllib.parse.urljoin(url, location)
            if url.startswith(redirect_uri):
                code = urllib.parse.parse_qs(urllib.parse.urlparse(url).query).get("code", [None])[0]
                break
            if "/realms/" + realm + "/" not in url:
                visited_idp = True
            status, headers, page = request(opener, url)
            continue

        action = form_action(page)
        if VERBOSE:
            print(f"       ← {status} página={kc_context_value(page, 'pageId')} msg={kc_context_value(page, 'summary')}")
        if status != 200 or not action:
            summary = kc_context_value(page, "summary")
            check(False, f"[{username}] formulário encontrado (HTTP {status}, página {kc_context_value(page, 'pageId')}, mensagem: {summary})")
            return None
        message = kc_context_value(page, "summary")
        if message and message == last_message:
            check(False, f"[{username}] login recusado: {message}")
            return None
        last_message = message
        if kc_context_value(page, "pageId") == "select-organization.ftl":
            form = {"kc.org": organization}
        else:
            form = {"username": username, "password": PASSWORD, "credentialId": ""}
        url = urllib.parse.urljoin(url, action)
        status, headers, page = request(opener, url, form)

    check(code is not None, f"[{username}] login concluído e código recebido")
    if expect_idp_host is not None:
        check(visited_idp, f"[{username}] login passou pelo IdP do cliente")
    if code is None:
        return None

    status, _, body = request(
        opener,
        f"{KEYCLOAK}/realms/{realm}/protocol/openid-connect/token",
        {
            "grant_type": "authorization_code",
            "client_id": client_id,
            "code": code,
            "redirect_uri": redirect_uri,
            "code_verifier": verifier,
        },
    )
    check(status == 200, f"[{username}] troca do código por token (HTTP {status})")
    return json.loads(body)["access_token"] if status == 200 else None


def api(path, token=None, base=None, body=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    url = f"{base or API}{path}"
    if body is not None:
        headers["Content-Type"] = "application/json"
        req = urllib.request.Request(url, data=json.dumps(body).encode(), headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                status, text = resp.status, resp.read().decode()
        except urllib.error.HTTPError as e:
            status, text = e.code, e.read().decode(errors="replace")
    else:
        status, _, text = request(new_opener(), url, headers=headers)
    try:
        return status, json.loads(text)
    except ValueError:
        return status, text


PORTAL_API = os.environ.get("PORTAL_API_URL", "http://localhost:8082")
PORTAL_WEB = os.environ.get("PORTAL_WEB_URL", "http://localhost:3001")
KC_ADMIN = (os.environ.get("KC_ADMIN_USER", "admin"), os.environ.get("KC_ADMIN_PASSWORD", "admin"))

# Configurações do template golden que o realm criado pelo portal precisa ter iguais ao realm do Terraform
GOLDEN_KEYS = [
    "loginTheme", "organizationsEnabled", "sslRequired", "registrationAllowed", "resetPasswordAllowed", "rememberMe",
    "loginWithEmailAllowed", "duplicateEmailsAllowed", "verifyEmail", "passwordPolicy", "accessTokenLifespan",
    "ssoSessionIdleTimeout", "ssoSessionMaxLifespan", "revokeRefreshToken", "refreshTokenMaxReuse",
    "internationalizationEnabled", "supportedLocales", "defaultLocale", "bruteForceProtected", "permanentLockout",
    "failureFactor", "waitIncrementSeconds", "quickLoginCheckMilliSeconds", "minimumQuickLoginWaitSeconds",
    "maxFailureWaitSeconds", "maxDeltaTimeSeconds", "browserSecurityHeaders", "eventsEnabled", "eventsExpiration",
    "eventsListeners", "adminEventsEnabled", "adminEventsDetailsEnabled",
]


def admin_realm(token, realm):
    status, body = api(f"/admin/realms/{realm}", token, base=KEYCLOAK)
    return body if status == 200 else {}


def portal_checks():
    print("Portal — operador cria sistema e clientes")
    portal_login = dict(realm="plataforma", client_id="portal-web", redirect_uri=f"{PORTAL_WEB}/", scope="openid profile email")
    token = login("operador", **portal_login)
    if not token:
        return
    system = {"slug": "financeiro", "displayName": "Financeiro", "webBaseUrl": "http://localhost:3100",
              "roles": ["auditor"], "multiTenant": True}
    status, body = api("/api/systems", token, base=PORTAL_API, body=system)
    check(status == 200 and body.get("webClientId") == "financeiro-web", f"portal cria o sistema financeiro (HTTP {status}: {str(body)[:200]})")
    status, body = api("/api/systems", token, base=PORTAL_API, body=system)
    check(status == 200, f"repetir o cadastro é idempotente (HTTP {status})")
    status, env = api("/api/systems/financeiro/env", token, base=PORTAL_API)
    check(status == 200 and "KEYCLOAK_CLIENT_ID=financeiro-web" in str(env) and "API_AUDIENCE=financeiro-api" in str(env),
          f".env gerado com client e audience (HTTP {status})")

    idp = f"{KEYCLOAK}/realms/initech-idp/protocol/openid-connect"
    initech = {"alias": "initech", "name": "Initech", "domains": ["initech.test"], "loginMethod": "oidc",
               "oidc": {"issuer": f"{KEYCLOAK}/realms/initech-idp", "authorizationUrl": f"{idp}/auth", "tokenUrl": f"{idp}/token",
                        "jwksUrl": f"{idp}/certs", "userInfoUrl": f"{idp}/userinfo", "logoutUrl": f"{idp}/logout",
                        "clientId": "kerberos-broker"},
               "secret": "segredo-local-initech"}
    status, body = api("/api/systems/financeiro/tenants", token, base=PORTAL_API, body=initech)
    check(status == 200 and body.get("loginMethod") == "oidc", f"portal cadastra cliente Initech com IdP OIDC (HTTP {status}: {str(body)[:200]})")
    umbrella = {"alias": "umbrella", "name": "Umbrella", "domains": ["umbrella.test"], "loginMethod": "ldap",
                "ldap": {"connectionUrl": "ldap://ldap-umbrella:389", "usersDn": "ou=people,dc=umbrella,dc=test",
                         "bindDn": "cn=admin,dc=umbrella,dc=test", "vendor": "openldap"},
                "secret": "admin-umbrella"}
    status, body = api("/api/systems/financeiro/tenants", token, base=PORTAL_API, body=umbrella)
    check(status == 200 and body.get("loginMethod") == "ldap", f"portal cadastra cliente Umbrella com LDAP (HTTP {status}: {str(body)[:200]})")
    status, body = api("/api/systems/financeiro/tenants", token, base=PORTAL_API, body=initech | {"secret": None})
    check(status == 200, f"atualizar cliente sem reenviar o segredo funciona (HTTP {status})")

    status, audit = api("/api/audit", token, base=PORTAL_API)
    actions = {(e["action"], e["target"], e["actor"], e["outcome"]) for e in audit} if status == 200 else set()
    check(("SYSTEM_CREATED", "financeiro", "operador", "SUCCESS") in actions, "auditoria registra quem criou o sistema")
    check(not any("segredo" in (e.get("detail") or "") for e in audit or []), "auditoria não contém segredos")

    print("Sistema criado pelo portal — login dos clientes")
    app = dict(realm="financeiro", client_id="financeiro-web", redirect_uri="http://localhost:3100/")
    token_d = login("diego@initech.test", expect_idp_host="initech-idp", **app)
    if token_d:
        claims = decode_jwt(token_d)
        check(claims.get("organization") == ["initech"], f"[financeiro] organization = ['initech'] (veio {claims.get('organization')})")
        aud = claims.get("aud") if isinstance(claims.get("aud"), list) else [claims.get("aud")]
        check("financeiro-api" in aud, f"[financeiro] audience financeiro-api (veio {aud})")
    token_e = login("eva", **app)
    if token_e:
        claims = decode_jwt(token_e)
        check(claims.get("organization") == ["umbrella"], f"[financeiro] organization = ['umbrella'] (veio {claims.get('organization')})")
        check("usuario" in claims.get("realm_access", {}).get("roles", []), "[financeiro] role padrão 'usuario' no token")

    print("Portal — permissões")
    token_v = login("visitante", **portal_login)
    if token_v:
        status, _ = api("/api/systems", token_v, base=PORTAL_API, body=system | {"slug": "intruso"})
        check(status == 403, f"sem a role provisionador → 403 (HTTP {status})")
    status, _ = api("/api/systems", None, base=PORTAL_API)
    check(status == 401, f"portal sem token → 401 (HTTP {status})")

    print("Template golden — portal (Java) igual ao Terraform")
    status, _, body = request(new_opener(), f"{KEYCLOAK}/realms/master/protocol/openid-connect/token",
                              {"grant_type": "password", "client_id": "admin-cli", "username": KC_ADMIN[0], "password": KC_ADMIN[1]})
    admin_token = json.loads(body).get("access_token") if status == 200 else None
    if admin_token:
        terraform_realm, portal_realm = admin_realm(admin_token, "demo"), admin_realm(admin_token, "financeiro")
        diffs = [k for k in GOLDEN_KEYS if terraform_realm.get(k) != portal_realm.get(k)]
        def default_roles(realm):
            st, body = api(f"/admin/realms/{realm}/roles/default-roles-{realm}/composites", admin_token, base=KEYCLOAK)
            return sorted((r["name"], r.get("clientRole", False)) for r in body) if st == 200 else None
        if default_roles("demo") != default_roles("financeiro"):
            diffs.append(f"defaultRoles: tf={default_roles('demo')} portal={default_roles('financeiro')}")
        check(not diffs, "sem divergência: " + (", ".join(d if ":" in d else f"{d}: tf={terraform_realm.get(d)!r} portal={portal_realm.get(d)!r}" for d in diffs) or "ok"))
    else:
        check(False, f"token de admin para comparar realms (HTTP {status})")

    status, _, _ = request(new_opener(), PORTAL_WEB)
    check(status == 200, f"portal-web responde (HTTP {status})")


def main():
    print("Keycloak")
    status, _, body = request(new_opener(), f"{KEYCLOAK}/realms/{REALM}/.well-known/openid-configuration")
    check(status == 200, f"realm '{REALM}' publicado (HTTP {status})")

    print("Usuário ana (Acme, admin)")
    token = login("ana")
    if token:
        claims = decode_jwt(token)
        check(claims.get("organization") == ["acme"], f"claim organization = ['acme'] (veio {claims.get('organization')})")
        aud = claims.get("aud")
        aud = aud if isinstance(aud, list) else [aud]
        check("demo-api" in aud, f"audience inclui demo-api (veio {aud})")
        check("admin" in claims.get("realm_access", {}).get("roles", []), "role admin no token")
        status, me = api("/api/me", token)
        check(status == 200 and me.get("tenants") == ["acme"], f"API /api/me devolve tenant acme (HTTP {status})")
        status, _ = api("/api/tenants/acme/dashboard", token)
        check(status == 200, f"API permite o próprio tenant (HTTP {status})")
        status, _ = api("/api/tenants/globex/dashboard", token)
        check(status == 403, f"API bloqueia outro tenant (HTTP {status})")

    print("Usuário bruno (Globex, role padrão)")
    token = login("bruno")
    if token:
        claims = decode_jwt(token)
        check(claims.get("organization") == ["globex"], f"claim organization = ['globex'] (veio {claims.get('organization')})")
        check("usuario" in claims.get("realm_access", {}).get("roles", []), "role padrão 'usuario' no token")

    print("Usuária carla (Acme e Globex) escolhendo Globex")
    token = login("carla", organization="globex")
    if token:
        claims = decode_jwt(token)
        check(claims.get("organization") == ["globex"], f"claim organization = ['globex'] (veio {claims.get('organization')})")

    print("Usuário diego — cliente Initech, login pelo IdP do cliente (OIDC)")
    token = login("diego@initech.test", expect_idp_host="initech-idp")
    if token:
        claims = decode_jwt(token)
        check(claims.get("organization") == ["initech"], f"claim organization = ['initech'] (veio {claims.get('organization')})")
        check(claims.get("email") == "diego@initech.test", f"e-mail veio do IdP do cliente (veio {claims.get('email')})")
        status, _ = api("/api/tenants/initech/dashboard", token)
        check(status == 200, f"API permite o tenant initech (HTTP {status})")

    print("Usuária eva — cliente Umbrella, login pelo LDAP do cliente")
    token = login("eva")
    if token:
        claims = decode_jwt(token)
        check(claims.get("organization") == ["umbrella"], f"claim organization = ['umbrella'] (veio {claims.get('organization')})")
        check(claims.get("email") == "eva@umbrella.test", f"e-mail veio do LDAP (veio {claims.get('email')})")

    print("Segurança da API")
    status, _ = api("/api/me")
    check(status == 401, f"sem token → 401 (HTTP {status})")

    portal_checks()

    print("Front-end")
    status, _, _ = request(new_opener(), WEB)
    check(status == 200, f"demo-web responde (HTTP {status})")

    print()
    if failures:
        print(f"{len(failures)} verificação(ões) falharam.")
        sys.exit(1)
    print("Tudo certo.")


if __name__ == "__main__":
    main()

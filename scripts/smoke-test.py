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

failures = []


def check(condition, message):
    print(("  ok   " if condition else "  FALHA ") + message)
    if not condition:
        failures.append(message)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Session:
    """Cliente HTTP mínimo com cookies. O http.cookiejar do Python recusa cookies de "localhost"
    (e os marcados Secure em HTTP), então guardamos os cookies aqui, como um navegador em dev faria."""

    def __init__(self):
        self.opener = urllib.request.build_opener(NoRedirect())
        self.cookies = {}

    def open(self, req, timeout):
        if self.cookies:
            req.add_header("Cookie", "; ".join(f"{k}={v}" for k, v in self.cookies.items()))
        try:
            resp = self.opener.open(req, timeout=timeout)
        except urllib.error.HTTPError as e:
            self._store(e.headers)
            raise
        self._store(resp.headers)
        return resp

    def _store(self, headers):
        for header in headers.get_all("Set-Cookie") or []:
            name, _, rest = header.partition("=")
            value = rest.split(";", 1)[0]
            if value:
                self.cookies[name.strip()] = value
            else:
                self.cookies.pop(name.strip(), None)


def new_opener():
    return Session()


def request(opener, url, data=None, headers=None):
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers or {})
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


def login(username, organization=None):
    """Login pelo formulário do tema. Retorna o access token."""
    opener = new_opener()
    verifier = secrets.token_urlsafe(48)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    params = {
        "client_id": CLIENT_ID,
        "redirect_uri": REDIRECT_URI,
        "response_type": "code",
        "scope": "openid profile email organization",
        "state": secrets.token_urlsafe(8),
        "code_challenge": challenge,
        "code_challenge_method": "S256",
    }
    auth_url = f"{KEYCLOAK}/realms/{REALM}/protocol/openid-connect/auth?" + urllib.parse.urlencode(params)
    status, headers, page = request(opener, auth_url)
    check(status == 200, f"[{username}] página de login abre (HTTP {status})")
    check("/login/kerberos/" in page, f"[{username}] página usa o tema 'kerberos'")

    # Fluxo pode ter várias etapas (identity-first, senha, escolha de organização)
    for _ in range(5):
        action = kc_context_value(page, "loginAction")
        page_id = kc_context_value(page, "pageId")
        if not action:
            summary = kc_context_value(page, "summary")
            check(False, f"[{username}] formulário encontrado (página {page_id}, mensagem: {summary})")
            return None
        if page_id == "select-organization.ftl":
            form = {"kc.org": organization}
        else:
            form = {"username": username, "password": PASSWORD, "credentialId": ""}
        status, headers, page = request(opener, action, form)
        location = headers.get("Location") or headers.get("location") or ""
        if status in (302, 303) and location.startswith(REDIRECT_URI):
            code = urllib.parse.parse_qs(urllib.parse.urlparse(location).query).get("code", [None])[0]
            check(code is not None, f"[{username}] login concluído e código recebido")
            break
        if status in (302, 303):
            status, headers, page = request(opener, urllib.parse.urljoin(action, location))
    else:
        check(False, f"[{username}] login concluído em até 5 etapas")
        return None

    status, _, body = request(
        opener,
        f"{KEYCLOAK}/realms/{REALM}/protocol/openid-connect/token",
        {
            "grant_type": "authorization_code",
            "client_id": CLIENT_ID,
            "code": code,
            "redirect_uri": REDIRECT_URI,
            "code_verifier": verifier,
        },
    )
    check(status == 200, f"[{username}] troca do código por token (HTTP {status})")
    return json.loads(body)["access_token"] if status == 200 else None


def api(path, token=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    status, _, body = request(new_opener(), f"{API}{path}", headers=headers)
    try:
        return status, json.loads(body)
    except ValueError:
        return status, body


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

    print("Segurança da API")
    status, _ = api("/api/me")
    check(status == 401, f"sem token → 401 (HTTP {status})")

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

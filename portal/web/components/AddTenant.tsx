"use client";

import { useState, type FormEvent } from "react";
import type { PortalApi, TenantRequest } from "@/lib/portal-api";

type Method = TenantRequest["loginMethod"];

const METHODS: { value: Method; label: string }[] = [
    { value: "oidc", label: "IdP do cliente" },
    { value: "ldap", label: "LDAP" },
    { value: "local", label: "Usuários locais" }
];

/** Cadastro de cliente. A preferência da plataforma é o IdP do próprio cliente (Entra ID, Google, Okta). */
export function AddTenant({ api, slug, onAdded }: { api: PortalApi; slug: string; onAdded: () => void }) {
    const [method, setMethod] = useState<Method>("oidc");
    const [form, setForm] = useState<Record<string, string>>({ vendor: "openldap" });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string>();
    const [done, setDone] = useState<string>();

    const set = (key: string) => (e: { target: { value: string } }) => setForm(f => ({ ...f, [key]: e.target.value }));
    const v = (key: string) => form[key] ?? "";

    /** Preenche as URLs a partir do issuer (padrão OIDC/Keycloak; para Entra ID confira as URLs geradas). */
    function fillFromIssuer() {
        const issuer = v("issuer").replace(/\/$/, "");
        if (!issuer) return;
        setForm(f => ({
            ...f,
            authorizationUrl: `${issuer}/protocol/openid-connect/auth`,
            tokenUrl: `${issuer}/protocol/openid-connect/token`,
            jwksUrl: `${issuer}/protocol/openid-connect/certs`,
            userInfoUrl: `${issuer}/protocol/openid-connect/userinfo`,
            logoutUrl: `${issuer}/protocol/openid-connect/logout`
        }));
    }

    async function submit(e: FormEvent) {
        e.preventDefault();
        setSaving(true);
        setError(undefined);
        setDone(undefined);
        const request: TenantRequest = {
            alias: v("alias"),
            name: v("name"),
            domains: v("domains").split(",").map(d => d.trim()).filter(Boolean),
            loginMethod: method,
            secret: v("secret") || undefined
        };
        if (method === "oidc") {
            request.oidc = {
                issuer: v("issuer"),
                authorizationUrl: v("authorizationUrl"),
                tokenUrl: v("tokenUrl"),
                jwksUrl: v("jwksUrl"),
                userInfoUrl: v("userInfoUrl") || undefined,
                logoutUrl: v("logoutUrl") || undefined,
                clientId: v("clientId")
            };
        }
        if (method === "ldap") {
            request.ldap = {
                connectionUrl: v("connectionUrl"),
                usersDn: v("usersDn"),
                bindDn: v("bindDn"),
                vendor: v("vendor") === "ad" ? "ad" : "openldap"
            };
        }
        try {
            const tenant = await api.addTenant(slug, request);
            setDone(`Cliente ${tenant.name} cadastrado.`);
            setForm({ vendor: "openldap" });
            onAdded();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setSaving(false);
        }
    }

    return (
        <form className="card form" onSubmit={submit}>
            <h2>Novo cliente</h2>
            <div className="fields-2">
                <div className="field">
                    <label htmlFor="t-name">Nome</label>
                    <input id="t-name" required value={v("name")} onChange={set("name")} placeholder="Prefeitura de Osasco" />
                </div>
                <div className="field">
                    <label htmlFor="t-alias">Alias</label>
                    <input id="t-alias" required value={v("alias")} onChange={set("alias")} placeholder="osasco" />
                </div>
            </div>
            <div className="field">
                <label htmlFor="t-domains">Domínios de e-mail</label>
                <input id="t-domains" required value={v("domains")} onChange={set("domains")} placeholder="osasco.sp.gov.br" />
                <small>Separe por vírgula. Quem digitar um e-mail do primeiro domínio vai direto para o login do cliente.</small>
            </div>

            <div className="field">
                <label>Como os usuários entram</label>
                <div className="segmented" role="group" aria-label="Forma de login">
                    {METHODS.map(m => (
                        <button key={m.value} type="button" aria-pressed={method === m.value} onClick={() => setMethod(m.value)}>
                            {m.label}
                        </button>
                    ))}
                </div>
            </div>

            {method === "oidc" ? (
                <fieldset>
                    <legend>IdP OIDC do cliente</legend>
                    <div className="field">
                        <label htmlFor="t-issuer">Issuer</label>
                        <div className="row" style={{ flexWrap: "nowrap" }}>
                            <input id="t-issuer" style={{ flex: 1 }} required value={v("issuer")} onChange={set("issuer")} placeholder="https://login.microsoftonline.com/<tenant>/v2.0" />
                            <button type="button" className="btn btn--secondary btn--compact" onClick={fillFromIssuer}>
                                Preencher URLs
                            </button>
                        </div>
                    </div>
                    <div className="field">
                        <label htmlFor="t-authz">URL de autorização</label>
                        <input id="t-authz" required value={v("authorizationUrl")} onChange={set("authorizationUrl")} />
                    </div>
                    <div className="fields-2">
                        <div className="field">
                            <label htmlFor="t-token">URL de token</label>
                            <input id="t-token" required value={v("tokenUrl")} onChange={set("tokenUrl")} />
                        </div>
                        <div className="field">
                            <label htmlFor="t-jwks">URL das chaves (JWKS)</label>
                            <input id="t-jwks" required value={v("jwksUrl")} onChange={set("jwksUrl")} />
                        </div>
                    </div>
                    <div className="fields-2">
                        <div className="field">
                            <label htmlFor="t-client">Client ID</label>
                            <input id="t-client" required value={v("clientId")} onChange={set("clientId")} />
                        </div>
                        <div className="field">
                            <label htmlFor="t-secret">Client secret</label>
                            <input id="t-secret" type="password" autoComplete="off" value={v("secret")} onChange={set("secret")} />
                        </div>
                    </div>
                    <small className="muted">O segredo vai direto para o Keycloak e não fica guardado no portal. Ao atualizar, deixe em branco para manter.</small>
                </fieldset>
            ) : null}

            {method === "ldap" ? (
                <fieldset>
                    <legend>LDAP / Active Directory do cliente</legend>
                    <div className="fields-2">
                        <div className="field">
                            <label htmlFor="t-ldapurl">URL</label>
                            <input id="t-ldapurl" required value={v("connectionUrl")} onChange={set("connectionUrl")} placeholder="ldaps://ldap.cliente.com" />
                        </div>
                        <div className="field">
                            <label htmlFor="t-vendor">Tipo</label>
                            <select id="t-vendor" value={v("vendor")} onChange={set("vendor")}>
                                <option value="openldap">OpenLDAP</option>
                                <option value="ad">Active Directory</option>
                            </select>
                        </div>
                    </div>
                    <div className="field">
                        <label htmlFor="t-usersdn">DN dos usuários</label>
                        <input id="t-usersdn" required value={v("usersDn")} onChange={set("usersDn")} placeholder="ou=people,dc=cliente,dc=com" />
                    </div>
                    <div className="fields-2">
                        <div className="field">
                            <label htmlFor="t-binddn">DN de bind (leitura)</label>
                            <input id="t-binddn" required value={v("bindDn")} onChange={set("bindDn")} />
                        </div>
                        <div className="field">
                            <label htmlFor="t-bindpw">Senha de bind</label>
                            <input id="t-bindpw" type="password" autoComplete="off" value={v("secret")} onChange={set("secret")} />
                        </div>
                    </div>
                    <small className="muted">Somente leitura. Exige rede até o diretório do cliente (VPN/túnel).</small>
                </fieldset>
            ) : null}

            {method === "local" ? (
                <p className="muted" style={{ margin: 0 }}>
                    Os usuários do cliente serão criados no próprio Keycloak. Prefira o IdP do cliente quando ele tiver um.
                </p>
            ) : null}

            {error ? <div className="alert alert--error">{error}</div> : null}
            {done ? <div className="alert alert--ok">{done}</div> : null}

            <div>
                <button className="btn" type="submit" disabled={saving}>
                    {saving ? "Cadastrando…" : "Cadastrar cliente"}
                </button>
            </div>
        </form>
    );
}

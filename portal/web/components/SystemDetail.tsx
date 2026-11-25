"use client";

import { useState } from "react";
import type { PortalApi } from "@/lib/portal-api";
import { href } from "@/lib/router";
import { useLoad } from "./useLoad";
import { AddTenant } from "./AddTenant";

const LOGIN_LABEL = { local: "Usuários locais", oidc: "IdP do cliente (OIDC)", ldap: "LDAP do cliente" } as const;

export function SystemDetail({ api, slug }: { api: PortalApi; slug: string }) {
    const system = useLoad(() => api.getSystem(slug), [api, slug]);
    const env = useLoad(() => api.envFile(slug), [api, slug]);
    const tenants = useLoad(() => api.listTenants(slug), [api, slug]);
    const [copied, setCopied] = useState(false);

    function downloadEnv() {
        if (!env.data) return;
        const url = URL.createObjectURL(new Blob([env.data], { type: "text/plain" }));
        const a = document.createElement("a");
        a.href = url;
        a.download = `${slug}.env`;
        a.click();
        URL.revokeObjectURL(url);
    }

    async function copyEnv() {
        if (!env.data) return;
        await navigator.clipboard.writeText(env.data);
        setCopied(true);
        setTimeout(() => setCopied(false), 1800);
    }

    if (system.error) {
        return <div className="alert alert--error">{system.error}</div>;
    }
    const s = system.data;

    return (
        <>
            <div className="page-head">
                <div>
                    <a href={href({ name: "systems" })} className="muted">
                        ← Sistemas
                    </a>
                    <h1>{s?.displayName ?? slug}</h1>
                    <p>
                        Realm <span className="mono">{slug}</span> · atualizado por {s?.updatedBy ?? "…"}
                        {s ? ` em ${new Date(s.updatedAt).toLocaleString("pt-BR")}` : ""}
                    </p>
                </div>
            </div>

            <div className="split">
                <section className="card">
                    <div className="row" style={{ justifyContent: "space-between" }}>
                        <h2>.env do projeto</h2>
                        <div className="row">
                            <button className="btn btn--secondary btn--compact" onClick={() => void copyEnv()} disabled={!env.data}>
                                {copied ? "Copiado" : "Copiar"}
                            </button>
                            <button className="btn btn--compact" onClick={downloadEnv} disabled={!env.data} id="download-env">
                                Baixar .env
                            </button>
                        </div>
                    </div>
                    <p className="muted" style={{ margin: 0 }}>
                        Cole no projeto e comece pelas telas. Não há segredo aqui: o front usa PKCE e a API valida o token pela chave pública.
                    </p>
                    {env.error ? <div className="alert alert--error">{env.error}</div> : <pre className="env">{env.data ?? "…"}</pre>}
                </section>

                <section className="card">
                    <h2>Configuração</h2>
                    <dl style={{ margin: 0, display: "grid", gridTemplateColumns: "auto 1fr", gap: "8px 16px" }}>
                        <dt className="muted">Front-end</dt>
                        <dd style={{ margin: 0 }}>{s?.webBaseUrl}</dd>
                        <dt className="muted">Client web</dt>
                        <dd style={{ margin: 0 }} className="mono">{s?.webClientId}</dd>
                        <dt className="muted">Audience da API</dt>
                        <dd style={{ margin: 0 }} className="mono">{s?.apiAudience}</dd>
                        <dt className="muted">Tenants</dt>
                        <dd style={{ margin: 0 }}>{s ? (s.multiTenant ? "Vários clientes" : "Cliente único") : ""}</dd>
                    </dl>
                    <span className="label">Roles</span>
                    <div className="chips">
                        {s?.roles.map(r => (
                            <span key={r} className="chip">
                                {r}
                            </span>
                        ))}
                    </div>
                </section>
            </div>

            {s?.multiTenant ? (
                <div className="split">
                    <section className="card">
                        <h2>Clientes</h2>
                        {tenants.error ? <div className="alert alert--error">{tenants.error}</div> : null}
                        {tenants.data && tenants.data.length === 0 ? (
                            <p className="muted">Nenhum cliente ainda. Cadastre o primeiro ao lado.</p>
                        ) : null}
                        {tenants.data && tenants.data.length > 0 ? (
                            <table>
                                <thead>
                                    <tr>
                                        <th>Cliente</th>
                                        <th>Domínios</th>
                                        <th>Login</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {tenants.data.map(t => (
                                        <tr key={t.alias}>
                                            <td>
                                                <strong>{t.name}</strong>
                                                <div className="muted mono">{t.alias}</div>
                                            </td>
                                            <td>{t.domains.join(", ")}</td>
                                            <td>
                                                <span className={t.loginMethod === "local" ? "chip" : "chip chip--info"}>
                                                    {LOGIN_LABEL[t.loginMethod]}
                                                </span>
                                                {t.identityProviderRedirectUri ? (
                                                    <div className="muted" style={{ marginTop: 8, fontSize: 12 }}>
                                                        URL de retorno para o IdP do cliente:
                                                        <div className="mono" style={{ wordBreak: "break-all" }}>
                                                            {t.identityProviderRedirectUri}
                                                        </div>
                                                    </div>
                                                ) : null}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : null}
                    </section>
                    <AddTenant api={api} slug={slug} onAdded={tenants.reload} />
                </div>
            ) : null}
        </>
    );
}

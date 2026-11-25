"use client";

import type { PortalApi } from "@/lib/portal-api";
import { href } from "@/lib/router";
import { useLoad } from "./useLoad";

export function SystemsList({ api }: { api: PortalApi }) {
    const { data, error, loading } = useLoad(() => api.listSystems(), [api]);

    return (
        <>
            <div className="page-head">
                <div>
                    <h1>Sistemas</h1>
                    <p>Cada sistema ganha um realm com o template da plataforma, o login no layout da empresa e um .env pronto.</p>
                </div>
                <a className="btn" href={href({ name: "new-system" })} id="new-system">
                    Novo sistema
                </a>
            </div>

            {error ? <div className="alert alert--error">{error}</div> : null}
            {loading && !data ? <p className="muted">Carregando…</p> : null}
            {data && data.length === 0 ? (
                <div className="card empty">Nenhum sistema ainda. Comece pelo botão “Novo sistema”.</div>
            ) : null}

            <div className="grid">
                {data?.map(s => (
                    <a key={s.slug} className="card" href={href({ name: "system", slug: s.slug })}>
                        <span className="label">{s.slug}</span>
                        <h2>{s.displayName}</h2>
                        <span className="muted">{s.webBaseUrl}</span>
                        <div className="chips">
                            <span className={s.multiTenant ? "chip chip--info" : "chip"}>
                                {s.multiTenant ? "Multi-tenant" : "Tenant único"}
                            </span>
                            <span className="chip">{s.roles.length} roles</span>
                        </div>
                    </a>
                ))}
            </div>
        </>
    );
}

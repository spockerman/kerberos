"use client";

import type { PortalApi } from "@/lib/portal-api";
import { useLoad } from "./useLoad";

const ACTION_LABEL: Record<string, string> = {
    SYSTEM_CREATED: "Sistema criado",
    SYSTEM_UPDATED: "Sistema atualizado",
    TENANT_CREATED: "Cliente criado",
    TENANT_UPDATED: "Cliente atualizado"
};

export function AuditTrail({ api }: { api: PortalApi }) {
    const { data, error, loading } = useLoad(() => api.audit(), [api]);

    return (
        <>
            <div className="page-head">
                <div>
                    <h1>Auditoria</h1>
                    <p>Tudo o que foi feito pelo portal: quem, quando, o quê e o resultado. Segredos nunca aparecem aqui.</p>
                </div>
            </div>
            {error ? <div className="alert alert--error">{error}</div> : null}
            {loading && !data ? <p className="muted">Carregando…</p> : null}
            {data ? (
                <section className="card" style={{ overflowX: "auto" }}>
                    {data.length === 0 ? (
                        <p className="muted">Nada registrado ainda.</p>
                    ) : (
                        <table>
                            <thead>
                                <tr>
                                    <th>Quando</th>
                                    <th>Quem</th>
                                    <th>Ação</th>
                                    <th>Alvo</th>
                                    <th>Resultado</th>
                                </tr>
                            </thead>
                            <tbody>
                                {data.map((e, i) => (
                                    <tr key={i}>
                                        <td className="tabular">{new Date(e.at).toLocaleString("pt-BR")}</td>
                                        <td>{e.actor}</td>
                                        <td>{ACTION_LABEL[e.action] ?? e.action}</td>
                                        <td className="mono">{e.target}</td>
                                        <td>
                                            <span className={e.outcome === "SUCCESS" ? "chip chip--ok" : "chip chip--error"}>
                                                {e.outcome === "SUCCESS" ? "Sucesso" : "Falha"}
                                            </span>
                                            {e.outcome === "FAILURE" && e.detail ? (
                                                <div className="muted" style={{ fontSize: 12, marginTop: 6, maxWidth: 360 }}>
                                                    {e.detail}
                                                </div>
                                            ) : null}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </section>
            ) : null}
        </>
    );
}

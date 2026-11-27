"use client";

import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { ApiError, demoApi, type Me, type TenantDashboard } from "@/lib/api";
import logo from "@/app/_design-system/brand/logo-mark.svg";
import brand from "@/app/_design-system/brand/brand.json";

type TenantResult = { tenant: string; data?: TenantDashboard; error?: string };

export function Home({ apiUrl }: { apiUrl: string }) {
    const auth = useAuth();
    const [me, setMe] = useState<Me>();
    const [meError, setMeError] = useState<string>();
    const [tenantResults, setTenantResults] = useState<TenantResult[]>([]);

    const token = auth.user?.access_token;

    useEffect(() => {
        if (!token) return;
        demoApi
            .me(apiUrl, token)
            .then(setMe)
            .catch((e: Error) => setMeError(e.message));
    }, [apiUrl, token]);

    async function probeTenant(tenant: string) {
        if (!token) return;
        try {
            const data = await demoApi.tenantDashboard(apiUrl, token, tenant);
            setTenantResults(r => [...r.filter(x => x.tenant !== tenant), { tenant, data }]);
        } catch (e) {
            const message = e instanceof ApiError ? `${e.status} — ${e.message}` : String(e);
            setTenantResults(r => [...r.filter(x => x.tenant !== tenant), { tenant, error: message }]);
        }
    }

    const logoSrc = typeof logo === "string" ? logo : (logo as { src: string }).src;

    return (
        <main className="page">
            <header className="topbar">
                <img src={logoSrc} alt={brand.productName} />
                {auth.isAuthenticated ? (
                    <button className="btn btn--secondary" onClick={() => void auth.signoutRedirect()}>
                        Sair
                    </button>
                ) : null}
            </header>

            <section className="hero">
                <h1>Aplicação de exemplo</h1>
                <p>Login pelo Keycloak com o tema da plataforma, tenant vindo da Organization e API validando o token.</p>
            </section>

            {auth.isLoading ? (
                <p className="muted">Carregando…</p>
            ) : auth.error ? (
                <div className="card">
                    <span className="chip chip--error">Erro de autenticação</span>
                    <p>{auth.error.message}</p>
                </div>
            ) : !auth.isAuthenticated ? (
                <div className="card" style={{ alignItems: "flex-start" }}>
                    <span className="label">Não autenticado</span>
                    <p className="muted">Usuários de teste: ana, bruno, carla — senha no README.</p>
                    <button className="btn" id="login" onClick={() => void auth.signinRedirect()}>
                        Entrar
                    </button>
                </div>
            ) : (
                <div className="grid">
                    <div className="card">
                        <span className="label">Usuário (da API /api/me)</span>
                        {meError ? <span className="chip chip--error">{meError}</span> : null}
                        {me ? (
                            <>
                                <span className="value">{me.username}</span>
                                <span className="muted">{me.email}</span>
                                <span className="label">Tenants</span>
                                <div className="chips">
                                    {me.tenants.length === 0 ? <span className="muted">nenhum</span> : null}
                                    {me.tenants.map(t => (
                                        <span key={t} className="chip chip--ok">
                                            {t}
                                        </span>
                                    ))}
                                </div>
                                <span className="label">Roles</span>
                                <div className="chips">
                                    {me.roles
                                        .filter(r => ["admin", "gestor", "usuario"].includes(r))
                                        .map(r => (
                                            <span key={r} className="chip">
                                                {r}
                                            </span>
                                        ))}
                                </div>
                            </>
                        ) : !meError ? (
                            <span className="muted">Consultando…</span>
                        ) : null}
                    </div>

                    <div className="card">
                        <span className="label">Isolamento de tenant</span>
                        <p className="muted">A API só devolve dados dos tenants dos quais você é membro.</p>
                        <div className="chips">
                            {["acme", "globex"].map(t => (
                                <button key={t} className="btn btn--secondary" onClick={() => void probeTenant(t)}>
                                    Acessar {t}
                                </button>
                            ))}
                        </div>
                        {tenantResults.map(r => (
                            <div key={r.tenant}>
                                {r.data ? (
                                    <span className="chip chip--ok">
                                        {r.tenant}: {r.data.message}
                                    </span>
                                ) : (
                                    <span className="chip chip--error">
                                        {r.tenant}: {r.error}
                                    </span>
                                )}
                            </div>
                        ))}
                    </div>

                    <div className="card">
                        <span className="label">Claims do ID token</span>
                        <pre>{JSON.stringify(auth.user?.profile, null, 2)}</pre>
                    </div>
                </div>
            )}
        </main>
    );
}

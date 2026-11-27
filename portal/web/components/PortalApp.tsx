"use client";

import { useEffect, useState } from "react";
import { AuthProvider, useAuth } from "react-oidc-context";
import { WebStorageStateStore } from "oidc-client-ts";
import type { PortalConfig } from "@/lib/config";
import { Shell } from "./Shell";
import logo from "@/app/_design-system/brand/logo.svg";
import brand from "@/app/_design-system/brand/brand.json";

/** Login no realm da plataforma (Authorization Code + PKCE, cliente público). */
export function PortalApp({ config }: { config: PortalConfig }) {
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);
    if (!mounted) return null;

    const origin = window.location.origin;
    return (
        <AuthProvider
            authority={`${config.keycloakUrl}/realms/${config.realm}`}
            client_id={config.clientId}
            redirect_uri={`${origin}/`}
            post_logout_redirect_uri={`${origin}/`}
            scope="openid profile email"
            userStore={new WebStorageStateStore({ store: window.sessionStorage })}
            onSigninCallback={() => window.history.replaceState({}, document.title, window.location.pathname + window.location.hash)}
        >
            <Gate apiUrl={config.apiUrl} />
        </AuthProvider>
    );
}

function Gate({ apiUrl }: { apiUrl: string }) {
    const auth = useAuth();
    const logoSrc = typeof logo === "string" ? logo : (logo as { src: string }).src;

    if (auth.isLoading) {
        return <p className="empty">Carregando…</p>;
    }
    if (auth.isAuthenticated) {
        return <Shell apiUrl={apiUrl} />;
    }
    return (
        <main className="shell">
            <section className="hero" style={{ minHeight: 360 }}>
                <div>
                    <img src={logoSrc} alt={brand.productName} style={{ height: 144 }} />
                    <h1 style={{ marginTop: 32 }}>Portal de Provisionamento</h1>
                    <p>Crie sistemas e cadastre clientes na plataforma de identidade, com o login no layout da empresa já pronto.</p>
                </div>
                <button className="btn" id="login" onClick={() => void auth.signinRedirect()}>
                    Entrar
                </button>
            </section>
            {auth.error ? <div className="alert alert--error">{auth.error.message}</div> : null}
        </main>
    );
}

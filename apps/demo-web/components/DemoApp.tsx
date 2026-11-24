"use client";

import { useEffect, useState } from "react";
import { AuthProvider } from "react-oidc-context";
import { WebStorageStateStore } from "oidc-client-ts";
import type { AppConfig } from "@/lib/config";
import { Home } from "./Home";

/** Login OIDC: Authorization Code + PKCE, cliente público (sem segredo no navegador). */
export function DemoApp({ config }: { config: AppConfig }) {
    // O login usa window/sessionStorage: renderiza só no navegador (evita divergência de hidratação).
    const [mounted, setMounted] = useState(false);
    useEffect(() => setMounted(true), []);
    if (!mounted) {
        return null;
    }

    const origin = window.location.origin;

    return (
        <AuthProvider
            authority={`${config.keycloakUrl}/realms/${config.realm}`}
            client_id={config.clientId}
            redirect_uri={`${origin}/`}
            post_logout_redirect_uri={`${origin}/`}
            scope="openid profile email organization"
            // Tokens na sessionStorage: somem ao fechar a aba
            userStore={new WebStorageStateStore({ store: window.sessionStorage })}
            onSigninCallback={() => window.history.replaceState({}, document.title, window.location.pathname)}
        >
            <Home apiUrl={config.apiUrl} />
        </AuthProvider>
    );
}

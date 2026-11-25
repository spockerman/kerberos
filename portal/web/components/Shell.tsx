"use client";

import { useMemo } from "react";
import { useAuth } from "react-oidc-context";
import { createPortalApi } from "@/lib/portal-api";
import { href, useRoute } from "@/lib/router";
import { SystemsList } from "./SystemsList";
import { NewSystem } from "./NewSystem";
import { SystemDetail } from "./SystemDetail";
import { AuditTrail } from "./AuditTrail";
import logoMark from "@/app/_design-system/brand/logo-mark.svg";

export function Shell({ apiUrl }: { apiUrl: string }) {
    const auth = useAuth();
    const route = useRoute();
    const api = useMemo(() => createPortalApi(apiUrl, () => auth.user?.access_token), [apiUrl, auth.user]);
    const logoSrc = typeof logoMark === "string" ? logoMark : (logoMark as { src: string }).src;
    const username = (auth.user?.profile.preferred_username as string | undefined) ?? "";

    const section = route.name === "audit" ? "audit" : "systems";

    return (
        <div className="shell">
            <header className="topbar">
                <a className="topbar__brand" href={href({ name: "systems" })}>
                    <img src={logoSrc} alt="Central 156" />
                    <span>Portal de Provisionamento</span>
                </a>
                <nav className="nav" aria-label="Seções">
                    <a href={href({ name: "systems" })} aria-current={section === "systems" ? "page" : undefined}>
                        Sistemas
                    </a>
                    <a href={href({ name: "audit" })} aria-current={section === "audit" ? "page" : undefined}>
                        Auditoria
                    </a>
                </nav>
                <div className="user">
                    <span>{username}</span>
                    <button className="btn btn--secondary btn--compact" onClick={() => void auth.signoutRedirect()}>
                        Sair
                    </button>
                </div>
            </header>

            {route.name === "systems" && <SystemsList api={api} />}
            {route.name === "new-system" && <NewSystem api={api} />}
            {route.name === "system" && <SystemDetail api={api} slug={route.slug} />}
            {route.name === "audit" && <AuditTrail api={api} />}
        </div>
    );
}

"use client";

import { useEffect, useState } from "react";

/** Rotas por hash (#/sistemas/financeiro): uma página só, um login só. */
export type Route =
    | { name: "systems" }
    | { name: "new-system" }
    | { name: "system"; slug: string }
    | { name: "audit" };

export function parseRoute(hash: string): Route {
    const path = hash.replace(/^#/, "") || "/";
    if (path === "/novo") return { name: "new-system" };
    if (path === "/auditoria") return { name: "audit" };
    const match = path.match(/^\/sistemas\/([a-z0-9-]+)$/);
    if (match) return { name: "system", slug: match[1] };
    return { name: "systems" };
}

export function href(route: Route): string {
    switch (route.name) {
        case "new-system":
            return "#/novo";
        case "audit":
            return "#/auditoria";
        case "system":
            return `#/sistemas/${route.slug}`;
        default:
            return "#/";
    }
}

export function useRoute(): Route {
    const [route, setRoute] = useState<Route>(() => parseRoute(window.location.hash));
    useEffect(() => {
        const onChange = () => setRoute(parseRoute(window.location.hash));
        window.addEventListener("hashchange", onChange);
        return () => window.removeEventListener("hashchange", onChange);
    }, []);
    return route;
}

export function navigate(route: Route) {
    window.location.hash = href(route);
}

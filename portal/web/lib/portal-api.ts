/**
 * Adapter de saída do front: única camada que conhece as rotas e o formato da portal-api.
 * Os componentes chamam estas funções, nunca fetch direto.
 */
export type SystemView = {
    slug: string;
    displayName: string;
    webBaseUrl: string;
    webClientId: string;
    apiAudience: string;
    roles: string[];
    multiTenant: boolean;
    updatedAt: string;
    updatedBy: string;
};

export type SystemRequest = {
    slug: string;
    displayName: string;
    webBaseUrl: string;
    roles: string[];
    multiTenant: boolean;
};

export type TenantView = {
    alias: string;
    name: string;
    domains: string[];
    loginMethod: "local" | "oidc" | "ldap";
    identityProviderRedirectUri: string | null;
    updatedAt: string;
};

export type TenantRequest = {
    alias: string;
    name: string;
    domains: string[];
    loginMethod: "local" | "oidc" | "ldap";
    oidc?: {
        issuer: string;
        authorizationUrl: string;
        tokenUrl: string;
        jwksUrl: string;
        userInfoUrl?: string;
        logoutUrl?: string;
        clientId: string;
    };
    ldap?: { connectionUrl: string; usersDn: string; bindDn: string; vendor: "openldap" | "ad" };
    secret?: string;
};

export type AuditEntry = {
    at: string;
    actor: string;
    action: string;
    target: string;
    outcome: "SUCCESS" | "FAILURE";
    detail: string | null;
};

export class PortalApiError extends Error {
    constructor(
        readonly status: number,
        message: string
    ) {
        super(message);
    }
}

export function createPortalApi(baseUrl: string, getToken: () => string | undefined) {
    async function call<T>(path: string, init: RequestInit = {}, as: "json" | "text" = "json"): Promise<T> {
        const response = await fetch(`${baseUrl}${path}`, {
            ...init,
            headers: {
                Authorization: `Bearer ${getToken() ?? ""}`,
                ...(init.body ? { "Content-Type": "application/json" } : {}),
                ...init.headers
            }
        });
        if (!response.ok) {
            const problem = (await response.json().catch(() => null)) as { detail?: string } | null;
            const fallback =
                response.status === 403
                    ? "Você não tem permissão de provisionamento."
                    : response.status === 401
                      ? "Sessão expirada. Entre de novo."
                      : response.statusText;
            throw new PortalApiError(response.status, problem?.detail ?? fallback);
        }
        return (as === "text" ? await response.text() : await response.json()) as T;
    }

    return {
        listSystems: () => call<SystemView[]>("/api/systems"),
        getSystem: (slug: string) => call<SystemView>(`/api/systems/${encodeURIComponent(slug)}`),
        registerSystem: (request: SystemRequest) =>
            call<SystemView>("/api/systems", { method: "POST", body: JSON.stringify(request) }),
        envFile: (slug: string) => call<string>(`/api/systems/${encodeURIComponent(slug)}/env`, {}, "text"),
        listTenants: (slug: string) => call<TenantView[]>(`/api/systems/${encodeURIComponent(slug)}/tenants`),
        addTenant: (slug: string, request: TenantRequest) =>
            call<TenantView>(`/api/systems/${encodeURIComponent(slug)}/tenants`, {
                method: "POST",
                body: JSON.stringify(request)
            }),
        audit: () => call<AuditEntry[]>("/api/audit?limit=200")
    };
}

export type PortalApi = ReturnType<typeof createPortalApi>;

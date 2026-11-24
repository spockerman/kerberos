/**
 * Adapter de saída do front-end: única camada que conhece as URLs e o formato da demo-api.
 * Os componentes chamam estas funções, nunca fetch direto.
 */
export type Me = {
    userId: string;
    username: string;
    email: string;
    tenants: string[];
    roles: string[];
};

export type TenantDashboard = {
    tenant: string;
    message: string;
    canManage: boolean;
};

export class ApiError extends Error {
    constructor(
        readonly status: number,
        message: string
    ) {
        super(message);
    }
}

async function get<T>(apiUrl: string, path: string, accessToken: string): Promise<T> {
    const response = await fetch(`${apiUrl}${path}`, {
        headers: { Authorization: `Bearer ${accessToken}` }
    });
    if (!response.ok) {
        const body = (await response.json().catch(() => null)) as { detail?: string } | null;
        throw new ApiError(response.status, body?.detail ?? response.statusText);
    }
    return (await response.json()) as T;
}

export const demoApi = {
    me: (apiUrl: string, token: string) => get<Me>(apiUrl, "/api/me", token),
    tenantDashboard: (apiUrl: string, token: string, tenant: string) =>
        get<TenantDashboard>(apiUrl, `/api/tenants/${encodeURIComponent(tenant)}/dashboard`, token)
};

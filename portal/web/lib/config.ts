/** Configuração lida do ambiente em tempo de execução (a mesma imagem serve a qualquer ambiente). */
export type PortalConfig = {
    keycloakUrl: string;
    realm: string;
    clientId: string;
    apiUrl: string;
};

export function readConfig(): PortalConfig {
    return {
        keycloakUrl: process.env.KEYCLOAK_URL ?? "http://localhost:8080",
        realm: process.env.KEYCLOAK_REALM ?? "plataforma",
        clientId: process.env.KEYCLOAK_CLIENT_ID ?? "portal-web",
        apiUrl: process.env.PORTAL_API_URL ?? "http://localhost:8082"
    };
}

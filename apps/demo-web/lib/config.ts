/** Configuração do front-end, lida do ambiente em tempo de execução (a mesma imagem serve a qualquer ambiente). */
export type AppConfig = {
    keycloakUrl: string;
    realm: string;
    clientId: string;
    apiUrl: string;
};

export function readConfig(): AppConfig {
    return {
        keycloakUrl: process.env.KEYCLOAK_URL ?? "http://localhost:8080",
        realm: process.env.KEYCLOAK_REALM ?? "demo",
        clientId: process.env.KEYCLOAK_CLIENT_ID ?? "demo-web",
        apiUrl: process.env.API_URL ?? "http://localhost:8081"
    };
}

import { createRoot } from "react-dom/client";
import { StrictMode } from "react";
import { KcPage } from "./kc.gen";

async function bootstrap() {
    // Modo de pré-visualização (npm run dev): abre a página de login com dados simulados.
    // Troque o pageId para ver outras telas. Em produção o Keycloak injeta window.kcContext.
    if (import.meta.env.DEV && !window.kcContext) {
        const { getKcContextMock } = await import("./login/mock");
        const pageId = (new URLSearchParams(location.search).get("page") ?? "login.ftl") as "login.ftl";
        window.kcContext = getKcContextMock({ pageId });
    }

    createRoot(document.getElementById("root")!).render(
        <StrictMode>
            {!window.kcContext ? <h1>Sem contexto do Keycloak</h1> : <KcPage kcContext={window.kcContext} />}
        </StrictMode>
    );
}

void bootstrap();

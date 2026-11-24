import { createGetKcContextMock } from "keycloakify/login/KcContext";
import type { KcContextExtension, KcContextExtensionPerPage } from "./KcContext";
import { themeNames, kcEnvDefaults } from "../kc.gen";

/** Contexto simulado para pré-visualizar o tema sem um Keycloak rodando. */
export const { getKcContextMock } = createGetKcContextMock({
    kcContextExtension: {
        themeName: themeNames[0],
        properties: { ...kcEnvDefaults }
    } satisfies KcContextExtension,
    kcContextExtensionPerPage: {} satisfies KcContextExtensionPerPage,
    overrides: {
        realm: { displayName: "Demo", name: "demo" },
        locale: { currentLanguageTag: "pt-BR" }
    },
    overridesPerPage: {}
});

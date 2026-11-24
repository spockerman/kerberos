import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { keycloakify } from "keycloakify/vite-plugin";

export default defineConfig({
    plugins: [
        react(),
        keycloakify({
            // Nome técnico do tema no Keycloak. A identidade visível vem de design-system/brand.
            themeName: "kerberos",
            accountThemeImplementation: "none",
            keycloakVersionTargets: {
                // Um único JAR para o Keycloak 26.x
                "22-to-25": false,
                "all-other-versions": "kerberos-theme.jar"
            }
        })
    ],
    server: {
        // Permite importar design-system/ (fora da pasta theme/) no modo dev
        fs: { allow: [".."] }
    }
});

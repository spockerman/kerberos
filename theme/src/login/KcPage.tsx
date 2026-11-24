import { Suspense, lazy } from "react";
import type { ClassKey } from "keycloakify/login";
import DefaultPage from "keycloakify/login/DefaultPage";
import type { KcContext } from "./KcContext";
import { useI18n } from "./i18n";
import Template from "./Template";
import "./styles.css";

const UserProfileFormFields = lazy(() => import("keycloakify/login/UserProfileFormFields"));

/**
 * Todas as páginas usam os componentes padrão do Keycloakify com o nosso Template e o nosso CSS.
 * doUseDefaultCss=false: o CSS do PatternFly não é carregado; o visual vem só de styles.css (tokens).
 * Para personalizar uma página específica: `npx keycloakify eject-page` e adicione um case aqui.
 */
export default function KcPage(props: { kcContext: KcContext }) {
    const { kcContext } = props;
    const { i18n } = useI18n({ kcContext });

    return (
        <Suspense>
            <DefaultPage
                kcContext={kcContext}
                i18n={i18n}
                classes={classes}
                Template={Template}
                doUseDefaultCss={false}
                UserProfileFormFields={UserProfileFormFields}
                doMakeUserConfirmPassword={true}
            />
        </Suspense>
    );
}

/** Classes extras por chave do Keycloak. A própria chave (ex.: .kcInputClass) também vira classe CSS. */
const classes = {
    kcButtonPrimaryClass: "ds-btn ds-btn--primary",
    kcButtonDefaultClass: "ds-btn ds-btn--secondary",
    kcInputClass: "ds-input",
    kcLabelClass: "ds-label",
    kcInputErrorMessageClass: "ds-error-text",
    kcCheckboxInputClass: "ds-checkbox"
} satisfies { [key in ClassKey]?: string };

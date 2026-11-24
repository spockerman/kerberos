import { useEffect } from "react";
import { clsx } from "keycloakify/tools/clsx";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { useSetClassName } from "keycloakify/tools/useSetClassName";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import type { I18n } from "./i18n";
import type { KcContext } from "./KcContext";
import { brand } from "../brand";

/**
 * Layout de todas as telas de autenticação.
 * Esquerda: faixa com o gradiente assinatura (um por tela) e a identidade do sistema.
 * Direita: card com o formulário da página atual.
 * Mantém os mesmos comportamentos do Template padrão do Keycloakify (idioma, mensagens,
 * "tentar outro método", provedores de identidade, área de informação).
 */
export default function Template(props: TemplateProps<KcContext, I18n>) {
    const {
        displayInfo = false,
        displayMessage = true,
        displayRequiredFields = false,
        headerNode,
        socialProvidersNode = null,
        infoNode = null,
        documentTitle,
        bodyClassName,
        kcContext,
        i18n,
        doUseDefaultCss,
        classes,
        children
    } = props;

    const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });
    const { msg, msgStr, currentLanguage, enabledLanguages } = i18n;
    const { realm, auth, url, message, isAppInitiatedAction } = kcContext;

    // Nome exibido: o do sistema (brand.json); enquanto for placeholder, o nome do realm.
    const productName = brand.isPlaceholder ? realm.displayName || realm.name : brand.productName;

    useEffect(() => {
        document.title = documentTitle ?? msgStr("loginTitle", productName);
    }, []);

    useSetClassName({ qualifiedName: "html", className: kcClsx("kcHtmlClass") });
    useSetClassName({ qualifiedName: "body", className: bodyClassName ?? kcClsx("kcBodyClass") });

    const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss });

    if (!isReadyToRender) {
        return null;
    }

    const showAttemptedUsername = auth !== undefined && auth.showUsername && !auth.showResetCredentials;

    return (
        <div className={clsx("kc-shell", kcClsx("kcLoginClass"))}>
            <aside className="kc-hero" aria-hidden="true">
                <img className="kc-hero__logo" src={brand.logoUrl} alt="" />
                <p className="kc-hero__name">{productName}</p>
            </aside>

            <main className="kc-main">
                <div className={clsx("kc-card", kcClsx("kcFormCardClass"))}>
                    <header className={clsx("kc-card__header", kcClsx("kcFormHeaderClass"))}>
                        <div className="kc-card__topbar">
                            <img className="kc-card__logo" src={brand.logoMarkUrl} alt={productName} />
                            {enabledLanguages.length > 1 && (
                                <details className={clsx("kc-locale", kcClsx("kcLocaleMainClass"))} id="kc-locale">
                                    <summary aria-label={msgStr("languages")}>{currentLanguage.label}</summary>
                                    <ul role="menu" className={kcClsx("kcLocaleListClass")}>
                                        {enabledLanguages.map(({ languageTag, label, href }) => (
                                            <li key={languageTag} role="none">
                                                <a role="menuitem" href={href} lang={languageTag}>
                                                    {label}
                                                </a>
                                            </li>
                                        ))}
                                    </ul>
                                </details>
                            )}
                        </div>

                        {showAttemptedUsername ? (
                            <div id="kc-username" className="kc-attempted-user">
                                <span id="kc-attempted-username">{auth.attemptedUsername}</span>
                                <a id="reset-login" href={url.loginRestartFlowUrl} aria-label={msgStr("restartLoginTooltip")}>
                                    {msg("restartLoginTooltip")}
                                </a>
                            </div>
                        ) : (
                            <h1 id="kc-page-title" className="kc-title">
                                {headerNode}
                            </h1>
                        )}

                        {displayRequiredFields && (
                            <p className="kc-required-hint">
                                <span className="required">*</span> {msg("requiredFields")}
                            </p>
                        )}
                    </header>

                    <div id="kc-content">
                        {displayMessage && message !== undefined && (message.type !== "warning" || !isAppInitiatedAction) && (
                            <div
                                role={message.type === "error" ? "alert" : "status"}
                                className={clsx("kc-alert", `kc-alert--${message.type}`, kcClsx("kcAlertClass"))}
                            >
                                <span
                                    className={kcClsx("kcAlertTitleClass")}
                                    dangerouslySetInnerHTML={{ __html: kcSanitize(message.summary) }}
                                />
                            </div>
                        )}

                        {children}

                        {auth !== undefined && auth.showTryAnotherWayLink && (
                            <form id="kc-select-try-another-way-form" action={url.loginAction} method="post">
                                <input type="hidden" name="tryAnotherWay" value="on" />
                                <button type="submit" className="kc-link-button" id="try-another-way">
                                    {msg("doTryAnotherWay")}
                                </button>
                            </form>
                        )}

                        {socialProvidersNode}

                        {displayInfo && (
                            <div id="kc-info" className={clsx("kc-info", kcClsx("kcSignUpClass"))}>
                                {infoNode}
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}

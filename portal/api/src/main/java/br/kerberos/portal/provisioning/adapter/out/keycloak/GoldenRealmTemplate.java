package br.kerberos.portal.provisioning.adapter.out.keycloak;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cópia em Java do módulo Terraform infra/terraform/modules/realm-golden (ver ADR 0004).
 * QUALQUER mudança lá precisa ser repetida aqui — o teste ponta a ponta compara os dois e falha se divergirem.
 */
public final class GoldenRealmTemplate {

    static final String PASSWORD_POLICY = "length(12) and notUsername and notEmail and passwordHistory(3)";

    private GoldenRealmTemplate() {}

    /** Configurações do realm (sem roles), usadas na criação e para consertar um realm existente. */
    public static Map<String, Object> settings(String realm, String displayName, String loginTheme, Map<String, String> smtp) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("realm", realm);
        r.put("enabled", true);
        r.put("displayName", displayName);
        r.put("organizationsEnabled", true);
        r.put("loginTheme", loginTheme);
        r.put("sslRequired", "external");

        r.put("registrationAllowed", false);
        r.put("resetPasswordAllowed", true);
        r.put("rememberMe", true);
        r.put("loginWithEmailAllowed", true);
        r.put("duplicateEmailsAllowed", false);
        r.put("verifyEmail", false);
        r.put("passwordPolicy", PASSWORD_POLICY);

        r.put("accessTokenLifespan", 300);
        r.put("ssoSessionIdleTimeout", 1800);
        r.put("ssoSessionMaxLifespan", 36000);
        r.put("revokeRefreshToken", true);
        r.put("refreshTokenMaxReuse", 0);

        r.put("internationalizationEnabled", true);
        r.put("supportedLocales", List.of("pt-BR", "en"));
        r.put("defaultLocale", "pt-BR");

        r.put("bruteForceProtected", true);
        r.put("permanentLockout", false);
        r.put("failureFactor", 10);
        r.put("waitIncrementSeconds", 60);
        r.put("quickLoginCheckMilliSeconds", 1000);
        r.put("minimumQuickLoginWaitSeconds", 60);
        r.put("maxFailureWaitSeconds", 900);
        r.put("maxDeltaTimeSeconds", 43200);

        r.put("browserSecurityHeaders", Map.of(
                "xFrameOptions", "SAMEORIGIN",
                "contentSecurityPolicy", "frame-src 'self'; frame-ancestors 'self'; object-src 'none';",
                "contentSecurityPolicyReportOnly", "",
                "xContentTypeOptions", "nosniff",
                "xRobotsTag", "none",
                "xXSSProtection", "1; mode=block",
                "strictTransportSecurity", "max-age=31536000; includeSubDomains"));

        r.put("eventsEnabled", true);
        r.put("eventsExpiration", 2592000);
        r.put("eventsListeners", List.of("jboss-logging"));
        r.put("adminEventsEnabled", true);
        r.put("adminEventsDetailsEnabled", true);

        if (smtp != null && !smtp.isEmpty()) {
            r.put("smtpServer", smtp);
        }
        return r;
    }

    /** Role atribuída automaticamente a todo usuário novo. */
    public static final String DEFAULT_ROLE = "usuario";

    /** Composição de default-roles-<realm> (módulo Terraform: keycloak_default_roles). */
    public static final List<String> DEFAULT_ROLES = List.of("offline_access", "uma_authorization", DEFAULT_ROLE);

    /** Escopos sempre presentes no token de um client de SPA (igual ao módulo spa-app). */
    public static final List<String> SPA_DEFAULT_SCOPES = List.of("basic", "profile", "email", "roles", "web-origins", "acr", "organization");

    public static final List<String> SPA_OPTIONAL_SCOPES = List.of("offline_access");
}

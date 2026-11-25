package br.kerberos.portal.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kerberos")
public record PortalProperties(Security security, Keycloak keycloak) {

    public record Security(String issuer, String jwkSetUri, String audience, String requiredRole, List<String> allowedOrigins) {}

    public record Keycloak(
            String adminUrl, String publicUrl, String clientId, String clientSecret, String loginTheme,
            String smtpHost, Integer smtpPort, String smtpFrom) {}
}

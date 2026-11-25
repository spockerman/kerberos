package br.kerberos.portal.config;

import br.kerberos.portal.provisioning.adapter.out.keycloak.KeycloakAdminClient;
import br.kerberos.portal.provisioning.adapter.out.keycloak.KeycloakIdentityProvisioningAdapter;
import br.kerberos.portal.provisioning.adapter.out.persistence.JdbcProvisioningAudit;
import br.kerberos.portal.provisioning.adapter.out.persistence.JdbcSystemCatalog;
import br.kerberos.portal.provisioning.adapter.out.security.JwtCurrentOperatorAdapter;
import br.kerberos.portal.provisioning.application.port.out.CurrentOperatorPort;
import br.kerberos.portal.provisioning.application.port.out.IdentityProvisioningPort;
import br.kerberos.portal.provisioning.application.port.out.ProvisioningAuditPort;
import br.kerberos.portal.provisioning.application.port.out.SystemCatalogPort;
import br.kerberos.portal.provisioning.application.usecase.AuditTrailService;
import br.kerberos.portal.provisioning.application.usecase.SystemProvisioningService;
import br.kerberos.portal.provisioning.application.usecase.TenantProvisioningService;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

/** Composition root: liga adapters concretos às portas dos casos de uso. */
@Configuration
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdentityProvisioningPort identityProvisioning(PortalProperties props) {
        PortalProperties.Keycloak kc = props.keycloak();
        if (kc.clientSecret() == null || kc.clientSecret().isBlank()) {
            throw new IllegalStateException("KEYCLOAK_CLIENT_SECRET não definido (segredo da conta de serviço portal-provisioner)");
        }
        Map<String, String> smtp = new LinkedHashMap<>();
        if (kc.smtpHost() != null && !kc.smtpHost().isBlank()) {
            smtp.put("host", kc.smtpHost());
            smtp.put("port", String.valueOf(kc.smtpPort() == null ? 25 : kc.smtpPort()));
            smtp.put("from", kc.smtpFrom());
        }
        KeycloakAdminClient client = new KeycloakAdminClient(RestClient.builder(), kc.adminUrl(), kc.clientId(), kc.clientSecret());
        return new KeycloakIdentityProvisioningAdapter(client, kc.loginTheme(), smtp);
    }

    @Bean
    SystemCatalogPort systemCatalog(JdbcClient jdbc) {
        return new JdbcSystemCatalog(jdbc);
    }

    @Bean
    ProvisioningAuditPort provisioningAudit(JdbcClient jdbc) {
        return new JdbcProvisioningAudit(jdbc);
    }

    @Bean
    CurrentOperatorPort currentOperator(PortalProperties props) {
        return new JwtCurrentOperatorAdapter(props.security().requiredRole());
    }

    @Bean
    SystemProvisioningService systemProvisioning(
            IdentityProvisioningPort identity, SystemCatalogPort catalog, ProvisioningAuditPort audit,
            CurrentOperatorPort operators, Clock clock, PortalProperties props) {
        return new SystemProvisioningService(identity, catalog, audit, operators, clock, props.keycloak().publicUrl());
    }

    @Bean
    TenantProvisioningService tenantProvisioning(
            IdentityProvisioningPort identity, SystemCatalogPort catalog, ProvisioningAuditPort audit,
            CurrentOperatorPort operators, Clock clock, PortalProperties props) {
        return new TenantProvisioningService(identity, catalog, audit, operators, clock, props.keycloak().publicUrl());
    }

    @Bean
    AuditTrailService auditTrail(ProvisioningAuditPort audit, CurrentOperatorPort operators) {
        return new AuditTrailService(audit, operators);
    }
}

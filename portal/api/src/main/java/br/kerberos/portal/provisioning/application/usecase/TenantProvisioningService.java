package br.kerberos.portal.provisioning.application.usecase;

import br.kerberos.portal.provisioning.application.SystemNotFoundException;
import br.kerberos.portal.provisioning.application.port.in.AddTenant;
import br.kerberos.portal.provisioning.application.port.in.QueryTenants;
import br.kerberos.portal.provisioning.application.port.in.TenantView;
import br.kerberos.portal.provisioning.application.port.out.CurrentOperatorPort;
import br.kerberos.portal.provisioning.application.port.out.IdentityProvisioningPort;
import br.kerberos.portal.provisioning.application.port.out.ProvisioningAuditPort;
import br.kerberos.portal.provisioning.application.port.out.SystemCatalogPort;
import br.kerberos.portal.provisioning.application.port.out.SystemCatalogPort.Stamped;
import br.kerberos.portal.provisioning.domain.InvalidSpecException;
import br.kerberos.portal.provisioning.domain.LoginMethod;
import br.kerberos.portal.provisioning.domain.Operator;
import br.kerberos.portal.provisioning.domain.SystemSlug;
import br.kerberos.portal.provisioning.domain.SystemSpec;
import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

public class TenantProvisioningService implements AddTenant, QueryTenants {

    private final IdentityProvisioningPort identity;
    private final SystemCatalogPort catalog;
    private final CurrentOperatorPort operators;
    private final AuditedExecution audited;
    private final Clock clock;
    private final String keycloakPublicUrl;

    public TenantProvisioningService(
            IdentityProvisioningPort identity,
            SystemCatalogPort catalog,
            ProvisioningAuditPort audit,
            CurrentOperatorPort operators,
            Clock clock,
            String keycloakPublicUrl) {
        this.identity = identity;
        this.catalog = catalog;
        this.operators = operators;
        this.clock = clock;
        this.keycloakPublicUrl = keycloakPublicUrl;
        this.audited = new AuditedExecution(audit, clock);
    }

    @Override
    public TenantView execute(String systemSlug, TenantSpec tenant, Optional<String> secret) {
        Operator operator = operators.current();
        operator.assertCanProvision();

        SystemSpec system = loadSystem(systemSlug);
        TenantSpec.assertAllowedIn(system);
        boolean needsSecret = !(tenant.loginMethod() instanceof LoginMethod.LocalUsers);
        boolean existed = catalog.listTenants(system.slug()).stream().anyMatch(t -> t.value().alias().equals(tenant.alias()));
        if (needsSecret && !existed && secret.filter(s -> !s.isBlank()).isEmpty()) {
            throw new InvalidSpecException(tenant.loginMethod() instanceof LoginMethod.Oidc
                    ? "Informe o client secret do IdP do cliente"
                    : "Informe a senha do usuário de bind do LDAP");
        }

        String target = system.slug() + "/" + tenant.alias();
        String action = existed ? "TENANT_UPDATED" : "TENANT_CREATED";
        String detail = "login=" + tenant.loginMethod().code() + " domains=" + tenant.domains();

        return audited.run(operator.username(), action, target, detail, () -> {
            identity.ensureTenant(system, tenant, secret.filter(s -> !s.isBlank()));
            catalog.saveTenant(system.slug(), tenant, clock.instant(), operator.username());
            return list(systemSlug).stream().filter(t -> t.alias().equals(tenant.alias().value())).findFirst().orElseThrow();
        });
    }

    @Override
    public List<TenantView> list(String systemSlug) {
        SystemSpec system = loadSystem(systemSlug);
        return catalog.listTenants(system.slug()).stream().map(t -> view(system, t)).toList();
    }

    private SystemSpec loadSystem(String slug) {
        return catalog.findSystem(new SystemSlug(slug)).map(Stamped::value).orElseThrow(() -> new SystemNotFoundException(slug));
    }

    private TenantView view(SystemSpec system, Stamped<TenantSpec> stamped) {
        TenantSpec t = stamped.value();
        String redirect = t.loginMethod() instanceof LoginMethod.Oidc
                ? keycloakPublicUrl + "/realms/" + system.realmName() + "/broker/" + t.alias().identityProviderAlias() + "/endpoint"
                : null;
        return new TenantView(t.alias().value(), t.name(), t.domains(), t.loginMethod().code(), redirect, stamped.updatedAt());
    }
}

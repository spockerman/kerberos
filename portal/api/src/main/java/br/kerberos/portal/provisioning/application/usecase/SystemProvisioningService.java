package br.kerberos.portal.provisioning.application.usecase;

import br.kerberos.portal.provisioning.application.SystemNotFoundException;
import br.kerberos.portal.provisioning.application.port.in.QuerySystems;
import br.kerberos.portal.provisioning.application.port.in.RegisterSystem;
import br.kerberos.portal.provisioning.application.port.in.SystemView;
import br.kerberos.portal.provisioning.application.port.out.CurrentOperatorPort;
import br.kerberos.portal.provisioning.application.port.out.IdentityProvisioningPort;
import br.kerberos.portal.provisioning.application.port.out.ProvisioningAuditPort;
import br.kerberos.portal.provisioning.application.port.out.SystemCatalogPort;
import br.kerberos.portal.provisioning.application.port.out.SystemCatalogPort.Stamped;
import br.kerberos.portal.provisioning.domain.EnvFile;
import br.kerberos.portal.provisioning.domain.Operator;
import br.kerberos.portal.provisioning.domain.SystemSlug;
import br.kerberos.portal.provisioning.domain.SystemSpec;
import java.time.Clock;
import java.util.List;

public class SystemProvisioningService implements RegisterSystem, QuerySystems {

    private final IdentityProvisioningPort identity;
    private final SystemCatalogPort catalog;
    private final CurrentOperatorPort operators;
    private final AuditedExecution audited;
    private final Clock clock;
    private final String keycloakPublicUrl;

    public SystemProvisioningService(
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
    public SystemView execute(SystemSpec spec) {
        Operator operator = operators.current();
        operator.assertCanProvision();

        boolean existed = catalog.findSystem(spec.slug()).isPresent();
        String action = existed ? "SYSTEM_UPDATED" : "SYSTEM_CREATED";
        String detail = "web=" + spec.webBaseUrl() + " multiTenant=" + spec.multiTenant() + " roles=" + spec.allRoles();

        return audited.run(operator.username(), action, spec.slug().value(), detail, () -> {
            identity.ensureSystem(spec);
            catalog.saveSystem(spec, clock.instant(), operator.username());
            return view(catalog.findSystem(spec.slug()).orElseThrow());
        });
    }

    @Override
    public List<SystemView> list() {
        return catalog.listSystems().stream().map(SystemProvisioningService::view).toList();
    }

    @Override
    public SystemView get(String slug) {
        return view(load(slug));
    }

    @Override
    public String envFile(String slug) {
        SystemSpec system = load(slug).value();
        return EnvFile.forSystem(system, keycloakPublicUrl)
                .render("Central 156 · Plataforma de Identidade — configuração do sistema " + system.displayName());
    }

    private Stamped<SystemSpec> load(String slug) {
        SystemSlug id = new SystemSlug(slug);
        return catalog.findSystem(id).orElseThrow(() -> new SystemNotFoundException(slug));
    }

    static SystemView view(Stamped<SystemSpec> stamped) {
        SystemSpec s = stamped.value();
        return new SystemView(
                s.slug().value(),
                s.displayName(),
                s.webBaseUrl().toString(),
                s.webClientId(),
                s.apiAudience(),
                s.allRoles(),
                s.multiTenant(),
                stamped.updatedAt(),
                stamped.updatedBy());
    }
}

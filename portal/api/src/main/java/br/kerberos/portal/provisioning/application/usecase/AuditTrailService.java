package br.kerberos.portal.provisioning.application.usecase;

import br.kerberos.portal.provisioning.application.port.in.QueryAuditTrail;
import br.kerberos.portal.provisioning.application.port.out.CurrentOperatorPort;
import br.kerberos.portal.provisioning.application.port.out.ProvisioningAuditPort;
import br.kerberos.portal.provisioning.domain.AuditEntry;
import java.util.List;

public class AuditTrailService implements QueryAuditTrail {

    private final ProvisioningAuditPort audit;
    private final CurrentOperatorPort operators;

    public AuditTrailService(ProvisioningAuditPort audit, CurrentOperatorPort operators) {
        this.audit = audit;
        this.operators = operators;
    }

    @Override
    public List<AuditEntry> latest(int limit) {
        operators.current().assertCanProvision();
        return audit.latest(Math.max(1, Math.min(limit, 500)));
    }
}

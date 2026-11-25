package br.kerberos.portal.provisioning.application.port.out;

import br.kerberos.portal.provisioning.domain.AuditEntry;
import java.util.List;

public interface ProvisioningAuditPort {

    void record(AuditEntry entry);

    List<AuditEntry> latest(int limit);
}

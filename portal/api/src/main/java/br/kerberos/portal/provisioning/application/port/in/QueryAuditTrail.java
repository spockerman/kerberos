package br.kerberos.portal.provisioning.application.port.in;

import br.kerberos.portal.provisioning.domain.AuditEntry;
import java.util.List;

public interface QueryAuditTrail {

    List<AuditEntry> latest(int limit);
}

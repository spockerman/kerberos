package br.kerberos.portal.provisioning.application.usecase;

import br.kerberos.portal.provisioning.application.ProvisioningFailedException;
import br.kerberos.portal.provisioning.application.port.out.ProvisioningAuditPort;
import br.kerberos.portal.provisioning.domain.AuditEntry;
import java.time.Clock;
import java.util.function.Supplier;

/** Executa uma ação de provisionamento e grava o resultado na auditoria, com sucesso ou falha. */
final class AuditedExecution {

    private final ProvisioningAuditPort audit;
    private final Clock clock;

    AuditedExecution(ProvisioningAuditPort audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    <T> T run(String actor, String action, String target, String detail, Supplier<T> work) {
        try {
            T result = work.get();
            audit.record(AuditEntry.success(clock.instant(), actor, action, target, detail));
            return result;
        } catch (ProvisioningFailedException e) {
            audit.record(AuditEntry.failure(clock.instant(), actor, action, target, e.getMessage()));
            throw e;
        } catch (RuntimeException e) {
            audit.record(AuditEntry.failure(clock.instant(), actor, action, target, e.getClass().getSimpleName() + ": " + e.getMessage()));
            throw e;
        }
    }
}

package br.kerberos.portal.provisioning.application.port.out;

import br.kerberos.portal.provisioning.domain.SystemSpec;
import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.util.Optional;

/**
 * Aplica o desejado no provedor de identidade. Toda operação é idempotente: cria o que falta e
 * corrige o que divergir, sem duplicar.
 */
public interface IdentityProvisioningPort {

    void ensureSystem(SystemSpec system);

    void ensureTenant(SystemSpec system, TenantSpec tenant, Optional<String> secret);
}

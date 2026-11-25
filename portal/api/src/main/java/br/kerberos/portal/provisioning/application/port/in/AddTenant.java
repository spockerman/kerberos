package br.kerberos.portal.provisioning.application.port.in;

import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.util.Optional;

/**
 * Cadastra (ou conserta) um cliente num sistema. O segredo do cliente (client secret do IdP ou senha de
 * bind do LDAP) vai direto ao Keycloak e não é guardado pelo portal.
 */
public interface AddTenant {

    TenantView execute(String systemSlug, TenantSpec tenant, Optional<String> secret);
}

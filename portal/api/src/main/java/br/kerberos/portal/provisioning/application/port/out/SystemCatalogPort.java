package br.kerberos.portal.provisioning.application.port.out;

import br.kerberos.portal.provisioning.domain.SystemSlug;
import br.kerberos.portal.provisioning.domain.SystemSpec;
import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Catálogo do que o portal criou. */
public interface SystemCatalogPort {

    record Stamped<T>(T value, Instant updatedAt, String updatedBy) {}

    Optional<Stamped<SystemSpec>> findSystem(SystemSlug slug);

    List<Stamped<SystemSpec>> listSystems();

    void saveSystem(SystemSpec system, Instant at, String actor);

    List<Stamped<TenantSpec>> listTenants(SystemSlug slug);

    void saveTenant(SystemSlug slug, TenantSpec tenant, Instant at, String actor);
}

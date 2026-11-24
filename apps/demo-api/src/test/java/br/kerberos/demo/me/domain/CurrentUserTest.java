package br.kerberos.demo.me.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CurrentUserTest {

    private final CurrentUser carla = new CurrentUser(
            "u-1", "carla", "carla@acme.test", List.of(new TenantId("acme"), new TenantId("globex")), Set.of("gestor"));

    @Test
    void membroAcessaOProprioTenant() {
        carla.assertCanAccess(new TenantId("acme"));
        assertThat(carla.belongsTo(new TenantId("globex"))).isTrue();
    }

    @Test
    void naoMembroERecusado() {
        assertThatThrownBy(() -> carla.assertCanAccess(new TenantId("initech")))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    void tenantRejeitaFormatoInvalido() {
        assertThatThrownBy(() -> new TenantId("Acme Corp")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TenantId("../etc")).isInstanceOf(IllegalArgumentException.class);
    }
}

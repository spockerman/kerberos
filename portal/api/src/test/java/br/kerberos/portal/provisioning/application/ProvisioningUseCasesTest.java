package br.kerberos.portal.provisioning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.kerberos.portal.provisioning.application.port.out.CurrentOperatorPort;
import br.kerberos.portal.provisioning.application.port.out.IdentityProvisioningPort;
import br.kerberos.portal.provisioning.application.port.out.ProvisioningAuditPort;
import br.kerberos.portal.provisioning.application.port.out.SystemCatalogPort;
import br.kerberos.portal.provisioning.application.usecase.SystemProvisioningService;
import br.kerberos.portal.provisioning.application.usecase.TenantProvisioningService;
import br.kerberos.portal.provisioning.domain.AuditEntry;
import br.kerberos.portal.provisioning.domain.InvalidSpecException;
import br.kerberos.portal.provisioning.domain.LoginMethod;
import br.kerberos.portal.provisioning.domain.NotAllowedException;
import br.kerberos.portal.provisioning.domain.Operator;
import br.kerberos.portal.provisioning.domain.SystemSlug;
import br.kerberos.portal.provisioning.domain.SystemSpec;
import br.kerberos.portal.provisioning.domain.TenantAlias;
import br.kerberos.portal.provisioning.domain.TenantSpec;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Casos de uso com fakes em memória das portas de saída — sem Spring, sem Keycloak, sem banco. */
class ProvisioningUseCasesTest {

    static class FakeIdentity implements IdentityProvisioningPort {
        final List<String> calls = new ArrayList<>();
        final List<Optional<String>> secrets = new ArrayList<>();
        boolean fail;

        @Override
        public void ensureSystem(SystemSpec system) {
            if (fail) throw new ProvisioningFailedException("Keycloak fora do ar", null);
            calls.add("system:" + system.slug());
        }

        @Override
        public void ensureTenant(SystemSpec system, TenantSpec tenant, Optional<String> secret) {
            calls.add("tenant:" + system.slug() + "/" + tenant.alias());
            secrets.add(secret);
        }
    }

    static class FakeCatalog implements SystemCatalogPort {
        final Map<String, Stamped<SystemSpec>> systems = new LinkedHashMap<>();
        final Map<String, Stamped<TenantSpec>> tenants = new LinkedHashMap<>();

        public Optional<Stamped<SystemSpec>> findSystem(SystemSlug slug) { return Optional.ofNullable(systems.get(slug.value())); }
        public List<Stamped<SystemSpec>> listSystems() { return List.copyOf(systems.values()); }
        public void saveSystem(SystemSpec s, Instant at, String actor) { systems.put(s.slug().value(), new Stamped<>(s, at, actor)); }
        public List<Stamped<TenantSpec>> listTenants(SystemSlug slug) {
            return tenants.entrySet().stream().filter(e -> e.getKey().startsWith(slug.value() + "/")).map(Map.Entry::getValue).toList();
        }
        public void saveTenant(SystemSlug slug, TenantSpec t, Instant at, String actor) { tenants.put(slug + "/" + t.alias(), new Stamped<>(t, at, actor)); }
    }

    static class FakeAudit implements ProvisioningAuditPort {
        final List<AuditEntry> entries = new ArrayList<>();
        public void record(AuditEntry e) { entries.add(e); }
        public List<AuditEntry> latest(int limit) { return entries; }
    }

    final FakeIdentity identity = new FakeIdentity();
    final FakeCatalog catalog = new FakeCatalog();
    final FakeAudit audit = new FakeAudit();
    Operator operator = new Operator("operador", true);
    final CurrentOperatorPort operators = () -> operator;
    final Clock clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC);

    SystemProvisioningService systems;
    TenantProvisioningService tenants;

    @BeforeEach
    void setUp() {
        systems = new SystemProvisioningService(identity, catalog, audit, operators, clock, "https://auth.empresa.com");
        tenants = new TenantProvisioningService(identity, catalog, audit, operators, clock, "https://auth.empresa.com");
    }

    static SystemSpec financeiro() {
        return new SystemSpec(new SystemSlug("financeiro"), "Financeiro", URI.create("https://fin.empresa.com"), List.of(), true);
    }

    static TenantSpec initechOidc() {
        return new TenantSpec(new TenantAlias("initech"), "Initech", List.of("initech.test"), new LoginMethod.Oidc(
                URI.create("https://idp/x"), URI.create("https://idp/auth"), URI.create("https://idp/token"),
                URI.create("https://idp/certs"), null, null, "kerberos"));
    }

    @Test
    void criaSistemaAplicaNoProvedorGravaCatalogoEAudita() {
        var view = systems.execute(financeiro());

        assertThat(identity.calls).containsExactly("system:financeiro");
        assertThat(view.webClientId()).isEqualTo("financeiro-web");
        assertThat(view.updatedBy()).isEqualTo("operador");
        assertThat(audit.entries).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo("SYSTEM_CREATED");
            assertThat(e.outcome()).isEqualTo(AuditEntry.Outcome.SUCCESS);
        });
    }

    @Test
    void repetirOCadastroEIdempotente() {
        systems.execute(financeiro());
        systems.execute(financeiro());

        assertThat(catalog.systems).hasSize(1);
        assertThat(audit.entries).extracting(AuditEntry::action).containsExactly("SYSTEM_CREATED", "SYSTEM_UPDATED");
    }

    @Test
    void falhaNoProvedorEAuditadaENaoEntraNoCatalogo() {
        identity.fail = true;

        assertThatThrownBy(() -> systems.execute(financeiro())).isInstanceOf(ProvisioningFailedException.class);
        assertThat(catalog.systems).isEmpty();
        assertThat(audit.entries).singleElement().extracting(AuditEntry::outcome).isEqualTo(AuditEntry.Outcome.FAILURE);
    }

    @Test
    void semRoleDeProvisionamentoNadaAcontece() {
        operator = new Operator("visitante", false);

        assertThatThrownBy(() -> systems.execute(financeiro())).isInstanceOf(NotAllowedException.class);
        assertThat(identity.calls).isEmpty();
    }

    @Test
    void clienteOidcExigeSegredoNaCriacaoEOSegredoNaoVaiParaOCatalogo() {
        systems.execute(financeiro());

        assertThatThrownBy(() -> tenants.execute("financeiro", initechOidc(), Optional.empty()))
                .isInstanceOf(InvalidSpecException.class);

        var view = tenants.execute("financeiro", initechOidc(), Optional.of("s3gredo"));

        assertThat(identity.secrets).containsExactly(Optional.of("s3gredo"));
        assertThat(view.identityProviderRedirectUri())
                .isEqualTo("https://auth.empresa.com/realms/financeiro/broker/initech-idp/endpoint");
        assertThat(audit.entries.getLast().detail()).doesNotContain("s3gredo");
    }

    @Test
    void atualizarClienteSemSegredoMantemOAtual() {
        systems.execute(financeiro());
        tenants.execute("financeiro", initechOidc(), Optional.of("s3gredo"));

        tenants.execute("financeiro", initechOidc(), Optional.empty());

        assertThat(identity.secrets).containsExactly(Optional.of("s3gredo"), Optional.empty());
    }

    @Test
    void clienteEmSistemaInexistente() {
        assertThatThrownBy(() -> tenants.execute("nao-existe", initechOidc(), Optional.of("x")))
                .isInstanceOf(SystemNotFoundException.class);
    }

    @Test
    void envFileDoSistema() {
        systems.execute(financeiro());

        assertThat(systems.envFile("financeiro")).contains("KEYCLOAK_CLIENT_ID=financeiro-web");
    }
}

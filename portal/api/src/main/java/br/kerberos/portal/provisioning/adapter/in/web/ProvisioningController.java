package br.kerberos.portal.provisioning.adapter.in.web;

import br.kerberos.portal.provisioning.application.port.in.AddTenant;
import br.kerberos.portal.provisioning.application.port.in.QueryAuditTrail;
import br.kerberos.portal.provisioning.application.port.in.QuerySystems;
import br.kerberos.portal.provisioning.application.port.in.QueryTenants;
import br.kerberos.portal.provisioning.application.port.in.RegisterSystem;
import br.kerberos.portal.provisioning.application.port.in.SystemView;
import br.kerberos.portal.provisioning.application.port.in.TenantView;
import br.kerberos.portal.provisioning.domain.AuditEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adapter HTTP do portal. Só traduz JSON ↔ casos de uso. */
@RestController
@RequestMapping("/api")
public class ProvisioningController {

    private final RegisterSystem registerSystem;
    private final QuerySystems querySystems;
    private final AddTenant addTenant;
    private final QueryTenants queryTenants;
    private final QueryAuditTrail auditTrail;

    public ProvisioningController(
            RegisterSystem registerSystem,
            QuerySystems querySystems,
            AddTenant addTenant,
            QueryTenants queryTenants,
            QueryAuditTrail auditTrail) {
        this.registerSystem = registerSystem;
        this.querySystems = querySystems;
        this.addTenant = addTenant;
        this.queryTenants = queryTenants;
        this.auditTrail = auditTrail;
    }

    @GetMapping("/systems")
    public List<SystemView> systems() {
        return querySystems.list();
    }

    /** Cria ou atualiza (idempotente): enviar o mesmo cadastro de novo conserta o que estiver faltando. */
    @PostMapping("/systems")
    public SystemView registerSystem(@RequestBody SystemRequest request) {
        return registerSystem.execute(request.toSpec());
    }

    @GetMapping("/systems/{slug}")
    public SystemView system(@PathVariable String slug) {
        return querySystems.get(slug);
    }

    @GetMapping(value = "/systems/{slug}/env", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> envFile(@PathVariable String slug, @RequestParam(defaultValue = "false") boolean download) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN);
        if (download) {
            response.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + slug + ".env\"");
        }
        return response.body(querySystems.envFile(slug));
    }

    @GetMapping("/systems/{slug}/tenants")
    public List<TenantView> tenants(@PathVariable String slug) {
        return queryTenants.list(slug);
    }

    @PostMapping("/systems/{slug}/tenants")
    public TenantView addTenant(@PathVariable String slug, @RequestBody TenantRequest request) {
        return addTenant.execute(slug, request.toSpec(), Optional.ofNullable(request.secret()));
    }

    @GetMapping("/audit")
    public List<AuditEntry> audit(@RequestParam(defaultValue = "100") int limit) {
        return auditTrail.latest(limit);
    }
}

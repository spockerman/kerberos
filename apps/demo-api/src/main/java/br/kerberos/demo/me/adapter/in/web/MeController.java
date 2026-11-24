package br.kerberos.demo.me.adapter.in.web;

import br.kerberos.demo.me.application.port.in.GetCurrentUserProfile;
import br.kerberos.demo.me.application.port.in.GetTenantDashboard;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada HTTP: converte requisição em chamada de caso de uso. Sem regra de negócio. */
@RestController
@RequestMapping("/api")
public class MeController {

    private final GetCurrentUserProfile getCurrentUserProfile;
    private final GetTenantDashboard getTenantDashboard;

    public MeController(GetCurrentUserProfile getCurrentUserProfile, GetTenantDashboard getTenantDashboard) {
        this.getCurrentUserProfile = getCurrentUserProfile;
        this.getTenantDashboard = getTenantDashboard;
    }

    @GetMapping("/me")
    public MeResponse me() {
        GetCurrentUserProfile.UserProfile profile = getCurrentUserProfile.execute();
        return new MeResponse(profile.userId(), profile.username(), profile.email(), profile.tenants(), profile.roles());
    }

    @GetMapping("/tenants/{tenant}/dashboard")
    public TenantDashboardResponse dashboard(@PathVariable String tenant) {
        GetTenantDashboard.TenantDashboard dashboard = getTenantDashboard.execute(tenant);
        return new TenantDashboardResponse(dashboard.tenant(), dashboard.message(), dashboard.canManage());
    }

    public record MeResponse(String userId, String username, String email, List<String> tenants, Set<String> roles) {}

    public record TenantDashboardResponse(String tenant, String message, boolean canManage) {}
}

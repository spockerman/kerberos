package br.kerberos.demo.me.application.usecase;

import br.kerberos.demo.me.application.NotAuthenticatedException;
import br.kerberos.demo.me.application.port.in.GetTenantDashboard;
import br.kerberos.demo.me.application.port.out.CurrentUserPort;
import br.kerberos.demo.me.domain.CurrentUser;
import br.kerberos.demo.me.domain.TenantId;

public class GetTenantDashboardService implements GetTenantDashboard {

    private final CurrentUserPort currentUserPort;

    public GetTenantDashboardService(CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
    }

    @Override
    public TenantDashboard execute(String tenant) {
        CurrentUser user = currentUserPort.currentUser().orElseThrow(NotAuthenticatedException::new);
        TenantId tenantId = new TenantId(tenant);

        user.assertCanAccess(tenantId);

        boolean canManage = user.hasRole("admin") || user.hasRole("gestor");
        return new TenantDashboard(tenantId.value(), "Olá, " + user.username() + "! Dados do tenant " + tenantId, canManage);
    }
}

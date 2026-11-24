package br.kerberos.demo.me.application.port.in;

/** Caso de uso de exemplo com isolamento de tenant: só membros do tenant recebem os dados. */
public interface GetTenantDashboard {

    TenantDashboard execute(String tenant);

    record TenantDashboard(String tenant, String message, boolean canManage) {}
}

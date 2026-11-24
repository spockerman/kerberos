package br.kerberos.demo.me.domain;

/** O usuário tentou acessar dados de um tenant do qual não é membro. */
public class TenantAccessDeniedException extends RuntimeException {

    private final TenantId tenant;

    public TenantAccessDeniedException(TenantId tenant) {
        super("Acesso negado ao tenant " + tenant);
        this.tenant = tenant;
    }

    public TenantId tenant() {
        return tenant;
    }
}

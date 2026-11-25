package br.kerberos.portal.provisioning.application.port.in;

import java.util.List;

public interface QueryTenants {

    List<TenantView> list(String systemSlug);
}

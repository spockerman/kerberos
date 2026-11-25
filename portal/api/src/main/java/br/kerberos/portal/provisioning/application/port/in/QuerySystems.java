package br.kerberos.portal.provisioning.application.port.in;

import java.util.List;

/** Consultas do catálogo: lista, detalhe e o .env pronto para o projeto. */
public interface QuerySystems {

    List<SystemView> list();

    SystemView get(String slug);

    String envFile(String slug);
}

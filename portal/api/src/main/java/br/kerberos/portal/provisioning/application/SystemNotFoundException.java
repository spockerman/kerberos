package br.kerberos.portal.provisioning.application;

public class SystemNotFoundException extends RuntimeException {

    public SystemNotFoundException(String slug) {
        super("Sistema não encontrado: " + slug);
    }
}

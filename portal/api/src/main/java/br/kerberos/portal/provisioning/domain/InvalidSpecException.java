package br.kerberos.portal.provisioning.domain;

/** Dados de cadastro que violam uma regra da plataforma. */
public class InvalidSpecException extends RuntimeException {

    public InvalidSpecException(String message) {
        super(message);
    }
}

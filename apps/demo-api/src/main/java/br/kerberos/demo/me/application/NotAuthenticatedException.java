package br.kerberos.demo.me.application;

/** Não há usuário autenticado no contexto da requisição. */
public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("Usuário não autenticado");
    }
}

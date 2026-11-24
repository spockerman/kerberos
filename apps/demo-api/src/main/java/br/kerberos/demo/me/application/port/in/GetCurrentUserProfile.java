package br.kerberos.demo.me.application.port.in;

import java.util.List;
import java.util.Set;

/** Caso de uso: "quem sou eu?" — dados do usuário autenticado, seus tenants e roles. */
public interface GetCurrentUserProfile {

    UserProfile execute();

    record UserProfile(String userId, String username, String email, List<String> tenants, Set<String> roles) {}
}

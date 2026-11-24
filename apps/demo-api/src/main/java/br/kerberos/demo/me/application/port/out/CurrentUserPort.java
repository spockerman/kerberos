package br.kerberos.demo.me.application.port.out;

import br.kerberos.demo.me.domain.CurrentUser;
import java.util.Optional;

/** De onde vem o usuário autenticado. Implementado pelo adapter de segurança (token JWT). */
public interface CurrentUserPort {

    Optional<CurrentUser> currentUser();
}

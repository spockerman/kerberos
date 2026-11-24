package br.kerberos.demo.me.application.usecase;

import br.kerberos.demo.me.application.NotAuthenticatedException;
import br.kerberos.demo.me.application.port.in.GetCurrentUserProfile;
import br.kerberos.demo.me.application.port.out.CurrentUserPort;
import br.kerberos.demo.me.domain.CurrentUser;
import br.kerberos.demo.me.domain.TenantId;

public class GetCurrentUserProfileService implements GetCurrentUserProfile {

    private final CurrentUserPort currentUserPort;

    public GetCurrentUserProfileService(CurrentUserPort currentUserPort) {
        this.currentUserPort = currentUserPort;
    }

    @Override
    public UserProfile execute() {
        CurrentUser user = currentUserPort.currentUser().orElseThrow(NotAuthenticatedException::new);
        return new UserProfile(
                user.userId(),
                user.username(),
                user.email(),
                user.tenants().stream().map(TenantId::value).toList(),
                user.roles());
    }
}

package br.kerberos.demo.config;

import br.kerberos.demo.me.adapter.out.security.JwtCurrentUserAdapter;
import br.kerberos.demo.me.application.port.in.GetCurrentUserProfile;
import br.kerberos.demo.me.application.port.in.GetTenantDashboard;
import br.kerberos.demo.me.application.port.out.CurrentUserPort;
import br.kerberos.demo.me.application.usecase.GetCurrentUserProfileService;
import br.kerberos.demo.me.application.usecase.GetTenantDashboardService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root: liga os adapters concretos às portas dos casos de uso. */
@Configuration
public class UseCaseConfig {

    @Bean
    CurrentUserPort currentUserPort() {
        return new JwtCurrentUserAdapter();
    }

    @Bean
    GetCurrentUserProfile getCurrentUserProfile(CurrentUserPort currentUserPort) {
        return new GetCurrentUserProfileService(currentUserPort);
    }

    @Bean
    GetTenantDashboard getTenantDashboard(CurrentUserPort currentUserPort) {
        return new GetTenantDashboardService(currentUserPort);
    }
}

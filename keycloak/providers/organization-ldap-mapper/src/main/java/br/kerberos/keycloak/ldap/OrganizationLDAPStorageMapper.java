package br.kerberos.keycloak.ldap;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;

/**
 * O Keycloak não liga federação LDAP a uma Organization. Este mapper faz a ligação:
 * todo usuário importado do LDAP do cliente vira membro da Organization (tenant) do cliente,
 * e o token passa a trazer o claim {@code organization} como acontece com os demais usuários.
 */
public class OrganizationLDAPStorageMapper extends AbstractLDAPStorageMapper {

    static final String ORGANIZATION_ALIAS = "organization.alias";

    private static final Logger LOG = Logger.getLogger(OrganizationLDAPStorageMapper.class);

    public OrganizationLDAPStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        KeycloakSession session = getSession();
        RealmModel previousRealm = session.getContext().getRealm();
        if (previousRealm == null) {
            // Sincronização em segundo plano não tem realm no contexto; o OrganizationProvider precisa dele
            session.getContext().setRealm(realm);
        }
        try {
            addMembership(session, user);
        } finally {
            if (previousRealm == null) {
                session.getContext().setRealm(null);
            }
        }
    }

    private void addMembership(KeycloakSession session, UserModel user) {
        String alias = mapperModel.getConfig().getFirst(ORGANIZATION_ALIAS);
        OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
        if (organizations == null || alias == null) {
            return;
        }

        OrganizationModel organization = organizations.getAllStream()
                .filter(org -> alias.equals(org.getAlias()))
                .findFirst()
                .orElse(null);
        if (organization == null) {
            LOG.warnf("Organization '%s' não encontrada; usuário %s não foi vinculado", alias, user.getUsername());
            return;
        }

        if (organizations.getMemberById(organization, user.getId()) == null) {
            organizations.addMember(organization, user);
            LOG.debugf("Usuário %s adicionado à organization %s", user.getUsername(), alias);
        }
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        // Federação somente leitura: nada a gravar no LDAP do cliente
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return delegate;
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        // Nenhum atributo LDAP extra é necessário
    }
}

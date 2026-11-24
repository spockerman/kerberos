package br.kerberos.keycloak.ldap;

import java.util.List;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;

/**
 * Registra o mapper "organization-membership-ldap-mapper".
 * Configurado pelo Terraform (módulo tenant) em cada federação LDAP de cliente.
 */
public class OrganizationLDAPStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "organization-membership-ldap-mapper";

    private static final List<ProviderConfigProperty> CONFIG = List.of(createConfigProperty(
            OrganizationLDAPStorageMapper.ORGANIZATION_ALIAS,
            "Organization alias",
            "Alias da Organization que recebe como membros os usuários importados deste LDAP.",
            ProviderConfigProperty.STRING_TYPE,
            null,
            true));

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Adiciona cada usuário importado deste LDAP como membro da Organization informada.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        ConfigurationValidationHelper.check(config)
                .checkRequired(OrganizationLDAPStorageMapper.ORGANIZATION_ALIAS, "Organization alias");
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new OrganizationLDAPStorageMapper(mapperModel, federationProvider);
    }
}

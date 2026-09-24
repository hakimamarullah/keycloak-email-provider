package com.starline.keycloak.email;

import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class FREmailTemplateProviderFactory implements EmailTemplateProviderFactory {

    private Publisher publisher;

    @Override
    public void init(Config.Scope config) {
        this.publisher = new ArtemisPublisher(DefaultArtemisConfig.fromEnv());
    }

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        return new FREmailTemplateProvider(session, publisher);
    }

    @Override
    public void close() {
        publisher.close();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No-op
    }

    @Override
    public String getId() {
        return "fr-email-provider";
    }
}

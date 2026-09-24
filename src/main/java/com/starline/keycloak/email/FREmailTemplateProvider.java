package com.starline.keycloak.email;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FREmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    private final Publisher publisher;

    public FREmailTemplateProvider(KeycloakSession session, Publisher publisher) {
        super(session);
        this.publisher = publisher;
    }


    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        dispatch("VERIFY_EMAIL", linkParams(link, expirationInMinutes));
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        dispatch("RESET_PASSWORD", linkParams(link, expirationInMinutes));
    }


    private Map<String, Object> linkParams(String link, long expirationInMinutes) {
        Map<String, Object> p = new HashMap<>();
        p.put("link", link);
        p.put("linkExpirationMinutes", expirationInMinutes);
        return p;
    }

    private void dispatch(String eventCode, Map<String, Object> params) throws EmailException {
        params.putIfAbsent("username", user.getUsername());
        params.putIfAbsent("firstName", user.getFirstName());
        params.putIfAbsent("lastName", user.getLastName());
        params.putIfAbsent("email", user.getEmail());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event_code", eventCode);
        envelope.put("to", user.getEmail());
        envelope.put("realm", realm.getName());
        envelope.put("locale", user.getFirstAttribute("locale"));
        envelope.put("params", params);

        try {
            String json = JsonSerialization.writeValueAsString(envelope);

            Publisher.MessageProperties properties = new Publisher.MessageProperties(
                    "EMAIL",
                    eventCode,
                    realm.getId(),
                    realm.getName(),
                    authenticationSession != null ? authenticationSession.getClient().getClientId() : null,
                    user.getId()
            );

            publisher.publish(json, properties);
        } catch (ArtemisPublisher.ArtemisPublishException e) {
            throw new EmailException("Failed to publish email event to Artemis", e);
        } catch (Exception e) {
            throw new EmailException("Failed to build/publish email event", e);
        }
    }
}
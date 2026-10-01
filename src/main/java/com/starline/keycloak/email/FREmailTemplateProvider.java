package com.starline.keycloak.email;

import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class FREmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    public static final String EMAIL_EVENT_KIND = "EMAIL";
    public static final String RESET_PASSWORD_EVENT_CODE = "RESET_PASSWORD";
    public static final String VERIFY_EMAIL_EVENT_CODE = "VERIFY_EMAIL";
    private final Publisher publisher;

    public FREmailTemplateProvider(KeycloakSession session, Publisher publisher) {
        super(session);
        this.publisher = publisher;
    }


    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        dispatch(VERIFY_EMAIL_EVENT_CODE, linkParams(link, expirationInMinutes));
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        dispatch(RESET_PASSWORD_EVENT_CODE, linkParams(link, expirationInMinutes));
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
        params.put("userId", user.getId());

        var locale = session.getContext().resolveLocale(user,
                Boolean.parseBoolean(String.valueOf(attributes.get(Constants.IGNORE_ACCEPT_LANGUAGE_HEADER))));
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventCode", eventCode);
        envelope.put("to", user.getEmail());
        envelope.put("realm", realm.getName());
        envelope.put("locale", Optional.ofNullable(locale)
                .map(Locale::getLanguage)
                .orElse(null));
        envelope.put("params", params);

        try {
            String json = JsonSerialization.writeValueAsString(envelope);

            Publisher.MessageProperties properties = new Publisher.MessageProperties(
                    EMAIL_EVENT_KIND,
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
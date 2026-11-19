package com.vitorbetmann.schoolapi.usecases.interfaces;

/**
 * Port for sending a message to the administrators.
 */
public interface NotificationGateway {

    /**
     * Publishes a plain-text message to the administrators.
     *
     * @param subject the message subject
     * @param message the message body
     */
    void publish(String subject, String message);
}

package com.lorettabank.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email delivery seam. The default implementation only logs a redacted line; plug in SES or another
 * provider behind this interface. Message bodies are never logged because they contain amounts and
 * account numbers.
 */
@Component
@Slf4j
public class EmailSender {

    public void send(String to, String subject, String body) {
        log.info("Email queued to {} subject='{}'", mask(to), subject);
    }

    static String mask(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}

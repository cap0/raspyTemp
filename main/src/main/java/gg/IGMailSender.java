package gg;

import javax.mail.MessagingException;

public interface IGMailSender {

    void sendAlarm(String subject, String emailBody) throws MessagingException;
}

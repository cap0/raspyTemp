package gg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import static gg.util.Constants.*;

class GMailSender implements  IGMailSender{

    private static final Logger logger = LogManager.getLogger(GMailSender.class);

    private final String mailUsername;
    private final String mailPassword;
    private final String mailReceiver;
    private final Properties mailServerProperties;

    public GMailSender() {
       mailUsername = null;
       mailPassword = null;
       mailReceiver = null;
       mailServerProperties = null;
    }

    GMailSender(Properties p) {
        mailUsername = p.getProperty(MAIL_USERNAME);
        mailPassword = p.getProperty(MAIL_PASSWORD);
        mailReceiver = p.getProperty(MAIL_RECEIVER);

        mailServerProperties = getServerMailProperties();
    }

    @Override
    public void sendAlarm(String subject, String emailBody) throws MessagingException {
        logger.debug("get Mail Session..");
        Session getMailSession = Session.getDefaultInstance(mailServerProperties, null);
        MimeMessage generateMailMessage = new MimeMessage(getMailSession);

        generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(mailReceiver));
        generateMailMessage.setSubject(subject);
        generateMailMessage.setContent(emailBody, "text/html");

        logger.debug("Mail Session has been created successfully..");

        logger.debug(" Get Session and Send mail");
        Transport transport = getMailSession.getTransport("smtp");

        // Enter your correct gmail UserID and Password
        // if you have 2FA enabled then provide App Specific Password
        transport.connect("smtp.gmail.com", mailUsername, mailPassword);
        transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
        transport.close();
    }

    private Properties getServerMailProperties() {
        logger.debug("setup Mail Server Properties..");
        Properties mailProps = System.getProperties();
        mailProps.put("mail.smtp.port", "587");
        mailProps.put("mail.smtp.auth", "true");
        mailProps.put("mail.smtp.starttls.enable", "true");
        logger.debug("Mail Server Properties have been setup successfully..");
        return mailProps;
    }
}

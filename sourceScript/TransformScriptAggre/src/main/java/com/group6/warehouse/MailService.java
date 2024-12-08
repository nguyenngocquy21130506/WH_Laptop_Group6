package com.group6.warehouse;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.io.IOException;

public class MailService {
    private String fromEmail;
    private String password;
    private String toEmail;

    public MailService() {
        try {
            Properties properties = new Properties();
            InputStream input = MailService.class.getClassLoader().getResourceAsStream("config.properties");
            properties.load(input);

            this.fromEmail = properties.getProperty("mail.service.from.email");
            this.password = properties.getProperty("mail.service.password");
            this.toEmail = properties.getProperty("mail.service.to.email");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEmail(String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "*");

        Session sessionMail = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(sessionMail);
            message.setFrom(new InternetAddress(fromEmail, "Price_LapTop"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(body, "text/plain; charset=UTF-8");

            Transport.send(message);

            System.out.println("Sent message successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

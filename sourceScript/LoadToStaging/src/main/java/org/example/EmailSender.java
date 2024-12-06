package org.example;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailSender {
    private final String fromEmail; // Địa chỉ email người gửi
    private final String emailPassword; // Mật khẩu email của người gửi

    public EmailSender(String fromEmail, String emailPassword) {
        this.fromEmail = fromEmail;
        this.emailPassword = emailPassword;
    }

    public void sendEmail(String toEmail, String subject, String body) {
        // Thiết lập thuộc tính cho email
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Tạo phiên email
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, emailPassword);
            }
        });

        try {
            // Tạo đối tượng MimeMessage
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail)); // Địa chỉ email người gửi
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail)); // Địa chỉ email người nhận
            message.setSubject(subject); // Tiêu đề email
            message.setText(body); // Nội dung email

            // Gửi email
            Transport.send(message);
            System.out.println("Email đã được gửi thành công đến " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Gửi email thất bại: " + e.getMessage());
        }
    }

    // Phương thức main để thử nghiệm
    public static void main(String[] args) {
        // Thay đổi thông tin email và mật khẩu dưới đây
        String fromEmail = "Thongnhat73845@gmail.com"; // Địa chỉ email người gửi
        String emailPassword = "fcub ezue ztaz syml"; // Mật khẩu email của bạn

        // Tạo đối tượng EmailSender
        EmailSender emailSender = new EmailSender(fromEmail, emailPassword);

        // Gửi email thử nghiệm
        String toEmail = "21130453@st.hcmuaf.edu.vn"; // Địa chỉ email người nhận
        String subject = "Test Email";
        String body = "This is a test email sent from Java.";

        emailSender.sendEmail(toEmail, subject, body);
    }
}


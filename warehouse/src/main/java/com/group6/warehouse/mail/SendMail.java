package com.group6.warehouse.mail;

import com.group6.warehouse.control.model.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@Service
public class SendMail {
    JavaMailSender javaMailSender;

    public boolean sendEmail(String recipient, Log log) throws MessagingException {
        MimeMessage mimeMailMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMailMessage, true, "utf-8");

        // Tạo nội dung HTML cho thông báo log lỗi
        String html = "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; }" +
                ".container { width: 100%; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }" +
                ".header { background-color: #f4f4f4; padding: 10px; text-align: center; border-radius: 5px 5px 0 0; }" +
                ".content { padding: 15px; }" +
                ".footer { text-align: center; margin-top: 20px; font-size: 12px; color: #888; }" +
                ".error { color: red; font-weight: bold; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>Thông báo lỗi từ Warehouse System</h2>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Chào bạn,</p>" +
                "<p>Đã xảy ra lỗi trong hệ thống. Dưới đây là thông tin chi tiết:</p>" +
                "<p><strong>Tên tác vụ:</strong> " + log.getTaskName() + "</p>" +
                "<p><strong>Trạng thái:</strong> <span class='error'>" + log.getStatus() + "</span></p>" +
                "<p><strong>Thông điệp:</strong> " + log.getMessage() + "</p>" +
                "<p><strong>Cấp độ:</strong> " + log.getLevel() + "</p>" +
                "<p><strong>Thời gian tạo:</strong> " + log.getCreatedAt() + "</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2024 WarehouseSystem. Tất cả quyền được bảo lưu.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        try {
            helper.setTo(recipient);
            helper.setSubject("Thông báo từ Warehouse System!");
            helper.setText(html, true);
            javaMailSender.send(mimeMailMessage);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}

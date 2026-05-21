package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================
    // OLD (link kiểu reservationId&phone) - giữ để không vỡ code
    // Khuyến nghị: chuyển code gọi sang method WithOrderLink
    // =========================
    @Override
    public void sendReservationConfirmedEmail(
            String to,
            String customerName,
            String date,
            String time,
            Integer guests,
            Integer reservationId,
            String customerPhone
    ) {
        // fallback link cũ (không khuyến nghị)
        String orderLink = "http://localhost:9090/user/order.html?reservationId="
                + reservationId + "&phone=" + customerPhone;

        sendReservationConfirmedEmailWithOrderLink(
                to, customerName, date, time, guests, reservationId, customerPhone, orderLink
        );
    }

    @Override
    public void sendReservationConfirmedEmailWithOrderLink(
            String to,
            String customerName,
            String date,
            String time,
            Integer guests,
            Integer reservationId,
            String customerPhone,
            String orderLink
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Xác nhận đặt bàn thành công");
        message.setText(
                "Xin chào " + customerName + ",\n\n" +
                        "Nhà hàng đã xác nhận đặt bàn của bạn.\n" +
                        "Mã đặt bàn: " + reservationId + "\n" +
                        "Ngày: " + date + "\n" +
                        "Giờ: " + time + "\n" +
                        "Số khách: " + guests + "\n" +
                        "Số điện thoại: " + customerPhone + "\n\n" +
                        "Bạn có thể đặt món trước tại link bên dưới (có hiệu lực 30 phút):\n" +
                        orderLink + "\n\n" +
                        "Vui lòng đến đúng giờ. Nếu quá 10 phút mà chưa check-in, bàn có thể bị tự động hủy.\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    @Override
    public void sendReservationCancelledEmail(String to, String customerName, String date, String time) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Đặt bàn đã bị hủy");
        message.setText(
                "Xin chào " + customerName + ",\n\n" +
                        "Đặt bàn của bạn vào lúc " + time + " ngày " + date +
                        " đã bị hủy do quá 10 phút mà chưa check-in.\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    // OLD
    @Override
    public void sendDepositPaidEmail(String to, String customerName, Integer reservationId, String date, String time, BigDecimal depositAmount) {
        // fallback link cũ (không khuyến nghị)
        String orderLink = "http://localhost:9090/user/order.html?reservationId=" + reservationId;

        sendDepositPaidEmailWithOrderLink(to, customerName, reservationId, date, time, depositAmount, orderLink);
    }

    // ✅ NEW
    @Override
    public void sendDepositPaidEmailWithOrderLink(String to, String customerName, Integer reservationId, String date, String time, BigDecimal depositAmount, String orderLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Đặt cọc thành công - Đặt bàn đã được xác nhận");
        message.setText(
                "Xin chào " + customerName + ",\n\n" +
                        "Bạn đã thanh toán cọc thành công cho đặt bàn #" + reservationId + ".\n" +
                        "Ngày: " + date + "\n" +
                        "Giờ: " + time + "\n" +
                        "Số tiền cọc: " + depositAmount.toPlainString() + " VND\n\n" +
                        "Đặt bàn của bạn đã được xác nhận.\n\n" +
                        "Bạn có thể đặt món trước tại link bên dưới (có hiệu lực 30 phút):\n" +
                        orderLink + "\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    @Override
    public void sendDepositRefundedEmail(String to, String customerName, Integer reservationId, BigDecimal depositAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Hoàn cọc đặt bàn");
        message.setText(
                "Xin chào " + customerName + ",\n\n" +
                        "Khoản cọc cho đặt bàn #" + reservationId + " đã được hoàn.\n" +
                        "Số tiền hoàn: " + depositAmount.toPlainString() + " VND\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    @Override
    public void sendReservationExpiredEmail(String to,
                                            String customerName,
                                            Integer reservationId,
                                            String date,
                                            String time,
                                            BigDecimal depositAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Đặt bàn đã bị hủy do quá hạn thanh toán cọc");
        message.setText(
                "Xin chào " + customerName + ",\n\n" +
                        "Yêu cầu đặt bàn #" + reservationId + " của bạn đã bị hủy vì chưa hoàn tất thanh toán cọc trong thời gian quy định.\n\n" +
                        "Ngày: " + date + "\n" +
                        "Giờ: " + time + "\n" +
                        "Số tiền cọc: " + (depositAmount != null ? depositAmount.toPlainString() : "0") + " VND\n\n" +
                        "Nếu bạn vẫn muốn sử dụng dịch vụ, vui lòng tạo một yêu cầu đặt bàn mới trên hệ thống.\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset mật khẩu - Hương Việt");
        message.setText(
                "Xin chào,\n\n" +
                        "Bạn vừa yêu cầu đặt lại mật khẩu.\n\n" +
                        "Vui lòng mở link sau để đặt mật khẩu mới (hết hạn sau 15 phút):\n" +
                        resetLink + "\n\n" +
                        "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n" +
                        "Trân trọng."
        );
        mailSender.send(message);
    }

    @Override
    public void sendDepositRequestEmail(String email, String name, String paymentUrl, String date, String time) {
        String subject = "Thanh toán tiền cọc đặt bàn";
        String body = "<p>Chào " + name + ",</p>"
                + "<p>Bạn hãy <b>thanh toán cọc</b> bằng đường dẫn sau: "
                + "<a href='" + paymentUrl + "'>Thanh toán ngay</a></p>"
                + "<p>Thời gian: " + date + " " + time + "</p>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(body, true); // gửi mail dạng HTML
            mailSender.send(message);
        } catch (Exception e) {
            // log lỗi nếu cần
            e.printStackTrace();
        }
    }

    @Override
    public void sendLowRatingReviewAutoReply(String to, String customerName, String messageText) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Xin lỗi về trải nghiệm của bạn - Hương Việt");
        message.setText(
                "Xin chào " + (customerName != null ? customerName : "bạn") + ",\n\n"
                        + (messageText != null ? messageText : "") + "\n\n"
                        + "Trân trọng,\nHuongViet"
        );
        mailSender.send(message);
    }
}
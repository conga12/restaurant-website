package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

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
        String orderLink = "http://localhost:9090/user/order.html?reservationId="
                + reservationId + "&phone=" + customerPhone;

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
                        "Bạn có thể đặt món trước tại link bên dưới:\n" +
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

    @Override
    public void sendDepositPaidEmail(String to, String customerName, Integer reservationId, String date, String time, BigDecimal depositAmount) {
        String orderLink = "http://localhost:9090/user/order.html?reservationId=" + reservationId;

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
                        "Bạn có thể đặt món trước tại link bên dưới:\n" +
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
}
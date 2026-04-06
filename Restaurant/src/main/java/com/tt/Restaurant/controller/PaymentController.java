package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.Orders;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.repository.OrderRepository;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import com.tt.Restaurant.service.EmailService;
import com.tt.Restaurant.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final EmailService emailService;

    public PaymentController(
            VNPayService vnPayService,
            OrderRepository orderRepository,
            ReservationRepository reservationRepository,
            RestaurantTableRepository tableRepository,
            EmailService emailService
    ) {
        this.vnPayService = vnPayService;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.emailService = emailService;
    }

    // ================= ORDER PAYMENT =================
    @PostMapping("/vnpay/create")
    public Map<String, String> createPayment(@RequestParam Integer orderId, HttpServletRequest request) {
        Orders order = orderRepository.findById(orderId.longValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (order.getTotalAmount() == null || order.getTotalAmount().doubleValue() <= 0) {
            throw new RuntimeException("Số tiền thanh toán không hợp lệ");
        }

        String paymentUrl = vnPayService.createPaymentUrl(
                "ORD_" + order.getId(),
                order.getTotalAmount().longValue(),
                request
        );

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        return response;
    }

    // ================= RETURN =================
    @GetMapping("/vnpay-return")
    public void vnpayReturn(@RequestParam Map<String, String> params,
                            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        String secureHash = params.get("vnp_SecureHash");

        if (!vnPayService.verifySignature(params, secureHash)) {
            response.sendRedirect("/user/payment-result.html?status=invalid");
            return;
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if (txnRef == null || txnRef.isBlank()) {
            response.sendRedirect("/user/payment-result.html?status=notfound");
            return;
        }

        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        // ================= ORDER =================
        if (txnRef.startsWith("ORD_")) {
            Integer orderId = Integer.parseInt(txnRef.replace("ORD_", ""));

            Orders order = orderRepository.findById(orderId.longValue())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

            if (success) {
                if (order.getPaymentStatus() != Orders.PaymentStatus.PAID) {
                    order.setPaymentStatus(Orders.PaymentStatus.PAID);
                    order.setPaidAt(LocalDateTime.now());
                    order.setPaymentTxnRef(txnRef);
                    orderRepository.save(order);
                }

                response.sendRedirect("/user/payment-result.html?status=success&type=order&id=" + orderId);
                return;
            }

            response.sendRedirect("/user/payment-result.html?status=failed&type=order&id=" + orderId);
            return;
        }

        // ================= RESERVATION (CỌC) =================
        if (txnRef.startsWith("RES_")) {
            Integer reservationId = Integer.parseInt(txnRef.replace("RES_", ""));

            Reservation reservation = reservationRepository.findById(reservationId.longValue())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy reservation"));

            if (success) {
                reservation.setDepositStatus(Reservation.DepositStatus.PAID);
                reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);

                if (reservation.getTable() != null) {
                    RestaurantTable table = reservation.getTable();
                    table.setStatus(RestaurantTable.TableStatus.RESERVED);
                    tableRepository.save(table);
                }

                reservationRepository.save(reservation);

                if (reservation.getCustomerEmail() != null && !reservation.getCustomerEmail().isBlank()) {
                    emailService.sendDepositPaidEmail(
                            reservation.getCustomerEmail(),
                            reservation.getCustomerName(),
                            Math.toIntExact(reservation.getId()),
                            reservation.getReservationDate() != null ? reservation.getReservationDate().toString() : "",
                            reservation.getReservationTime() != null ? reservation.getReservationTime().toString() : "",
                            reservation.getDepositAmount()
                    );
                }

                response.sendRedirect("/user/payment-result.html?status=success&type=reservation&id=" + reservationId);
                return;
            }

            response.sendRedirect("/user/payment-result.html?status=failed&type=reservation&id=" + reservationId);
            return;
        }

        response.sendRedirect("/user/payment-result.html?status=unknown");
    }

    // ================= IPN =================
    @GetMapping("/vnpay-ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> response = new HashMap<>();

        try {
            String secureHash = params.get("vnp_SecureHash");

            if (!vnPayService.verifySignature(params, secureHash)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid signature");
                return response;
            }

            String txnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");

            if (txnRef == null || txnRef.isBlank()) {
                response.put("RspCode", "01");
                response.put("Message", "Transaction not found");
                return response;
            }

            boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

            // ORDER
            if (txnRef.startsWith("ORD_")) {
                Integer orderId = Integer.parseInt(txnRef.replace("ORD_", ""));

                Orders order = orderRepository.findById(orderId.longValue())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

                if (success && order.getPaymentStatus() != Orders.PaymentStatus.PAID) {
                    order.setPaymentStatus(Orders.PaymentStatus.PAID);
                    order.setPaidAt(LocalDateTime.now());
                    order.setPaymentTxnRef(txnRef);
                    orderRepository.save(order);
                }
            }

            // RESERVATION
            if (txnRef.startsWith("RES_")) {
                Integer reservationId = Integer.parseInt(txnRef.replace("RES_", ""));

                Reservation reservation = reservationRepository.findById(reservationId.longValue())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy reservation"));

                if (success && reservation.getDepositStatus() != Reservation.DepositStatus.PAID) {
                    reservation.setDepositStatus(Reservation.DepositStatus.PAID);
                    reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);

                    if (reservation.getTable() != null) {
                        RestaurantTable table = reservation.getTable();
                        table.setStatus(RestaurantTable.TableStatus.RESERVED);
                        tableRepository.save(table);
                    }

                    reservationRepository.save(reservation);

                    if (reservation.getCustomerEmail() != null && !reservation.getCustomerEmail().isBlank()) {
                        emailService.sendDepositPaidEmail(
                                reservation.getCustomerEmail(),
                                reservation.getCustomerName(),
                                Math.toIntExact(reservation.getId()),
                                reservation.getReservationDate() != null ? reservation.getReservationDate().toString() : "",
                                reservation.getReservationTime() != null ? reservation.getReservationTime().toString() : "",
                                reservation.getDepositAmount()
                        );
                    }
                }
            }

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;

        } catch (Exception e) {
            logger.error("Error processing VNPay IPN", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return response;
        }
    }
}
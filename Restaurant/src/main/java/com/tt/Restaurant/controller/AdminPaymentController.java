package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.AdminPaymentDTO;
import com.tt.Restaurant.model.Orders;
import com.tt.Restaurant.model.Payment;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.repository.OrderRepository;
import com.tt.Restaurant.repository.PaymentRepository;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api/payments")
public class AdminPaymentController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RestaurantTableRepository tableRepository;

    public AdminPaymentController(OrderRepository orderRepository,
                                  PaymentRepository paymentRepository,
                                  RestaurantTableRepository tableRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.tableRepository = tableRepository;
    }

    @GetMapping
    public List<AdminPaymentDTO> getAllPayments() {
        List<Orders> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        return orders.stream()
                .filter(order -> order.getStatus() != Orders.OrderStatus.CANCELLED)
                .map(this::mapOrderToPaymentDTO)
                .toList();
    }

    @GetMapping("/{orderId}")
    public AdminPaymentDTO getPaymentDetail(@PathVariable Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        return mapOrderToPaymentDTO(order);
    }

    @PutMapping("/{orderId}/confirm")
    public String confirmPayment(@PathVariable Long orderId,
                                 @RequestParam String paymentMethod,
                                 @RequestParam(required = false) String transactionId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == Orders.OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn đã hủy, không thể thanh toán");
        }

        if (order.getPaymentStatus() == Orders.PaymentStatus.PAID) {
            throw new RuntimeException("Đơn này đã được thanh toán");
        }

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ");
        }

        if (method != Payment.PaymentMethod.CASH && (transactionId == null || transactionId.isBlank())) {
            throw new RuntimeException("Phải nhập mã giao dịch khi không phải tiền mặt");
        }

        String txn = (transactionId != null && !transactionId.isBlank())
                ? transactionId.trim()
                : "CASH_" + order.getId() + "_" + System.currentTimeMillis();

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(method);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId(txn);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPaymentStatus(Orders.PaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentTxnRef(txn);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        releaseTableIfNeeded(order);

        return "Xác nhận thanh toán thành công";
    }

    @PutMapping("/{orderId}/fail")
    public String markPaymentFailed(@PathVariable Long orderId,
                                    @RequestParam String paymentMethod,
                                    @RequestParam(required = false) String transactionId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ");
        }

        String txn = (transactionId != null && !transactionId.isBlank())
                ? transactionId.trim()
                : "FAILED_" + order.getId() + "_" + System.currentTimeMillis();

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(method);
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setTransactionId(txn);
        payment.setPaidAt(null);
        paymentRepository.save(payment);

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return "Đã ghi nhận giao dịch thất bại";
    }

    private AdminPaymentDTO mapOrderToPaymentDTO(Orders order) {
        Optional<Payment> latestPaymentOpt = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getId());

        AdminPaymentDTO dto = new AdminPaymentDTO();
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setAmount(order.getTotalAmount());
        dto.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);

        if (order.getTable() != null) {
            dto.setTableNumber(order.getTable().getTableNumber());
        } else if (order.getReservation() != null && order.getReservation().getTable() != null) {
            dto.setTableNumber(order.getReservation().getTable().getTableNumber());
        }

        if (order.getReservation() != null) {
            dto.setCustomerName(order.getReservation().getCustomerName());
        } else {
            dto.setCustomerName("Khách tại quán");
        }

        if (latestPaymentOpt.isPresent()) {
            Payment payment = latestPaymentOpt.get();
            dto.setPaymentId(payment.getId());
            dto.setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
            dto.setPaymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
            dto.setTransactionId(payment.getTransactionId());
            dto.setPaidAt(payment.getPaidAt() != null ? payment.getPaidAt().toString() : null);
            dto.setCreatedAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null);
        } else {
            dto.setPaymentId(null);
            dto.setPaymentMethod(order.getPaymentOption() != null ? order.getPaymentOption().name() : "PAY_AT_RESTAURANT");
            dto.setPaymentStatus(order.getPaymentStatus() == Orders.PaymentStatus.PAID ? "COMPLETED" : "PENDING");
            dto.setTransactionId(order.getPaymentTxnRef());
            dto.setPaidAt(order.getPaidAt() != null ? order.getPaidAt().toString() : null);
            dto.setCreatedAt(order.getOrderDate() != null ? order.getOrderDate().toString() : null);
        }

        dto.setCanConfirm(order.getPaymentStatus() != Orders.PaymentStatus.PAID);

        return dto;
    }

    private void releaseTableIfNeeded(Orders order) {
        RestaurantTable table = null;

        if (order.getTable() != null) {
            table = order.getTable();
        } else if (order.getReservation() != null && order.getReservation().getTable() != null) {
            table = order.getReservation().getTable();
        }

        if (table != null) {
            table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }
    }
}
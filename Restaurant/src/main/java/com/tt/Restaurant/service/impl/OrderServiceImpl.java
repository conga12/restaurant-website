package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.*;
import com.tt.Restaurant.model.Dish;
import com.tt.Restaurant.model.OrderDetail;
import com.tt.Restaurant.model.Orders;
import com.tt.Restaurant.model.Reservation;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.repository.DishRepository;
import com.tt.Restaurant.repository.OrderDetailRepository;
import com.tt.Restaurant.repository.OrderRepository;
import com.tt.Restaurant.repository.ReservationRepository;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import com.tt.Restaurant.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final DishRepository dishRepository;
    private final RestaurantTableRepository restaurantTableRepository;

    public OrderServiceImpl(ReservationRepository reservationRepository,
                            OrderRepository orderRepository,
                            OrderDetailRepository orderDetailRepository,
                            DishRepository dishRepository,
                            RestaurantTableRepository restaurantTableRepository) {
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.dishRepository = dishRepository;
        this.restaurantTableRepository = restaurantTableRepository;
    }

    @Override
    public VerifyReservationResponseDTO verifyReservation(VerifyReservationRequestDTO request) {
        if (request.getReservationId() == null) {
            throw new RuntimeException("Mã đặt bàn không được để trống");
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        Reservation reservation = reservationRepository.findById((long) request.getReservationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getCustomerPhone() == null ||
                !reservation.getCustomerPhone().trim().equals(request.getCustomerPhone().trim())) {
            throw new RuntimeException("Số điện thoại không khớp với đặt bàn");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ đặt bàn đã được xác nhận mới được order món trước");
        }

        VerifyReservationResponseDTO response = new VerifyReservationResponseDTO();
        response.setValid(true);
        response.setReservationId(reservation.getId());
        response.setCustomerName(reservation.getCustomerName());
        response.setCustomerPhone(reservation.getCustomerPhone());
        response.setCustomerEmail(reservation.getCustomerEmail());
        response.setReservationDate(reservation.getReservationDate() != null ? reservation.getReservationDate().toString() : null);
        response.setReservationTime(reservation.getReservationTime() != null ? reservation.getReservationTime().toString() : null);
        response.setNumberOfGuests(reservation.getNumberOfGuests());
        response.setStatus(reservation.getStatus().name());
        response.setMessage("Xác minh đặt bàn thành công");

        if (reservation.getTable() != null) {
            response.setTableId(reservation.getTable().getId());
            response.setTableNumber(reservation.getTable().getTableNumber());
        }

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        if (request.getReservationId() == null) {
            throw new RuntimeException("Mã đặt bàn không được để trống");
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất 1 món");
        }

        Reservation reservation = reservationRepository.findById((long) request.getReservationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getCustomerPhone() == null ||
                !reservation.getCustomerPhone().trim().equals(request.getCustomerPhone().trim())) {
            throw new RuntimeException("Số điện thoại không khớp với đặt bàn");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ đặt bàn đã xác nhận mới được tạo order");
        }

        if (orderRepository.existsByReservationId(reservation.getId())) {
            throw new RuntimeException("Đặt bàn này đã có order rồi");
        }

        Orders order = new Orders();
        order.setReservation(reservation);
        order.setUser(reservation.getUser());
        order.setTable(reservation.getTable());
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus(Orders.OrderStatus.PENDING);
        order.setNote(request.getNote());
        order.setAdminSeen(false);

        if ("PAY_NOW".equalsIgnoreCase(request.getPaymentOption())) {
            order.setPaymentOption(Orders.PaymentOption.PAY_NOW);
        } else {
            order.setPaymentOption(Orders.PaymentOption.PAY_AT_RESTAURANT);
        }

        order.setPaymentStatus(Orders.PaymentStatus.UNPAID);

        Orders savedOrder = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;
        List<OrderItemResponseDTO> itemResponses = new ArrayList<>();

        for (CreateOrderItemDTO itemDTO : request.getItems()) {
            if (itemDTO.getDishId() == null) {
                throw new RuntimeException("dishId không được để trống");
            }

            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng món không hợp lệ");
            }

            Dish dish = dishRepository.findById(itemDTO.getDishId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn, id = " + itemDTO.getDishId()));

            if (dish.getAvailable() != null && !dish.getAvailable()) {
                throw new RuntimeException("Món '" + dish.getName() + "' hiện không còn bán");
            }

            BigDecimal unitPrice = dish.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity()));

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(savedOrder);
            orderDetail.setDish(dish);
            orderDetail.setQuantity(itemDTO.getQuantity());
            orderDetail.setUnitPrice(unitPrice);
            orderDetail.setSubtotal(subtotal);
            orderDetail.setNote(itemDTO.getNote());

            orderDetailRepository.save(orderDetail);

            totalAmount = totalAmount.add(subtotal);
            totalItems += itemDTO.getQuantity();

            OrderItemResponseDTO itemResponse = new OrderItemResponseDTO();
            itemResponse.setDishId(dish.getId());
            itemResponse.setDishName(dish.getName());
            itemResponse.setQuantity(itemDTO.getQuantity());
            itemResponse.setUnitPrice(unitPrice);
            itemResponse.setSubtotal(subtotal);
            itemResponse.setNote(itemDTO.getNote());
            itemResponses.add(itemResponse);
        }

        savedOrder.setTotalAmount(totalAmount);
        savedOrder.setUpdatedAt(LocalDateTime.now());
        savedOrder = orderRepository.save(savedOrder);

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(savedOrder.getId());
        response.setReservationId(reservation.getId());
        response.setCustomerName(reservation.getCustomerName());
        response.setCustomerPhone(reservation.getCustomerPhone());
        response.setOrderDate(savedOrder.getOrderDate() != null ? savedOrder.getOrderDate().toString() : null);
        response.setStatus(savedOrder.getStatus().name());
        response.setNote(savedOrder.getNote());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setTotalItems(totalItems);
        response.setItems(itemResponses);

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDTO createQrOrder(CreateQrOrderRequestDTO request) {
        if (request.getTableId() == null) {
            throw new RuntimeException("Mã bàn không được để trống");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất 1 món");
        }

        RestaurantTable table = restaurantTableRepository.findById(request.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));
        table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
        restaurantTableRepository.save(table);

        Orders order = new Orders();
        order.setReservation(null);
        order.setUser(null);
        order.setTable(table);
        order.setOrderCode(generateOrderCode());
        order.setOrderDate(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus(Orders.OrderStatus.PENDING);
        order.setNote(request.getNote());
        order.setAdminSeen(false);
        order.setPaymentOption(Orders.PaymentOption.PAY_AT_RESTAURANT);
        order.setPaymentStatus(Orders.PaymentStatus.UNPAID);

        Orders savedOrder = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;
        List<OrderItemResponseDTO> itemResponses = new ArrayList<>();

        for (CreateOrderItemDTO itemDTO : request.getItems()) {
            if (itemDTO.getDishId() == null) {
                throw new RuntimeException("dishId không được để trống");
            }

            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng món không hợp lệ");
            }

            Dish dish = dishRepository.findById(itemDTO.getDishId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn, id = " + itemDTO.getDishId()));

            if (dish.getAvailable() != null && !dish.getAvailable()) {
                throw new RuntimeException("Món '" + dish.getName() + "' hiện không còn bán");
            }

            BigDecimal unitPrice = dish.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity()));

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(savedOrder);
            orderDetail.setDish(dish);
            orderDetail.setQuantity(itemDTO.getQuantity());
            orderDetail.setUnitPrice(unitPrice);
            orderDetail.setSubtotal(subtotal);
            orderDetail.setNote(itemDTO.getNote());

            orderDetailRepository.save(orderDetail);

            totalAmount = totalAmount.add(subtotal);
            totalItems += itemDTO.getQuantity();

            OrderItemResponseDTO itemResponse = new OrderItemResponseDTO();
            itemResponse.setDishId(dish.getId());
            itemResponse.setDishName(dish.getName());
            itemResponse.setQuantity(itemDTO.getQuantity());
            itemResponse.setUnitPrice(unitPrice);
            itemResponse.setSubtotal(subtotal);
            itemResponse.setNote(itemDTO.getNote());
            itemResponses.add(itemResponse);
        }

        savedOrder.setTotalAmount(totalAmount);
        savedOrder.setUpdatedAt(LocalDateTime.now());
        savedOrder = orderRepository.save(savedOrder);

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(savedOrder.getId());
        response.setReservationId(null);
        response.setCustomerName(null);
        response.setCustomerPhone(null);
        response.setOrderDate(savedOrder.getOrderDate() != null ? savedOrder.getOrderDate().toString() : null);
        response.setStatus(savedOrder.getStatus().name());
        response.setNote(savedOrder.getNote());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setTotalItems(totalItems);
        response.setItems(itemResponses);

        return response;
    }

    @Override
    public OrderResponseDTO getOrderByReservation(Integer reservationId) {
        Orders order = orderRepository.findByReservationId((long) reservationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order cho đặt bàn này"));

        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
        List<OrderItemResponseDTO> itemResponses = new ArrayList<>();
        int totalItems = 0;

        for (OrderDetail detail : orderDetails) {
            OrderItemResponseDTO itemResponse = new OrderItemResponseDTO();
            itemResponse.setDishId(detail.getDish().getId());
            itemResponse.setDishName(detail.getDish().getName());
            itemResponse.setQuantity(detail.getQuantity());
            itemResponse.setUnitPrice(detail.getUnitPrice());
            itemResponse.setSubtotal(detail.getSubtotal());
            itemResponse.setNote(detail.getNote());
            itemResponses.add(itemResponse);

            totalItems += detail.getQuantity();
        }

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getId());

        if (order.getReservation() != null) {
            response.setReservationId(order.getReservation().getId());
            response.setCustomerName(order.getReservation().getCustomerName());
            response.setCustomerPhone(order.getReservation().getCustomerPhone());
        }

        response.setOrderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null);
        response.setStatus(order.getStatus().name());
        response.setNote(order.getNote());
        response.setTotalAmount(order.getTotalAmount());
        response.setTotalItems(totalItems);
        response.setItems(itemResponses);

        return response;
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        List<OrderDetail> details = orderDetailRepository.findByOrderId(orderId);

        List<OrderItemResponseDTO> items = new ArrayList<>();
        int totalItems = 0;

        for (OrderDetail d : details) {
            OrderItemResponseDTO item = new OrderItemResponseDTO();
            item.setDishId(d.getDish().getId());
            item.setDishName(d.getDish().getName());
            item.setQuantity(d.getQuantity());
            item.setUnitPrice(d.getUnitPrice());
            item.setSubtotal(d.getSubtotal());
            item.setNote(d.getNote());

            items.add(item);
            totalItems += d.getQuantity();
        }

        OrderResponseDTO res = new OrderResponseDTO();
        res.setOrderId(order.getId());

        if (order.getReservation() != null) {
            res.setReservationId(order.getReservation().getId());
            res.setCustomerName(order.getReservation().getCustomerName());
            res.setCustomerPhone(order.getReservation().getCustomerPhone());
        } else {
            res.setReservationId(null);
            res.setCustomerName(null);
            res.setCustomerPhone(null);
        }

        res.setOrderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null);
        res.setStatus(order.getStatus().name());
        res.setNote(order.getNote());
        res.setTotalAmount(order.getTotalAmount());
        res.setTotalItems(totalItems);
        res.setItems(items);

        return res;
    }

    private String generateOrderCode() {
        return "ORD" + System.currentTimeMillis();
    }
}
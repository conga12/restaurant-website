package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.*;
import com.tt.Restaurant.model.*;
import com.tt.Restaurant.repository.*;
import com.tt.Restaurant.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ReviewRepository reviewRepository;
    @Autowired
    private PromotionRepository promotionRepository;

    public OrderServiceImpl(ReservationRepository reservationRepository,
                            OrderRepository orderRepository,
                            OrderDetailRepository orderDetailRepository,
                            DishRepository dishRepository,
                            RestaurantTableRepository restaurantTableRepository,
                            ReviewRepository reviewRepository) {
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.dishRepository = dishRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public VerifyReservationResponseDTO verifyReservation(VerifyReservationRequestDTO request) {
        if (request.getReservationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã đặt bàn không được để trống");
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
        }

        Reservation reservation = reservationRepository.findById((long) request.getReservationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (reservation.getCustomerPhone() == null ||
                !reservation.getCustomerPhone().trim().equals(request.getCustomerPhone().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số điện thoại không khớp với đặt bàn");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ đặt bàn đã được xác nhận mới được order món trước");
        }

        // ==== BỔ SUNG KIỂM TRA CỌC ====
        boolean depositRequired = Boolean.TRUE.equals(reservation.getDepositRequired());
        String depositStatus = reservation.getDepositStatus() != null ? reservation.getDepositStatus().name() : null;

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

        if (reservation.getTable() != null) {
            response.setTableId(reservation.getTable().getId());
            response.setTableNumber(reservation.getTable().getTableNumber());
        }
        // Trả về trạng thái cọc cho FE
        response.setDepositRequired(depositRequired);
        response.setDepositStatus(depositStatus);

        if (depositRequired && !"PAID".equals(depositStatus)) {
            response.setValid(false);
            response.setMessage("Bạn cần thanh toán tiền cọc trước khi đặt món!");
            // FE có thể dùng response.valid để ẩn nút đặt món/hiện popup báo khách phải cọc mới tiếp tục.
        } else {
            response.setValid(true);
            response.setMessage("Xác minh đặt bàn thành công.");
        }

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        if (request.getReservationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã đặt bàn không được để trống");
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng phải có ít nhất 1 món");
        }
        Reservation reservation = reservationRepository.findById((long) request.getReservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy đặt bàn"));

        if (reservation.getCustomerPhone() == null ||
                !reservation.getCustomerPhone().trim().equals(request.getCustomerPhone().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số điện thoại không khớp với đặt bàn");
        }

        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ đặt bàn đã xác nhận mới được tạo order");
        }

        if (orderRepository.existsByReservation_Id(reservation.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Đặt bàn này đã có order rồi");
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"dishId không được để trống");
            }

            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số lượng món không hợp lệ");
            }

            Dish dish = dishRepository.findById(itemDTO.getDishId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không tìm thấy món ăn, id = " + itemDTO.getDishId()));

            if (dish.getAvailable() != null && !dish.getAvailable()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Món '" + dish.getName() + "' hiện không còn bán");
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
        // Lưu số tiền gốc
        savedOrder.setOriginAmount(totalAmount);

// Áp dụng khuyến mãi nếu có
        LocalDateTime now = LocalDateTime.now();
        Promotion promo = promotionRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                .filter(p -> !now.toLocalDate().isBefore(p.getStartDate()) && !now.toLocalDate().isAfter(p.getEndDate()))
                .filter(p -> p.getDiscountPercent() != null && p.getDiscountPercent() > 0)
                .findFirst().orElse(null);

        String couponCode = request.getCouponCode();

        if (couponCode != null && !couponCode.isBlank()) {
            Review couponReview = reviewRepository.findByCouponCodeIgnoreCase(couponCode.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không tồn tại"));

            if (couponReview.isCouponUsed()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã được sử dụng");
            }

            if (couponReview.getCouponExpiresAt() == null || couponReview.getCouponExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn");
            }

//            if (couponReview.getUser() == null || reservation.getUser() == null ||
//                    !couponReview.getUser().getId().equals(reservation.getUser().getId())) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không thuộc tài khoản này");
//            }

            Integer percent = couponReview.getCouponDiscountPercent() == null ? 0 : couponReview.getCouponDiscountPercent();

            BigDecimal discountAmt = totalAmount.multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            BigDecimal finalAmt = totalAmount.subtract(discountAmt);

            savedOrder.setDiscountPercent(percent);
            savedOrder.setPromotion(null);
            savedOrder.setFinalAmount(finalAmt);

            couponReview.setCouponUsed(true);
            reviewRepository.save(couponReview);

        } else if (promo != null) {
            Integer percent = promo.getDiscountPercent();

            BigDecimal discountAmt = totalAmount.multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            BigDecimal finalAmt = totalAmount.subtract(discountAmt);

            savedOrder.setDiscountPercent(percent);
            savedOrder.setPromotion(promo);
            savedOrder.setFinalAmount(finalAmt);
        } else {
            savedOrder.setDiscountPercent(0);
            savedOrder.setPromotion(null);
            savedOrder.setFinalAmount(totalAmount);
        }

        savedOrder.setUpdatedAt(LocalDateTime.now());
        savedOrder = orderRepository.save(savedOrder);

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
        response.setOriginAmount(savedOrder.getOriginAmount());
        response.setFinalAmount(savedOrder.getFinalAmount());
        response.setDiscountPercent(savedOrder.getDiscountPercent());
        if (savedOrder.getPromotion() != null) {
            response.setPromotionTitle(savedOrder.getPromotion().getTitle());
        } else {
            response.setPromotionTitle(null);
        }

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDTO createQrOrder(CreateQrOrderRequestDTO request) {
        if (request.getTableId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mã bàn không được để trống");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Đơn hàng phải có ít nhất 1 món");
        }

        RestaurantTable table = restaurantTableRepository.findById(request.getTableId())
                .orElseThrow(() ->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không tìm thấy bàn"));
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"dishId không được để trống");
            }

            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Số lượng món không hợp lệ");
            }

            Dish dish = dishRepository.findById(itemDTO.getDishId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn, id = " + itemDTO.getDishId()));

            if (dish.getAvailable() != null && !dish.getAvailable()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Món '" + dish.getName() + "' hiện không còn bán");
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
        // Lưu số tiền gốc
        savedOrder.setOriginAmount(totalAmount);
        // Áp dụng khuyến mãi nếu có
        LocalDateTime now = LocalDateTime.now();
        Promotion promo = promotionRepository.findByIsActiveTrue().stream()
                .filter(p -> p.getStartDate() != null && p.getEndDate() != null)
                .filter(p -> !now.toLocalDate().isBefore(p.getStartDate()) && !now.toLocalDate().isAfter(p.getEndDate()))
                .filter(p -> p.getDiscountPercent() != null && p.getDiscountPercent() > 0)
                .findFirst().orElse(null);

        if (promo != null) {
            Integer percent = promo.getDiscountPercent();
            BigDecimal discountAmt = totalAmount.multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            BigDecimal finalAmt = totalAmount.subtract(discountAmt);

            savedOrder.setDiscountPercent(percent);
            savedOrder.setPromotion(promo);
            savedOrder.setFinalAmount(finalAmt);
        } else {
            savedOrder.setDiscountPercent(0);
            savedOrder.setPromotion(null);
            savedOrder.setFinalAmount(totalAmount);
        }

        savedOrder.setUpdatedAt(LocalDateTime.now());
        savedOrder = orderRepository.save(savedOrder);

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
        Orders order = orderRepository.findByReservation_Id((long) reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không tìm thấy order cho đặt bàn này"));

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
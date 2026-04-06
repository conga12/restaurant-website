package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.AdminOrderDTO;
import com.tt.Restaurant.dto.OrderItemResponseDTO;
import com.tt.Restaurant.dto.OrderResponseDTO;
import com.tt.Restaurant.model.OrderDetail;
import com.tt.Restaurant.model.Orders;
import com.tt.Restaurant.model.RestaurantTable;
import com.tt.Restaurant.repository.OrderDetailRepository;
import com.tt.Restaurant.repository.OrderRepository;
import com.tt.Restaurant.repository.RestaurantTableRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/api/orders")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final RestaurantTableRepository restaurantTableRepository;

    public AdminOrderController(OrderRepository orderRepository,
                                OrderDetailRepository orderDetailRepository,
                                RestaurantTableRepository restaurantTableRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.restaurantTableRepository = restaurantTableRepository;
    }

    @GetMapping
    public List<AdminOrderDTO> getAllOrders() {
        List<Orders> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        return orders.stream().map(order -> {
            AdminOrderDTO dto = new AdminOrderDTO();

            dto.setId(order.getId());
            dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
            dto.setTotalAmount(order.getTotalAmount());
            dto.setOrderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null);

            // Luồng cũ: order từ reservation
            if (order.getReservation() != null) {
                dto.setReservationId(order.getReservation().getId());
                dto.setCustomerName(order.getReservation().getCustomerName());

                if (order.getReservation().getTable() != null) {
                    dto.setTableNumber(order.getReservation().getTable().getTableNumber());
                }
            }

            // Luồng mới: order từ QR
            if (order.getTable() != null) {
                dto.setTableNumber(order.getTable().getTableNumber());
            }

            dto.setAdminSeen(order.getAdminSeen() != null ? order.getAdminSeen() : false);

            List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
            int totalItems = orderDetails.stream()
                    .mapToInt(detail -> detail.getQuantity() != null ? detail.getQuantity() : 0)
                    .sum();

            dto.setTotalItems(totalItems);

            return dto;
        }).toList();
    }

    @GetMapping("/unread-count")
    public long getUnreadCount() {
        return orderRepository.countUnreadOrders();
    }

    @PutMapping("/{id}/mark-seen")
    public String markOrderSeen(@PathVariable Long id) {
        Orders order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setAdminSeen(true);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return "Đã đánh dấu đơn hàng là đã xem";
    }

    @GetMapping("/{id}")
    public OrderResponseDTO getOrderDetail(@PathVariable Long id) {
        Orders order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getAdminSeen() == null || !order.getAdminSeen()) {
            order.setAdminSeen(true);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
        List<OrderItemResponseDTO> itemResponses = new ArrayList<>();
        int totalItems = 0;

        for (OrderDetail detail : orderDetails) {
            OrderItemResponseDTO itemResponse = new OrderItemResponseDTO();
            itemResponse.setDishId(detail.getDish() != null ? detail.getDish().getId() : null);
            itemResponse.setDishName(detail.getDish() != null ? detail.getDish().getName() : null);
            itemResponse.setQuantity(detail.getQuantity());
            itemResponse.setUnitPrice(detail.getUnitPrice());
            itemResponse.setSubtotal(detail.getSubtotal());
            itemResponse.setNote(detail.getNote());
            itemResponses.add(itemResponse);

            totalItems += detail.getQuantity() != null ? detail.getQuantity() : 0;
        }

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getId());
        response.setOrderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : null);
        response.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        response.setNote(order.getNote());
        response.setTotalAmount(order.getTotalAmount());
        response.setTotalItems(totalItems);
        response.setItems(itemResponses);

        if (order.getReservation() != null) {
            response.setReservationId(order.getReservation().getId());
            response.setCustomerName(order.getReservation().getCustomerName());
            response.setCustomerPhone(order.getReservation().getCustomerPhone());
        } else {
            response.setReservationId(null);
            response.setCustomerName(null);
            response.setCustomerPhone(null);
        }

        return response;
    }

    @PutMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Orders order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Orders.OrderStatus currentStatus = order.getStatus();

        try {
            Orders.OrderStatus newStatus = Orders.OrderStatus.valueOf(status);

            boolean isValidTransition = false;

            if (currentStatus == Orders.OrderStatus.PENDING) {
                isValidTransition = newStatus == Orders.OrderStatus.PREPARING
                        || newStatus == Orders.OrderStatus.CANCELLED;
            } else if (currentStatus == Orders.OrderStatus.PREPARING) {
                isValidTransition = newStatus == Orders.OrderStatus.SERVED
                        || newStatus == Orders.OrderStatus.CANCELLED;
            }

            if (!isValidTransition) {
                throw new RuntimeException("Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
            }

            order.setStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Cập nhật trạng thái bàn cho order QR
            if (order.getTable() != null) {
                RestaurantTable table = order.getTable();

                if (newStatus == Orders.OrderStatus.PREPARING) {
                    table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
                    restaurantTableRepository.save(table);
                } else if (newStatus == Orders.OrderStatus.SERVED || newStatus == Orders.OrderStatus.CANCELLED) {
                    table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
                    restaurantTableRepository.save(table);
                }
            }

            return "Cập nhật trạng thái thành công";
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái không hợp lệ");
        }
    }
}
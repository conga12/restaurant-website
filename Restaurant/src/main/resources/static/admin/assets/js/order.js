const ORDER_API = "/admin/api/orders";
const ORDER_UNREAD_API = "/admin/api/orders/unread-count";

document.addEventListener("DOMContentLoaded", function () {
    loadOrders();
    loadOrderUnreadBadge();
});

async function loadOrders() {
    try {
        const response = await fetch(ORDER_API, {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Không thể tải danh sách đơn hàng");
        }

        const orders = await response.json();
        renderOrders(orders);
        await loadOrderUnreadBadge();
    } catch (error) {
        console.error("Lỗi loadOrders:", error);
    }
}

async function loadOrderUnreadBadge() {
    const badge = document.getElementById("orderUnreadBadge");
    if (!badge) return;

    try {
        const response = await fetch(ORDER_UNREAD_API, {
            credentials: "include"
        });

        if (!response.ok) {
            badge.classList.add("d-none");
            return;
        }

        const count = await response.json();

        if (count > 0) {
            badge.textContent = count > 99 ? "99+" : count;
            badge.classList.remove("d-none");
        } else {
            badge.textContent = "0";
            badge.classList.add("d-none");
        }
    } catch (error) {
        console.error("Lỗi loadOrderUnreadBadge:", error);
        badge.classList.add("d-none");
    }
}

function renderOrders(orders) {
    const tbody = document.getElementById("orderTableBody");
    if (!tbody) return;

    tbody.innerHTML = "";

    if (!orders || orders.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center py-4">Không có đơn hàng</td>
            </tr>
        `;
        return;
    }

    orders.forEach(order => {
        const orderId = order.id ?? order.orderId;
        const isNew = order.adminSeen === false;

        tbody.innerHTML += `
            <tr class="${isNew ? 'order-row-new' : ''}">
                <td>
                    <strong>#ORD${String(orderId).padStart(3, "0")}</strong>
                    ${isNew ? '<span class="order-new-badge">Mới</span>' : ''}
                </td>
                <td><span class="badge bg-secondary">${order.tableNumber ? "Bàn " + order.tableNumber : "Chưa có bàn"}</span></td>
                <td>${order.customerName ?? ""}</td>
                <td>${formatDateTime(order.orderDate)}</td>
                <td>${order.totalItems ?? 0} món</td>
                <td class="text-primary fw-bold">${formatMoney(order.totalAmount)}</td>
                <td>${renderStatusBadge(order.status)}</td>
                <td>
                    <div class="d-flex flex-wrap gap-1">
                        <button class="btn btn-sm btn-outline-primary" onclick="viewOrder(${orderId})">Xem</button>
                        ${renderActionButtons(order)}
                    </div>
                </td>
            </tr>
        `;
    });
}

function formatMoney(amount) {
    if (amount == null) return "0đ";
    return Number(amount).toLocaleString("vi-VN") + "đ";
}

function formatDateTime(dateTime) {
    if (!dateTime) return "";
    const date = new Date(dateTime);
    return date.toLocaleString("vi-VN");
}

function renderStatusBadge(status) {
    if (status === "PENDING") return `<span class="badge bg-primary">Chờ xử lý</span>`;
    if (status === "PREPARING") return `<span class="badge bg-warning text-dark">Đang nấu</span>`;
    if (status === "SERVED") return `<span class="badge bg-success">Đã phục vụ</span>`;
    if (status === "CANCELLED") return `<span class="badge bg-danger">Đã hủy</span>`;
    return `<span class="badge bg-secondary">${status ?? ""}</span>`;
}

async function viewOrder(id) {
    try {
        const response = await fetch(`${ORDER_API}/${id}`, {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Không thể tải chi tiết đơn hàng");
        }

        const order = await response.json();
        console.log("Chi tiết order:", order);

        renderOrderDetail(order);

        const modalEl = document.getElementById("viewOrderModal");
        if (!modalEl) {
            throw new Error("Không tìm thấy modal #viewOrderModal trong HTML");
        }

        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();

        await loadOrders();
    } catch (error) {
        console.error("Lỗi viewOrder:", error);
        alert(error.message || "Không thể tải chi tiết đơn hàng");
    }
}

function renderOrderDetail(order) {
    const tableEl = document.getElementById("viewOrderTable");
    const customerEl = document.getElementById("viewOrderCustomer");
    const timeEl = document.getElementById("viewOrderTime");
    const statusEl = document.getElementById("viewOrderStatus");
    const subTotalEl = document.getElementById("viewOrderSubTotal");
    const taxEl = document.getElementById("viewOrderTax");
    const discountEl = document.getElementById("viewOrderDiscount");
    const totalEl = document.getElementById("viewOrderTotal");
    const itemsBodyEl = document.getElementById("viewOrderItemsBody");

    if (!itemsBodyEl) return;

    const subtotal = Number(order.totalAmount || 0);
    const tax = 0;
    const discount = 0;
    const total = subtotal + tax - discount;

    if (tableEl) {
        tableEl.textContent = order.tableNumber ? `Bàn ${order.tableNumber}` : "Chưa có bàn";
    }

    if (customerEl) {
        const customerName = order.customerName || "Không có";
        const customerPhone = order.customerPhone ? ` - ${order.customerPhone}` : "";
        customerEl.textContent = customerName + customerPhone;
    }

    if (timeEl) {
        timeEl.textContent = formatDateTime(order.orderDate);
    }

    if (statusEl) {
        statusEl.innerHTML = renderStatusBadge(order.status);
    }

    if (subTotalEl) {
        subTotalEl.textContent = formatMoney(subtotal);
    }

    if (taxEl) {
        taxEl.textContent = formatMoney(tax);
    }

    if (discountEl) {
        discountEl.textContent = formatMoney(discount);
    }

    if (totalEl) {
        totalEl.textContent = formatMoney(total);
    }

    itemsBodyEl.innerHTML = "";

    if (!Array.isArray(order.items) || order.items.length === 0) {
        itemsBodyEl.innerHTML = `
            <tr>
                <td colspan="4" class="text-center text-muted">Không có món nào</td>
            </tr>
        `;
        return;
    }

    order.items.forEach(item => {
        itemsBodyEl.innerHTML += `
            <tr>
                <td>
                    <div class="fw-semibold">${escapeHtml(item.dishName ?? "")}</div>
                    <small class="text-muted">
                        ${item.note ? "Ghi chú: " + escapeHtml(item.note) : "Không có ghi chú"}
                    </small>
                </td>
                <td class="text-center">${item.quantity ?? 0}</td>
                <td class="text-end">${formatMoney(item.unitPrice)}</td>
                <td class="text-end">${formatMoney(item.subtotal)}</td>
            </tr>
        `;
    });
}

function escapeHtml(text) {
    return String(text)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function updateOrderStatus(id, status) {
    try {
        const response = await fetch(`/admin/api/orders/${id}/status?status=${status}`, {
            method: "PUT",
            credentials: "include"
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Không thể cập nhật trạng thái");
        }

        alert("Cập nhật trạng thái thành công");
        await loadOrders();
    } catch (error) {
        console.error("Lỗi updateOrderStatus:", error);
        alert(error.message || "Cập nhật trạng thái thất bại");
    }
}

function renderActionButtons(order) {
    const orderId = order.id ?? order.orderId;

    if (order.status === "PENDING") {
        return `
            <button class="btn btn-sm btn-warning" onclick="updateOrderStatus(${orderId}, 'PREPARING')">
                Bắt đầu nấu
            </button>
            <button class="btn btn-sm btn-danger" onclick="updateOrderStatus(${orderId}, 'CANCELLED')">
                Hủy
            </button>
        `;
    }

    if (order.status === "PREPARING") {
        return `
            <button class="btn btn-sm btn-success" onclick="updateOrderStatus(${orderId}, 'SERVED')">
                Đã phục vụ
            </button>
            <button class="btn btn-sm btn-danger" onclick="updateOrderStatus(${orderId}, 'CANCELLED')">
                Hủy
            </button>
        `;
    }

    if (order.status === "SERVED") {
        return `
            <span class="text-success small fw-semibold">Đã hoàn tất phục vụ</span>
        `;
    }

    if (order.status === "CANCELLED") {
        return `
            <span class="text-danger small fw-semibold">Đơn đã hủy</span>
        `;
    }

    return "";
}
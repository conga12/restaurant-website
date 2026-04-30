const API_URL = "/admin/api/payments";

let paymentsData = [];

document.addEventListener("DOMContentLoaded", () => {
    loadPayments();
});

async function loadPayments() {
    try {
        const res = await fetch(API_URL);
        paymentsData = await res.json();

        renderTable(paymentsData);
        renderStats(paymentsData);
    } catch (e) {
        console.error("Lỗi load payment:", e);
    }
}

function renderTable(data) {
    const tbody = document.getElementById("paymentTableBody");
    const empty = document.getElementById("paymentEmptyState");

    tbody.innerHTML = "";

    if (!data || data.length === 0) {
        empty.style.display = "block";
        return;
    }

    empty.style.display = "none";

    data.forEach(p => {
        const statusBadge = getStatusBadge(p.paymentStatus);

        // Khuyến mãi
        const promoCol = (p.discountPercent && p.discountPercent > 0)
            ? `<span class="badge bg-success">-${p.discountPercent}%</span>`
            : `<span class="text-muted">--</span>`;

        // Số tiền
        const origin = Number(p.originAmount || p.amount || 0);
        const final = Number(p.finalAmount || p.amount || 0);

        const moneyCol = (origin > final)
            ? `<span style="display:block;text-decoration:line-through; color:#aaa">${formatMoney(origin)}</span>
               <span style="font-weight:bold; color:#2e7d32;">${formatMoney(final)}</span>`
            : `<span style="font-weight:bold; color:#2e7d32;">${formatMoney(final)}</span>`;

        const row = `
            <tr>
                <td><strong>#PAY${p.orderId}</strong></td>
                <td><a href="#" class="text-primary">#ORD${p.orderId}</a></td>
                <td>${p.customerName || "Khách QR"}</td>
                <td>Bàn ${p.tableNumber || "-"}</td>
                <td>${promoCol}</td>
                <td>${moneyCol}</td>
                <td>${formatMethod(p.paymentMethod)}</td>
                <td>${formatDate(p.paidAt || p.createdAt || p.orderDate)}</td>
                <td>${statusBadge}</td>
                <td>
                    ${
                        p.paymentStatus === "PENDING"
                        ? `<button class="btn btn-sm btn-success"
                             onclick="openConfirmModal(${p.orderId})">
                             Xác nhận
                           </button>`
                        : `<span class="text-muted">Đã xử lý</span>`
                    }
                </td>
            </tr>
        `;
        tbody.innerHTML += row;
    });

    document.getElementById("paymentCountText").innerText =
        `Hiển thị ${data.length} giao dịch`;
}

function renderStats(data) {
    let revenue = 0;
    let success = 0;
    let pending = 0;

    data.forEach(p => {
        if (p.paymentStatus === "COMPLETED") {
            revenue += p.amount || 0;
            success++;
        }
        if (p.paymentStatus === "PENDING") {
            pending++;
        }
    });

    document.getElementById("todayRevenue").innerText = formatMoney(revenue);
    document.getElementById("successCount").innerText = success;
    document.getElementById("pendingCount").innerText = pending;
}

function getStatusBadge(status) {
    switch (status) {
        case "COMPLETED":
            return `<span class="badge bg-success">Thành công</span>`;
        case "PENDING":
            return `<span class="badge bg-warning text-dark">Chờ xử lý</span>`;
        case "FAILED":
            return `<span class="badge bg-danger">Thất bại</span>`;
        default:
            return `<span class="badge bg-secondary">${status || ""}</span>`;
    }
}

function formatMoney(num) {
    if (!num) return "0đ";
    return Number(num).toLocaleString("vi-VN") + "đ";
}

function formatDate(dateStr) {
    if (!dateStr) return "";
    return new Date(dateStr).toLocaleString("vi-VN");
}

function formatMethod(method) {
    switch (method) {
        case "CASH": return "💵 Tiền mặt";
        case "CARD": return "💳 Thẻ";
        case "TRANSFER": return "🏦 Chuyển khoản";
        case "PAY_NOW": return "QR Online";
        case "PAY_AT_RESTAURANT": return "Tại quán";
        default: return method || "";
    }
}

function applyFilters() {
    const method = document.getElementById("filterMethod").value;
    const status = document.getElementById("filterStatus").value;
    const orderStatus = document.getElementById("filterOrderStatus").value;

    let filtered = paymentsData;

    if (method) {
        filtered = filtered.filter(p => p.paymentMethod === method);
    }

    if (status) {
        filtered = filtered.filter(p => p.paymentStatus === status);
    }

    if (orderStatus) {
        filtered = filtered.filter(p => p.orderStatus === orderStatus);
    }

    renderTable(filtered);
}

function openConfirmModal(orderId) {
    document.getElementById("confirmOrderId").value = orderId;
    document.getElementById("confirmPaymentMethod").value = "";
    document.getElementById("confirmTransactionId").value = "";

    clearPaymentValidation();
    handlePaymentMethodChange();

    if (!confirmPaymentModalInstance) {
        confirmPaymentModalInstance = new bootstrap.Modal(
            document.getElementById("confirmPaymentModal")
        );
    }

    confirmPaymentModalInstance.show();
}

function handlePaymentMethodChange() {
    const method = document.getElementById("confirmPaymentMethod").value;
    const txnInput = document.getElementById("confirmTransactionId");
    const requiredMark = document.getElementById("transactionRequiredMark");
    const help = document.getElementById("transactionHelp");

    hideElement("paymentMethodError");
    hideElement("transactionError");

    if (method === "CASH") {
        txnInput.value = "";
        txnInput.disabled = true;
        requiredMark.style.display = "none";
        help.textContent = "Thanh toán tiền mặt không cần nhập mã giao dịch.";
    } else if (method === "CARD" || method === "TRANSFER") {
        txnInput.disabled = false;
        requiredMark.style.display = "inline";
        help.textContent = "Bắt buộc nhập mã giao dịch cho phương thức này.";
    } else {
        txnInput.disabled = false;
        requiredMark.style.display = "none";
        help.textContent = "Vui lòng chọn phương thức thanh toán trước.";
    }
}

function clearPaymentValidation() {
    hideElement("paymentMethodError");
    hideElement("transactionError");
}

function hideElement(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = "none";
}

function showElement(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = "block";
}

async function submitConfirmPayment() {
    const orderId = document.getElementById("confirmOrderId").value;
    const method = document.getElementById("confirmPaymentMethod").value;
    const txnInput = document.getElementById("confirmTransactionId");
    const txn = txnInput.value.trim();

    clearPaymentValidation();

    if (!method) {
        showElement("paymentMethodError");
        return;
    }

    if ((method === "CARD" || method === "TRANSFER") && !txn) {
        showElement("transactionError");
        txnInput.focus();
        return;
    }

    try {
        const url = `${API_URL}/${orderId}/confirm?paymentMethod=${encodeURIComponent(method)}&transactionId=${encodeURIComponent(txn)}`;

        const res = await fetch(url, {
            method: "PUT"
        });

        const text = await res.text();

        if (!res.ok) {
            throw new Error(text || "Lỗi xác nhận");
        }

        alert(text || "Thanh toán thành công");

        if (confirmPaymentModalInstance) {
            confirmPaymentModalInstance.hide();
        }

        loadPayments();

    } catch (e) {
        console.error(e);
        alert(e.message || "Lỗi thanh toán");
    }
}
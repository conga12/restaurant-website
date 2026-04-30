let revenueChartInstance = null;
let orderStatusChartInstance = null;

document.addEventListener("DOMContentLoaded", async function () {
    await loadDashboard();
});

window.addEventListener("resize", function () {
    clearTimeout(window.__chartResizeTimer);
    window.__chartResizeTimer = setTimeout(() => {
        if (revenueChartInstance) revenueChartInstance.resize();
        if (orderStatusChartInstance) orderStatusChartInstance.resize();
    }, 200);
});

async function loadDashboard() {
    try {
        const res = await fetch("/admin/api/dashboard", { credentials: "include" });

        if (!res.ok) {
            throw new Error("Không tải được dashboard");
        }

        const data = await res.json();

        document.getElementById("todayRevenue").textContent = formatMoney(data.todayRevenue);
        document.getElementById("totalOrders").textContent = data.totalOrders ?? 0;
        document.getElementById("todayReservations").textContent = data.todayReservations ?? 0;
        document.getElementById("totalCustomers").textContent = data.totalCustomers ?? 0;


        renderRecentOrders(data.recentOrders || []);
        renderTodayReservations(data.todayReservationList || []);
        renderTopDishes(data.topDishes || []);
        renderTables(data.tables || []);
        renderChange("#revenueChange", data.revenueChangePercent);
        renderChange("#orderChange", data.orderChangePercent);
        renderChange("#reservationChange", data.reservationChangePercent);
        renderChange("#customerChange", data.customerChangePercent);

        initRevenueChart(data.revenueChart || []);
        initOrderStatusChart(data.orderStatus || {});
    } catch (e) {
        console.error("Dashboard error:", e);
    }
}

function formatMoney(value) {
    return Number(value || 0).toLocaleString("vi-VN") + "đ";
}

function getOrderStatusBadge(status) {
    switch (status) {
        case "SERVED":
            return `<span class="badge bg-success">Hoàn thành</span>`;
        case "PENDING":
            return `<span class="badge bg-primary">Đã nhận</span>`;
        case "PREPARING":
            return `<span class="badge bg-warning">Đang nấu</span>`;
        case "CANCELLED":
            return `<span class="badge bg-danger">Đã hủy</span>`;
        default:
            return `<span class="badge bg-secondary">${status || "-"}</span>`;
    }
}

function getReservationStatusBadge(status) {
    switch (status) {
        case "CONFIRMED":
            return `<span class="badge bg-success">Đã xác nhận</span>`;
        case "PENDING":
            return `<span class="badge bg-warning">Chờ xác nhận</span>`;
        case "COMPLETED":
            return `<span class="badge bg-info">Hoàn thành</span>`;
        case "CANCELLED":
            return `<span class="badge bg-danger">Đã hủy</span>`;
        default:
            return `<span class="badge bg-secondary">${status || "-"}</span>`;
    }
}

function renderRecentOrders(items) {
    const tbody = document.getElementById("recentOrdersBody");
    if (!tbody) return;

    if (!items.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-muted text-center">Chưa có đơn hàng</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(o => `
        <tr>
            <td><strong>#${o.orderCode || ("ORD" + o.id)}</strong></td>
            <td>${o.tableNumber && o.tableNumber !== "-" ? "Bàn " + o.tableNumber : "-"}</td>
            <td>${formatMoney(o.amount)}</td>
            <td>${getOrderStatusBadge(o.status)}</td>
        </tr>
    `).join("");
}

function initials(name) {
    const parts = String(name || "K").trim().split(/\s+/);
    if (parts.length >= 2) {
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0][0].toUpperCase();
}

function renderTodayReservations(items) {
    const tbody = document.getElementById("todayReservationsBody");
    if (!tbody) return;

    if (!items.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-muted text-center">Hôm nay chưa có đặt bàn</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(r => `
        <tr>
            <td>
                <div class="d-flex align-items-center gap-2">
                    <div class="avatar avatar-sm">${initials(r.customerName)}</div>
                    <span>${r.customerName || "Khách"}</span>
                </div>
            </td>
            <td>${r.time || "-"}</td>
            <td>${r.guests || 0} người</td>
            <td>${getReservationStatusBadge(r.status)}</td>
        </tr>
    `).join("");
}

function renderTopDishes(items) {
    const box = document.getElementById("topDishesBox");
    if (!box) return;

    if (!items.length) {
        box.innerHTML = `<div class="text-muted">Chưa có dữ liệu món bán chạy</div>`;
        return;
    }

    const max = Math.max(...items.map(i => Number(i.quantity || 0)), 1);

    box.innerHTML = items.map((item, index) => {
        const qty = Number(item.quantity || 0);
        const percent = Math.round((qty / max) * 100);

        return `
            <div class="top-item">
                <div class="top-item-info">
                    <span class="rank">${index + 1}</span>
                    <span class="name">${item.name || "-"}</span>
                </div>
                <div class="top-item-stats">
                    <div class="progress">
                        <div class="progress-bar" style="width: ${percent}%"></div>
                    </div>
                    <span>${qty} phần</span>
                </div>
            </div>
        `;
    }).join("");
}

function tableClass(status) {
    switch (status) {
        case "AVAILABLE":
            return "available";
        case "OCCUPIED":
            return "occupied";
        case "RESERVED":
            return "reserved";
        default:
            return "available";
    }
}

function renderTables(items) {
    const grid = document.getElementById("tableGrid");
    if (!grid) return;

    if (!items.length) {
        grid.innerHTML = `<div class="text-muted">Chưa có bàn</div>`;
        return;
    }

    grid.innerHTML = items.map(t => `
        <div class="table-item ${tableClass(t.status)}">
            <span class="table-number">${t.tableNumber}</span>
            <span class="table-capacity">${t.capacity || 0} chỗ</span>
        </div>
    `).join("");
}

function initRevenueChart(values) {
    const ctx = document.getElementById("revenueChart");
    if (!ctx || typeof Chart === "undefined") return;

    if (revenueChartInstance) revenueChartInstance.destroy();

    const context = ctx.getContext("2d");
    const gradient = context.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, "rgba(16, 185, 129, 0.3)");
    gradient.addColorStop(1, "rgba(16, 185, 129, 0)");

    revenueChartInstance = new Chart(ctx, {
        type: "line",
        data: {
            labels: ["T2", "T3", "T4", "T5", "T6", "T7", "CN"],
            datasets: [{
                label: "Doanh thu",
                data: values.map(v => Number(v || 0)),
                borderColor: "#10b981",
                backgroundColor: gradient,
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                pointRadius: 4,
                pointBackgroundColor: "#10b981",
                pointBorderColor: "#fff",
                pointBorderWidth: 2,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: {
                    ticks: {
                        callback: value => (value / 1000000) + "M"
                    }
                }
            }
        }
    });
}

function initOrderStatusChart(orderStatus) {
    const ctx = document.getElementById("orderStatusChart");
    if (!ctx || typeof Chart === "undefined") return;

    if (orderStatusChartInstance) orderStatusChartInstance.destroy();

    const completed = Number(orderStatus.SERVED || 0);
    const processing = Number((orderStatus.PENDING || 0) + (orderStatus.PREPARING || 0));
    const cancelled = Number(orderStatus.CANCELLED || 0);

    orderStatusChartInstance = new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: ["Hoàn thành", "Đang xử lý", "Đã hủy"],
            datasets: [{
                data: [completed, processing, cancelled],
                backgroundColor: ["#10b981", "#f59e0b", "#ef4444"],
                borderColor: "#1e293b",
                borderWidth: 4,
                hoverOffset: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: "70%",
            plugins: { legend: { display: false } }
        }
    });
}
function renderChange(selector, value) {
    const el = document.querySelector(selector);
    if (!el) return;

    const n = Number(value || 0);
    const icon = n >= 0 ? "bi-arrow-up" : "bi-arrow-down";

    el.classList.remove("positive", "negative");
    el.classList.add(n >= 0 ? "positive" : "negative");

    el.innerHTML = `<i class="bi ${icon}"></i> ${Math.abs(n).toFixed(1)}%`;
}

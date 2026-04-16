const API_URL = "/admin/api/reservations";
const AVAILABLE_TABLES_URL = "/admin/api/reservations/available-tables";
const UNREAD_COUNT_URL = "/admin/api/reservations/unread-count";

let allReservations = [];
let autoRefreshTimer = null;
let isLoadingReservations = false;

document.addEventListener("DOMContentLoaded", function () {
    loadReservations();
    loadReservationUnreadBadge();
    setupReservationModalEvents();
    startAutoRefreshReservations();
});

async function loadReservations() {
    if (isLoadingReservations) return;
    isLoadingReservations = true;

    try {
        const response = await fetch(API_URL, {
            credentials: "include"
        });

        if (response.status === 401 || response.status === 403) {
            alert("Bạn chưa đăng nhập hoặc không có quyền.");
            window.location.href = "/auth/login.html";
            return;
        }

        if (!response.ok) {
            throw new Error("Không thể tải danh sách đặt bàn");
        }

        allReservations = await response.json();
        renderReservationTable(allReservations);
        updateSummary(allReservations.length, allReservations.length);
        await loadReservationUnreadBadge();
    } catch (error) {
        console.error("Lỗi loadReservations:", error);
        renderErrorRow("Không thể tải dữ liệu đặt bàn.");
    } finally {
        isLoadingReservations = false;
    }
}

function renderReservationTable(reservations) {
    const tbody = document.getElementById("reservationTableBody");
    if (!tbody) return;

    tbody.innerHTML = "";

    if (!reservations || reservations.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="text-center py-4">Không có dữ liệu</td>
            </tr>
        `;
        return;
    }

    reservations.forEach(r => {
        const avatar = getAvatarText(r.customerName);
        const tableName = r.tableNumber ? `Bàn ${r.tableNumber}` : "Chưa chọn";
        const statusBadge = getStatusBadge(r.status);

        tbody.innerHTML += `
            <tr>
                <td><strong>#RSV${r.id}</strong></td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <div class="avatar avatar-sm">${avatar}</div>
                        <span>${safe(r.customerName)}</span>
                    </div>
                </td>
                <td>${safe(r.customerPhone)}</td>
                <td>${safe(r.customerEmail)}</td>
                <td>
                    <div>${safe(r.reservationDate)}</div>
                    <strong class="text-primary">${formatTime(r.reservationTime)}</strong>
                </td>
                <td>${safe(r.numberOfGuests)} người</td>
                <td><span class="badge bg-secondary">${tableName}</span></td>
                <td>${statusBadge}</td>
                <td>
                    <div class="action-btns d-flex gap-1 flex-wrap">
                        <button class="btn btn-action btn-view" onclick="viewReservation(${r.id})" title="Chi tiết">
                            <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn btn-action btn-edit" onclick="editReservation(${r.id})" title="Sửa">
                            <i class="bi bi-pencil"></i>
                        </button>

                        ${r.status === "PENDING" ? `
                            <button class="btn btn-sm btn-success"
                                onclick="${
                                    !r.tableId
                                        ? `alert('Vui lòng chọn bàn trước khi xác nhận')`
                                        : (r.depositRequired && r.depositStatus !== 'PAID')
                                            ? `alert('Khách chưa thanh toán cọc, chưa thể xác nhận')`
                                            : `confirmReservation(${r.id})`
                                }">
                                Xác nhận
                            </button>
                        ` : ""}

                        ${r.status === "CONFIRMED" ? `
                            <button class="btn btn-sm btn-primary" onclick="completeReservation(${r.id})">
                                Hoàn tất
                            </button>
                        ` : ""}

                        ${r.status !== "CANCELLED" && r.status !== "COMPLETED" ? `
                            <button class="btn btn-action btn-delete" onclick="cancelReservation(${r.id})" title="Hủy">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        ` : ""}
                    </div>
                </td>
            </tr>
        `;
    });
}

function applyFilters() {
    const date = document.getElementById("filterDate")?.value || "";
    const status = document.getElementById("filterStatus")?.value || "";
    const keyword = (document.getElementById("filterKeyword")?.value || "").trim().toLowerCase();

    const filtered = allReservations.filter(r => {
        const matchDate = !date || r.reservationDate === date;
        const matchStatus = !status || r.status === status;
        const matchKeyword =
            !keyword ||
            (r.customerName && r.customerName.toLowerCase().includes(keyword)) ||
            (r.customerPhone && r.customerPhone.toLowerCase().includes(keyword)) ||
            (r.customerEmail && r.customerEmail.toLowerCase().includes(keyword));

        return matchDate && matchStatus && matchKeyword;
    });

    renderReservationTable(filtered);
    updateSummary(filtered.length, allReservations.length);
}

function updateSummary(current, total) {
    const summary = document.getElementById("tableSummary");
    if (summary) {
        summary.innerText = `Hiển thị ${current} / ${total} đặt bàn`;
    }
}

function setupReservationModalEvents() {
    const modal = document.getElementById("reservationModal");
    if (modal) {
        modal.addEventListener("hidden.bs.modal", resetForm);
    }

    const reservationDate = document.getElementById("reservationDate");
    const reservationTime = document.getElementById("reservationTime");
    const numberOfGuests = document.getElementById("numberOfGuests");

    if (reservationDate) {
        reservationDate.addEventListener("change", loadAvailableTables);
    }
    if (reservationTime) {
        reservationTime.addEventListener("change", loadAvailableTables);
    }
    if (numberOfGuests) {
        numberOfGuests.addEventListener("input", loadAvailableTables);
    }
}

function openCreateModal() {
    resetForm();
    const title = document.querySelector("#reservationModal .modal-title");
    if (title) {
        title.innerText = "Thêm đặt bàn mới";
    }
}

function resetForm() {
    const form = document.getElementById("reservationForm");
    if (form) form.reset();

    const reservationId = document.getElementById("reservationId");
    if (reservationId) reservationId.value = "";

    const tableSelect = document.getElementById("tableId");
    if (tableSelect) {
        tableSelect.innerHTML = `<option value="">Tự động chọn bàn phù hợp</option>`;
    }
}

async function loadAvailableTables() {
    const date = document.getElementById("reservationDate")?.value;
    const time = document.getElementById("reservationTime")?.value;
    const guests = document.getElementById("numberOfGuests")?.value;
    const tableSelect = document.getElementById("tableId");

    if (!tableSelect) return;

    // placeholder
    tableSelect.innerHTML = `<option value="">Tự động chọn bàn phù hợp (ưu tiên Standard)</option>`;

    if (!date || !time || !guests) return;

    try {
        const response = await fetch(
            `${AVAILABLE_TABLES_URL}?date=${encodeURIComponent(date)}&time=${encodeURIComponent(time)}&guests=${encodeURIComponent(guests)}`,
            { credentials: "include" }
        );

        if (response.status === 401 || response.status === 403) return;
        if (!response.ok) throw new Error("Không thể tải danh sách bàn trống");

        const tables = await response.json();
        const guestsNum = Number(guests) || 0;

        const norm = (v) => String(v || "").toUpperCase();
        const getType = (tb) => norm(tb.type || tb.tableType || tb.category);

        const isVip = (tb) => getType(tb).includes("VIP");
        const isPrivate = (tb) => getType(tb).includes("PRIVATE");
        const isStandard = (tb) => getType(tb).includes("STANDARD") || (!isVip(tb) && !isPrivate(tb));

        // Standard: chỉ hiện bàn đủ chỗ, ưu tiên bàn nhỏ nhất đủ chỗ
        let standardTables = tables
            .filter(isStandard)
            .filter(t => (Number(t.capacity) || 0) >= guestsNum)
            .sort((a, b) =>
                (a.capacity || 0) - (b.capacity || 0) ||
                (a.tableNumber || 0) - (b.tableNumber || 0)
            );

        // VIP/Private: hiện tất cả, sort cho đẹp
        const vipPrivateTables = tables
            .filter(t => isVip(t) || isPrivate(t))
            .sort((a, b) => {
                const rank = (x) => isVip(x) ? 0 : isPrivate(x) ? 1 : 2;
                const r = rank(a) - rank(b);
                if (r !== 0) return r;
                return (a.capacity || 0) - (b.capacity || 0) ||
                       (a.tableNumber || 0) - (b.tableNumber || 0);
            });

        // Nếu không có Standard đủ chỗ, vẫn show Standard (để admin tự quyết)
        if (standardTables.length === 0) {
            standardTables = tables
                .filter(isStandard)
                .sort((a, b) =>
                    (a.capacity || 0) - (b.capacity || 0) ||
                    (a.tableNumber || 0) - (b.tableNumber || 0)
                );
        }

        const finalTables = [...standardTables, ...vipPrivateTables];

        // reset lại option đầu
        tableSelect.innerHTML = `<option value="">Tự động chọn bàn phù hợp (ưu tiên Standard)</option>`;

        finalTables.forEach(table => {
            const option = document.createElement("option");
            option.value = table.id;

            const t = getType(table);
            let labelType = "";
            let depositHint = "";

            if (t.includes("VIP")) {
                labelType = "VIP";
                depositHint = " (yêu cầu cọc)";
            } else if (t.includes("PRIVATE")) {
                labelType = "Private Room";
                depositHint = " (yêu cầu cọc)";
            } else if (t.includes("STANDARD")) {
                labelType = "Standard";
            } else {
                // fallback nếu type lạ/không có
                labelType = "";
            }

            option.textContent =
                `Bàn ${table.tableNumber} - ${table.capacity} chỗ` +
                (labelType ? ` • ${labelType}` : "") +
                depositHint;

            tableSelect.appendChild(option);
        });
    } catch (error) {
        console.error("Lỗi loadAvailableTables:", error);
    }
}

async function saveReservation() {
    const id = document.getElementById("reservationId")?.value.trim() || "";

    const payload = {
        customerName: document.getElementById("customerName")?.value.trim(),
        customerPhone: document.getElementById("customerPhone")?.value.trim(),
        customerEmail: document.getElementById("customerEmail")?.value.trim(),
        reservationDate: document.getElementById("reservationDate")?.value,
        reservationTime: document.getElementById("reservationTime")?.value,
        numberOfGuests: parseInt(document.getElementById("numberOfGuests")?.value, 10),
        specialRequest: document.getElementById("specialRequest")?.value.trim(),
        status: document.getElementById("reservationStatus")?.value
    };

    const tableId = document.getElementById("tableId")?.value;
    if (tableId) {
        payload.tableId = Number(tableId);
    }

    if (
        !payload.customerName ||
        !payload.customerPhone ||
        !payload.reservationDate ||
        !payload.reservationTime ||
        !payload.numberOfGuests
    ) {
        alert("Vui lòng nhập đầy đủ thông tin bắt buộc");
        return;
    }

    try {
        let response;

        if (id) {
            response = await fetch(`${API_URL}/${id}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                credentials: "include",
                body: JSON.stringify(payload)
            });
        } else {
            response = await fetch(API_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                credentials: "include",
                body: JSON.stringify(payload)
            });
        }

        const text = await response.text();
        console.log("saveReservation status:", response.status);
        console.log("saveReservation raw:", text);

        if (response.status === 401 || response.status === 403) {
            alert("Bạn không có quyền thực hiện thao tác này hoặc phiên đăng nhập đã hết hạn.");
            window.location.href = "/auth/login.html";
            return;
        }

        if (!response.ok) {
            throw new Error(text || "Lưu đặt bàn thất bại");
        }

        const modalElement = document.getElementById("reservationModal");
        const modalInstance = bootstrap.Modal.getInstance(modalElement);
        if (modalInstance) {
            modalInstance.hide();
        }

        await loadReservations();
        alert(id ? "Cập nhật đặt bàn thành công" : "Thêm đặt bàn thành công");
    } catch (error) {
        console.error("Lỗi saveReservation:", error);
        alert(error.message || "Có lỗi xảy ra khi lưu đặt bàn");
    }
}

async function editReservation(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`, {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Không lấy được chi tiết đặt bàn");
        }

        const r = await response.json();

        document.getElementById("reservationId").value = r.id ?? "";
        document.getElementById("customerName").value = r.customerName ?? "";
        document.getElementById("customerPhone").value = r.customerPhone ?? "";
        document.getElementById("customerEmail").value = r.customerEmail ?? "";
        document.getElementById("reservationDate").value = r.reservationDate ?? "";
        document.getElementById("reservationTime").value = formatTimeForInput(r.reservationTime);
        document.getElementById("numberOfGuests").value = r.numberOfGuests ?? "";
        document.getElementById("specialRequest").value = r.specialRequest ?? "";
        document.getElementById("reservationStatus").value = r.status ?? "PENDING";

        await loadAvailableTables();

        document.getElementById("tableId").value = r.tableId ?? "";

        const title = document.querySelector("#reservationModal .modal-title");
        if (title) {
            title.innerText = "Cập nhật đặt bàn";
        }

        const modal = new bootstrap.Modal(document.getElementById("reservationModal"));
        modal.show();
    } catch (error) {
        console.error("Lỗi editReservation:", error);
        alert("Không thể lấy thông tin đặt bàn");
    }
}

async function confirmReservation(id) {
    try {
        const response = await fetch(`${API_URL}/${id}/confirm`, {
            method: "PUT",
            headers: {
                "Accept": "application/json"
            },
            credentials: "include"
        });

        const text = await response.text();
        console.log("confirm status:", response.status);
        console.log("confirm raw:", text);

        if (response.status === 401 || response.status === 403) {
            alert("Bạn không có quyền xác nhận đặt bàn hoặc phiên đăng nhập đã hết hạn.");
            window.location.href = "/auth/login.html";
            return;
        }

        if (!response.ok) {
            throw new Error(text || "Xác nhận thất bại");
        }

        await loadReservations();
        alert("Đã xác nhận đặt bàn");
    } catch (error) {
        console.error("Lỗi confirmReservation:", error);
        alert(error.message || "Không thể xác nhận đặt bàn");
    }
}

async function completeReservation(id) {
    try {
        const response = await fetch(`${API_URL}/${id}/complete`, {
            method: "PUT",
            headers: {
                "Accept": "application/json"
            },
            credentials: "include"
        });

        const text = await response.text();
        console.log("complete status:", response.status);
        console.log("complete raw:", text);

        if (response.status === 401 || response.status === 403) {
            alert("Bạn không có quyền hoàn tất đặt bàn hoặc phiên đăng nhập đã hết hạn.");
            window.location.href = "/auth/login.html";
            return;
        }

        if (!response.ok) {
            throw new Error(text || "Hoàn tất thất bại");
        }

        await loadReservations();
        alert("Đã hoàn tất đặt bàn");
    } catch (error) {
        console.error("Lỗi completeReservation:", error);
        alert(error.message || "Không thể hoàn tất đặt bàn");
    }
}

async function cancelReservation(id) {
    const confirmed = confirm("Bạn có chắc muốn hủy đặt bàn này không?");
    if (!confirmed) return;

    try {
        const response = await fetch(`${API_URL}/${id}/cancel`, {
            method: "PUT",
            headers: {
                "Accept": "application/json"
            },
            credentials: "include"
        });

        const text = await response.text();
        console.log("cancel status:", response.status);
        console.log("cancel raw:", text);

        if (response.status === 401 || response.status === 403) {
            alert("Bạn không có quyền hủy đặt bàn hoặc phiên đăng nhập đã hết hạn.");
            window.location.href = "/auth/login.html";
            return;
        }

        if (!response.ok) {
            throw new Error(text || "Hủy đặt bàn thất bại");
        }

        await loadReservations();
        alert("Đã hủy đặt bàn");
    } catch (error) {
        console.error("Lỗi cancelReservation:", error);
        alert(error.message || "Không thể hủy đặt bàn");
    }
}

async function viewReservation(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`, {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Không lấy được chi tiết đặt bàn");
        }

        const r = await response.json();

        await loadReservationUnreadBadge();
        await loadReservations();

        alert(
            `Khách hàng: ${safe(r.customerName)}\n` +
            `SĐT: ${safe(r.customerPhone)}\n` +
            `Email: ${safe(r.customerEmail)}\n` +
            `Ngày: ${safe(r.reservationDate)}\n` +
            `Giờ: ${formatTime(r.reservationTime)}\n` +
            `Số khách: ${safe(r.numberOfGuests)}\n` +
            `Bàn: ${r.tableNumber ? "Bàn " + r.tableNumber : "Chưa chọn"}\n` +
            `Trạng thái: ${safe(r.status)}\n` +
            `Ghi chú: ${safe(r.specialRequest)}`
        );
    } catch (error) {
        console.error("Lỗi viewReservation:", error);
        alert("Không thể xem chi tiết đặt bàn");
    }
}

function getStatusBadge(status) {
    if (status === "PENDING") return '<span class="badge bg-warning">Chờ xác nhận</span>';
    if (status === "CONFIRMED") return '<span class="badge bg-success">Đã xác nhận</span>';
    if (status === "COMPLETED") return '<span class="badge bg-primary">Hoàn thành</span>';
    if (status === "CANCELLED") return '<span class="badge bg-danger">Đã hủy</span>';
    return `<span class="badge bg-secondary">${safe(status)}</span>`;
}

function getAvatarText(name) {
    if (!name) return "NA";
    return name
        .trim()
        .split(" ")
        .filter(Boolean)
        .map(word => word.charAt(0).toUpperCase())
        .slice(0, 2)
        .join("");
}

function formatTime(time) {
    if (!time) return "";
    return time.length >= 5 ? time.substring(0, 5) : time;
}

function formatTimeForInput(time) {
    if (!time) return "";
    return time.length >= 5 ? time.substring(0, 5) : time;
}

function safe(value) {
    return value ?? "";
}

function renderErrorRow(message) {
    const tbody = document.getElementById("reservationTableBody");
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="9" class="text-center text-danger py-4">${message}</td>
        </tr>
    `;
}

function startAutoRefreshReservations() {
    if (autoRefreshTimer) {
        clearInterval(autoRefreshTimer);
    }

    autoRefreshTimer = setInterval(() => {
        const modalEl = document.getElementById("reservationModal");
        const modalOpen = modalEl && modalEl.classList.contains("show");

        if (document.visibilityState === "visible" && !modalOpen) {
            loadReservations();
        }
    }, 10000);
}

async function loadReservationUnreadBadge() {
    const badge = document.getElementById("reservationUnreadBadge");
    if (!badge) return;

    try {
        const response = await fetch(UNREAD_COUNT_URL, {
            credentials: "include"
        });

        if (response.status === 401 || response.status === 403) {
            badge.classList.add("d-none");
            return;
        }

        if (!response.ok) {
            throw new Error("Không thể tải số lượng đặt bàn mới");
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
        console.error("Lỗi loadReservationUnreadBadge:", error);
        badge.classList.add("d-none");
    }
}
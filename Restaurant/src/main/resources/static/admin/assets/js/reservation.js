const API_URL = "/admin/api/reservations";
const AVAILABLE_TABLES_URL = "/admin/api/reservations/available-tables";
const UNREAD_COUNT_URL = "/admin/api/reservations/unread-count";

let lastPageData = [];
let allReservations = [];
let filteredReservations = [];
let autoRefreshTimer = null;
let isLoadingReservations = false;

function notify(message, type = "info") {
  if (typeof showToast === "function") {
    showToast(message, type);
  } else {
    alert(message);
  }
}

function isPastDateIso(dateIso) {
  if (!dateIso) return false;
  const today = new Date();
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate()); // 00:00 today
  const d = new Date(dateIso + 'T00:00:00');
  return d < todayStart;
}

document.addEventListener("DOMContentLoaded", function () {
    document.getElementById('filter-btn').onclick = function() {
        loadReservations(0); // reset về trang đầu nếu filter
    }
    loadReservations();
    loadReservationUnreadBadge();
    setupReservationModalEvents();
    startAutoRefreshReservations();
});

async function loadReservations(page = 0, size = 20) {
  try {
    const rawDate = document.getElementById("filterDate")?.value || "";
    // convert to yyyy-MM-dd (formatDateToISO should exist in file)
    const dateIso = formatDateToISO(rawDate);

    // if user entered a date but formatDateToISO returned "", it's invalid input
    if (rawDate && !dateIso) {
      notify("Ngày không hợp lệ. Vui lòng chọn ngày theo định dạng yyyy-MM-dd hoặc dd/MM/yyyy.", "warning");
      return;
    }

    // disallow past dates
    if (dateIso && isPastDateIso(dateIso)) {
      notify("Vui lòng chọn ngày hiện tại hoặc tương lai.", "warning");
      return;
    }

    const status = document.getElementById("filterStatus")?.value || "";
    const keyword = (document.getElementById("filterKeyword")?.value || "").trim() || "";

    let url = `/admin/api/reservations?page=${page}&size=${size}`;
    if (dateIso) url += `&date=${encodeURIComponent(dateIso)}`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

    const res = await fetch(url, { credentials: "include" });
    if (res.status === 401 || res.status === 403) {
      notify("Bạn chưa đăng nhập hoặc không có quyền.", "danger");
      window.location.href = "/auth/login.html";
      return;
    }
    if (!res.ok) {
      throw new Error("Không thể tải danh sách đặt bàn");
    }

    const data = await res.json();

    // render as before
    renderReservationTable(data.content);
    renderPagination(data.totalPages, data.number);
    updateTableFooter(data);
    lastPageData = data.content;
  } catch (err) {
    console.error("Lỗi loadReservations:", err);
    renderErrorRow("Không thể tải dữ liệu đặt bàn.");
  }
}

// Ensure the date input cannot pick past dates and wire the filter button
document.addEventListener("DOMContentLoaded", function () {
  // set min for native date input
  const filterDateEl = document.getElementById("filterDate");
  if (filterDateEl) {
    // If it's a native date input, set min to today
    if (filterDateEl.type === "date") {
      const todayIso = new Date().toISOString().slice(0, 10); // yyyy-MM-dd
      filterDateEl.min = todayIso;
    }
    // optional: if user types in dd/MM/yyyy, keep formatDateToISO to validate
  }

  // ensure filter button triggers loadReservations with validation above
  const filterBtn = document.getElementById("filter-btn");
  if (filterBtn) {
    filterBtn.onclick = function () {
      loadReservations(0);
    };
  }
});

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
                <td>${renderDepositCol(r)}</td>
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

    filteredReservations = allReservations.filter(r => {
        const matchDate = !date || r.reservationDate === date;
        const matchStatus = !status || r.status === status;
        const matchKeyword =
            !keyword ||
            (r.customerName && r.customerName.toLowerCase().includes(keyword)) ||
            (r.customerPhone && r.customerPhone.toLowerCase().includes(keyword)) ||
            (r.customerEmail && r.customerEmail.toLowerCase().includes(keyword));

        return matchDate && matchStatus && matchKeyword;
    });

    renderReservationTable(filteredReservations);
    updateSummary(filteredReservations.length, allReservations.length);
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
    tableSelect.innerHTML = '<option value="">Tự động chọn bàn phù hợp</option>';
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
       const excludeId = document.getElementById("reservationId")?.value?.trim();
       const extra = excludeId ? `&excludeReservationId=${encodeURIComponent(excludeId)}` : "";

       const response = await fetch(
         `${AVAILABLE_TABLES_URL}?date=${encodeURIComponent(date)}&time=${encodeURIComponent(time)}&guests=${encodeURIComponent(guests)}${extra}`,
         { credentials: "include" }
        );

        if (response.status === 401 || response.status === 403) return;
        if (!response.ok) throw new Error("Không thể tải danh sách bàn trống");

        const tables = await response.json();
        const guestsNum = Number(guests) || 0;

        // Nếu không có bàn trống
        if (!Array.isArray(tables) || tables.length === 0) {
            tableSelect.innerHTML = `<option value="">Không có bàn trống phù hợp</option>`;
            return;
        }

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
function updateTableFooter(dataPage) {
    // Gán tổng đặt bàn: dataPage.totalElements (từ BE Page trả về)
    document.getElementById("totalReservations").innerText = dataPage.totalElements;
}
async function loadReservations(page = 0, size = 20) {
    const date = document.getElementById("filterDate")?.value || "";
    const status = document.getElementById("filterStatus")?.value || "";
    const keyword = document.getElementById("filterKeyword")?.value.trim() || "";

    let url = `/admin/api/reservations?page=${page}&size=${size}`;
    if (date) url += `&date=${encodeURIComponent(date)}`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;

    const res = await fetch(url);
    const data = await res.json();

    renderReservationTable(data.content);
    renderPagination(data.totalPages, data.number);
    updateTableFooter(data);
    lastPageData = data.content;
}


function renderPagination(totalPages, page) {
    const container = document.getElementById("reservationPagination");
    if (totalPages <= 1) {
        container.innerHTML = "";
        return;
    }

    let html = '';

    // Nút prev
    if (page > 0)
        html += `<button class="btn btn-sm btn-outline-secondary" onclick="gotoPage(${page-1})">«</button> `;

    for (let i=0; i<totalPages; i++) {
        html += `<button class="btn btn-sm ${i===page?'btn-primary':'btn-outline-secondary'}"
                        style="margin:0 2px"
                        onclick="gotoPage(${i})">${i+1}</button>`;
    }
    // Nút next
    if (page < totalPages - 1)
        html += ` <button class="btn btn-sm btn-outline-secondary" onclick="gotoPage(${page+1})">»</button>`;

    container.innerHTML = html;
}

function gotoPage(page) {
    loadReservations(page);
}


function exportReservations() {
    // Xuất theo dữ liệu đang xem (sau lọc)
    const exportData = lastPageData;
    if (!exportData.length) {
        alert("Không có dữ liệu để xuất!");
        return;
    }
    let csv = 'Mã đặt,Khách hàng,SDT,Email,Thời gian,Số khách,Bàn,Trạng thái\n';
    exportData.forEach(r => {
        csv += [
            `#RSV${r.id}`,
            `"${(r.customerName||'').replace(/"/g, '""')}"`,
            `'${r.customerPhone}'`,
            `"${(r.customerEmail||'').replace(/"/g, '""')}"`,
            `"${(r.reservationDate??'')} ${(r.reservationTime??'')}"`,
            r.numberOfGuests,
            r.tableNumber ?? '-',
            r.status
        ].join(',') + '\n';
    });
    const BOM = "\uFEFF";
    const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'danh_sach_dat_ban.csv';
    a.click();
    URL.revokeObjectURL(url);
}

function printReservations() {
    const printContents = document.getElementById("reservationTable").outerHTML;
    const win = window.open('', '', 'width=900,height=700');
    win.document.write('<html><head><title>In danh sách đặt bàn</title></head><body>');
    win.document.write('<h2>Danh sách đặt bàn</h2>');
    win.document.write(printContents);
    win.document.write('</body></html>');
    win.document.close();
    win.print();
}
async function saveReservation() {
    const btn = document.querySelector("#reservationModal .btn.btn-success");
    if (btn) btn.disabled = true;

    try {
        const id = document.getElementById("reservationId")?.value.trim() || "";

        const payload = {
            customerName: document.getElementById("customerName")?.value.trim(),
            customerPhone: document.getElementById("customerPhone")?.value.trim(),
            customerEmail: document.getElementById("customerEmail")?.value.trim(),
            reservationDate: formatDateToISO(document.getElementById("reservationDate")?.value),
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
            !payload.numberOfGuests ||
            !/^\d{4}-\d{2}-\d{2}$/.test(payload.reservationDate) // kiểm tra reservationDate hợp lệ
        ) {
            notify("Vui lòng nhập đầy đủ thông tin bắt buộc và chọn ngày theo định dạng yyyy-MM-dd.", "warning");
            if (btn) btn.disabled = false;
            return;
        }

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
        const rawDate = document.getElementById("reservationDate")?.value;
        console.log('[DEBUG] rawDate:', rawDate);

        if (response.status === 401 || response.status === 403) {
            alert("Bạn không có quyền thực hiện thao tác này hoặc phiên đăng nhập đã hết hạn.");
            window.location.href = "/auth/login.html";
            return;
        }

        if (!response.ok) {
            // Nếu bị trùng bàn/khung giờ => reload danh sách bàn để ẩn bàn vừa bị chiếm
            if (response.status === 409 || (text && text.includes("Bàn đã được đặt"))) {
                await loadAvailableTables();
            }
            throw new Error(text || "Lưu đặt bàn thất bại");
        }

        const modalElement = document.getElementById("reservationModal");
        const modalInstance = bootstrap.Modal.getInstance(modalElement);
        if (modalInstance) {
            modalInstance.hide();
        }

        await loadReservations();
        notify(id ? "Cập nhật đặt bàn thành công" : "Thêm đặt bàn thành công", "success");
    } catch (error) {
        console.error("Lỗi saveReservation:", error);
        notify(error.message || "Có lỗi xảy ra khi lưu đặt bàn", "danger");
    } finally {
        if (btn) btn.disabled = false;
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

        // Nếu bàn hiện tại không còn trong list available => báo và reset
        const tableSelect = document.getElementById("tableId");
        const desired = String(r.tableId ?? "");

        if (tableSelect) {
            if (desired) {
                const exists = Array.from(tableSelect.options).some(o => o.value === desired);
                if (exists) {
                    tableSelect.value = desired;
                } else {
                    tableSelect.value = "";
                    alert(`Bàn hiện tại không còn trống ở khung giờ này. Vui lòng chọn bàn khác.`);
                }
            } else {
                tableSelect.value = "";
            }
        }

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
  try {
    // show modal confirm (returns Promise<boolean>)
    const confirmed = await confirmDialog("Bạn có chắc muốn hủy đặt bàn này không?");
    if (!confirmed) return;

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
      // not authorized
      showToast("Bạn không có quyền hủy đặt bàn hoặc phiên đăng nhập đã hết hạn.", "danger");
      window.location.href = "/auth/login.html";
      return;
    }

    if (!response.ok) {
      // show server error (try parse JSON message if present)
      let msg = text || "Hủy đặt bàn thất bại";
      try {
        const parsed = text ? JSON.parse(text) : null;
        if (parsed && (parsed.message || parsed.error)) msg = parsed.message || parsed.error;
      } catch (e) { /* ignore parse error */ }
      throw new Error(msg);
    }

    // success: reload list and show toast
    await loadReservations();
    showToast("Đã hủy đặt bàn", "success");
  } catch (error) {
    console.error("Lỗi cancelReservation:", error);
    showToast(error.message || "Không thể hủy đặt bàn", "danger");
  }
}

// Thay thế hàm viewReservation cũ bằng hàm này
async function viewReservation(id) {
  try {
    const response = await fetch(`${API_URL}/${id}`, { credentials: "include" });
    if (!response.ok) {
      throw new Error("Không lấy được chi tiết đặt bàn");
    }
    const r = await response.json();

    // Helper format
    function fmtDate(d) {
      if (!d) return "-";
      try {
        const dt = new Date(d);
        return dt.toLocaleString('vi-VN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' });
      } catch (e) { return d; }
    }
    function money(v) { if (v == null) return "0 đ"; return (Number(v).toLocaleString('vi-VN') + " đ"); }
    function statusBadgeHtml(status) {
      if (!status) return '<span class="badge bg-secondary">—</span>';
      switch (String(status).toUpperCase()) {
        case 'PENDING': return '<span class="badge bg-warning text-dark">Chờ xác nhận</span>';
        case 'CONFIRMED': return '<span class="badge bg-success">Đã xác nhận</span>';
        case 'COMPLETED': return '<span class="badge bg-primary">Hoàn tất</span>';
        case 'CANCELLED': return '<span class="badge bg-danger">Đã hủy</span>';
        default: return `<span class="badge bg-secondary">${status}</span>`;
      }
    }

    // Fill modal fields
    document.getElementById('detailReservationCode').textContent = r.id ? `#RSV${r.id}` : '-';
    document.getElementById('detailCustomerName').textContent = r.customerName || '-';
    document.getElementById('detailCustomerPhone').textContent = r.customerPhone || '-';
    document.getElementById('detailCustomerEmail').textContent = r.customerEmail || '-';
    document.getElementById('detailDateTime').textContent = `${r.reservationDate || '-'} ${r.reservationTime || ''}`;
    document.getElementById('detailGuests').textContent = (r.numberOfGuests || '-') + ' người';
    document.getElementById('detailTable').textContent = r.tableNumber ? `Bàn ${r.tableNumber}` : 'Chưa chọn';
    document.getElementById('detailStatus').innerHTML = statusBadgeHtml(r.status);
    document.getElementById('detailNote').textContent = r.specialRequest || '-';
    document.getElementById('detailDepositRequired').textContent = r.depositRequired ? 'Có' : 'Không';
    document.getElementById('detailDepositAmount').textContent = money(r.depositAmount);
    document.getElementById('detailCreatedAt').textContent = r.createdAt ? fmtDate(r.createdAt) : '-';
    document.getElementById('detailExpireAt').textContent = r.expireAt ? fmtDate(r.expireAt) : '-';

    // Open modal
    const modalEl = document.getElementById('reservationDetailModal');
    const bsModal = new bootstrap.Modal(modalEl);
    bsModal.show();

    // Hook edit button to open edit modal (existing editReservation)
    const btnEdit = document.getElementById('btnOpenEditFromDetail');
    btnEdit.onclick = function() {
      bsModal.hide();

      if (typeof editReservation === 'function') {
        editReservation(id);
      } else {
        // fallback: cập nhật tiêu đề modal (nếu có) và mở modal edit thủ công
        const modalTitleEl = document.querySelector('#reservationModal .modal-title');
        if (modalTitleEl) modalTitleEl.innerText = 'Cập nhật đặt bàn';

        const reservationModalEl = document.getElementById('reservationModal');
        if (reservationModalEl) {
          const modal = new bootstrap.Modal(reservationModalEl);
          modal.show();
        }
      }
    };
  } catch (error) {
    console.error("Lỗi viewReservation:", error);
    showToast ? showToast("Không thể lấy chi tiết đặt bàn", "danger") : alert("Không thể lấy chi tiết đặt bàn");
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
function renderDepositCol(r) {
    if (!r.depositRequired) {
        return `<span class="badge bg-secondary">Không yêu cầu</span>`;
    }
    if (r.depositStatus === "PAID") {
        return `<span class="badge bg-success">Đã nhận cọc</span>`;
    }
    if (r.depositStatus === "PENDING") {
        return `<span class="badge bg-warning text-dark">Chờ cọc</span>`;
    }
    if (r.depositStatus === "REFUNDED") {
        return `<span class="badge bg-info text-dark">Đã hoàn cọc</span>`;
    }
    if (r.depositStatus === "FORFEITED" || r.depositStatus === "EXPIRED") {
        return `<span class="badge bg-danger">Mất/Quá hạn cọc</span>`;
    }
    return `<span class="badge bg-dark">${r.depositStatus || "Khác"}</span>`;
}
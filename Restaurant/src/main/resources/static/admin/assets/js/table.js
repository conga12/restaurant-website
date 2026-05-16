const TABLE_API_URL = "/admin/api/tables";

let allTables = [];

document.addEventListener("DOMContentLoaded", function () {
    loadTables();
    setupTableModalEvents();
});

async function loadTables() {
    try {
        const response = await fetch(TABLE_API_URL);
        if (!response.ok) {
            throw new Error("Không thể tải danh sách bàn");
        }

        allTables = await response.json();

        renderStats(allTables);
        renderTableGrid(allTables);
        renderTableList(allTables);
    } catch (error) {
        console.error("Lỗi loadTables:", error);
        renderTableListError("Không thể tải dữ liệu bàn.");
    }
}

function renderStats(tables) {
    const available = tables.filter(t => t.status === "AVAILABLE").length;
    const occupied = tables.filter(t => t.status === "OCCUPIED").length;
    const reserved = tables.filter(t => t.status === "RESERVED").length;

    document.getElementById("availableCount").innerText = available;
    document.getElementById("occupiedCount").innerText = occupied;
    document.getElementById("reservedCount").innerText = reserved;
    document.getElementById("totalCount").innerText = tables.length;
}

function renderTableGrid(tables) {
    const grid = document.getElementById("tableGrid");
    if (!grid) return;

    grid.innerHTML = "";

    if (!tables || tables.length === 0) {
        grid.innerHTML = `<div class="text-muted">Chưa có dữ liệu bàn</div>`;
        return;
    }

    // Render each table card; add class 'hidden' when table.active === false
    tables.forEach(table => {
        const statusClass = getTableStatusClass(table.status);
        const tableTypeClass = getTableTypeClass(table.tableType);
        const isHidden = table.active === false || table.active === 'false';

        grid.innerHTML += `
            <div class="table-item ${statusClass} ${tableTypeClass} ${isHidden ? 'hidden' : ''}" onclick="${isHidden ? 'void(0)' : `openTableAction(${table.id})`}">
                <span class="table-number">${table.tableNumber}</span>
                <span class="table-type-badge ${tableTypeClass}">
                    ${getTableTypeLabel(table.tableType)}
                </span>
                <span class="table-capacity">${table.capacity} chỗ</span>
            </div>
        `;
    });
}

function renderTableList(tables) {
    const tbody = document.getElementById("tableListBody");
    tbody.innerHTML = "";

    if (!tables || tables.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-4">Không có dữ liệu</td>
            </tr>
        `;
        return;
    }

    tables.forEach(table => {
        // status badge: nếu đã ẩn => Tạm ẩn, ngược lại dùng getStatusBadge
        const statusHtml = (table.active === false || table.active === 'false')
            ? '<span class="badge bg-secondary">Tạm ẩn</span>'
            : getStatusBadge(table.status);

        tbody.innerHTML += `
            <tr class="${(table.active === false || table.active === 'false') ? 'row-hidden' : ''}">
                <td><strong>Bàn ${table.tableNumber}</strong></td>
                <td>${getTableTypeBadge(table.tableType)}</td>
                <td>${table.capacity} chỗ</td>
                <td>${safe(table.location)}</td>
                <td>${statusHtml}</td>
                <td>
                    <div class="action-btns">
                        <button class="btn btn-action btn-edit" onclick="editTable(${table.id})" title="Sửa">
                            <i class="bi bi-pencil"></i>
                        </button>

                        ${ (table.active === false || table.active === 'false') ? `
                          <button class="btn btn-action btn-activate" title="Bật lại" onclick="activateTable(${table.id}, '${safe('Bàn ' + table.tableNumber)}')">
                            <i class="bi bi-eye-slash"></i>
                          </button>
                        ` : `
                          <button class="btn btn-action btn-hide" title="Ẩn" onclick="inactiveTable(${table.id}, '${safe('Bàn ' + table.tableNumber)}')">
                            <i class="bi bi-eye"></i>
                          </button>
                        ` }
                    </div>
                </td>
            </tr>
        `;
    });
}

function renderTableListError(message) {
    const tbody = document.getElementById("tableListBody");
    tbody.innerHTML = `
        <tr>
            <td colspan="6" class="text-center text-danger py-4">${message}</td>
        </tr>
    `;
}

function getTableStatusClass(status) {
    if (status === "AVAILABLE") return "available";
    if (status === "OCCUPIED") return "occupied";
    if (status === "RESERVED") return "reserved";
    return "";
}

function getStatusBadge(status) {
    if (status === "AVAILABLE") return '<span class="badge bg-success">Trống</span>';
    if (status === "OCCUPIED") return '<span class="badge bg-warning">Có khách</span>';
    if (status === "RESERVED") return '<span class="badge bg-info">Đã đặt</span>';
    return `<span class="badge bg-secondary">${safe(status)}</span>`;
}

function getTableTypeLabel(tableType) {
    if (tableType === "VIP") return "VIP";
    if (tableType === "PRIVATE_ROOM") return "Private Room";
    return "Standard";
}

function getTableTypeBadge(tableType) {
    if (tableType === "VIP") {
        return '<span class="badge rounded-pill text-bg-warning">VIP</span>';
    }
    if (tableType === "PRIVATE_ROOM") {
        return '<span class="badge rounded-pill" style="background:#6f42c1;color:#fff;">Private Room</span>';
    }
    return '<span class="badge rounded-pill bg-secondary">Standard</span>';
}

function getTableTypeClass(tableType) {
    if (tableType === "VIP") return "vip";
    if (tableType === "PRIVATE_ROOM") return "private-room";
    return "standard";
}

function openCreateTableModal() {
    resetTableForm();
    document.querySelector("#tableModal .modal-title").innerText = "Thêm bàn mới";
}

function setupTableModalEvents() {
    const modal = document.getElementById("tableModal");
    modal.addEventListener("hidden.bs.modal", resetTableForm);
}

async function saveTable() {
    const id = document.getElementById("tableId").value.trim();

    const payload = {
        tableNumber: parseInt(document.getElementById("tableNumber").value, 10),
        capacity: parseInt(document.getElementById("capacity").value, 10),
        tableType: document.getElementById("tableType").value,
        location: document.getElementById("location").value.trim(),
        status: document.getElementById("status").value
    };

    if (!payload.tableNumber || !payload.capacity || !payload.tableType) {
        alert("Vui lòng nhập đầy đủ số bàn, số chỗ và loại bàn");
        return;
    }

    try {
        let response;

        if (id) {
            response = await fetch(`${TABLE_API_URL}/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
        } else {
            response = await fetch(TABLE_API_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
        }

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Lưu bàn thất bại");
        }

        const modalElement = document.getElementById("tableModal");
        const modalInstance = bootstrap.Modal.getInstance(modalElement);
        if (modalInstance) modalInstance.hide();

        await loadTables();
        showToast(id ? "Cập nhật bàn thành công" : "Thêm bàn thành công", "success");
    } catch (error) {
        console.error("Lỗi saveTable:", error);
        showToast(error.message || "Có lỗi xảy ra khi lưu bàn", "danger");
    }
}

async function editTable(id) {
    try {
        const response = await fetch(`${TABLE_API_URL}/${id}`);
        if (!response.ok) {
            throw new Error("Không lấy được thông tin bàn");
        }

        const table = await response.json();

        document.getElementById("tableId").value = table.id ?? "";
        document.getElementById("tableNumber").value = table.tableNumber ?? "";
        document.getElementById("capacity").value = table.capacity ?? "";
        document.getElementById("tableType").value = table.tableType ?? "STANDARD";
        document.getElementById("location").value = table.location ?? "";
        document.getElementById("status").value = table.status ?? "AVAILABLE";

        document.querySelector("#tableModal .modal-title").innerText = "Cập nhật bàn";

        const modal = new bootstrap.Modal(document.getElementById("tableModal"));
        modal.show();
    } catch (error) {
        console.error("Lỗi editTable:", error);
        alert("Không thể lấy thông tin bàn");
    }
}

async function deleteTable(id) {
    const confirmed = confirm("Bạn có chắc muốn ẩn bàn này không?");
    if (!confirmed) return;

    try {
        const response = await fetch(`${TABLE_API_URL}/${id}`, {
            method: "DELETE"
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Ẩn bàn thất bại");
        }

        await loadTables();
        showToast("Ẩn bàn thành công", "success");
    } catch (error) {
          console.error("Lỗi deleteTable:", error);
          showToast(error.message || "Không thể ẩn bàn", "danger");
     }
}

async function openTableAction(id) {
    try {
        const response = await fetch(`${TABLE_API_URL}/${id}`);
        if (!response.ok) {
            throw new Error("Không lấy được thông tin bàn");
        }

        const table = await response.json();
        alert(
            `Bàn ${table.tableNumber}\n` +
            `Loại bàn: ${getTableTypeLabel(table.tableType)}\n` +
            `Sức chứa: ${table.capacity} chỗ\n` +
            `Khu vực: ${safe(table.location)}\n` +
            `Trạng thái: ${table.status}`
        );
    } catch (error) {
        console.error("Lỗi openTableAction:", error);
        alert("Không thể xem thông tin bàn");
    }
}

function resetTableForm() {
    document.getElementById("tableForm").reset();
    document.getElementById("tableId").value = "";

    const tableTypeElement = document.getElementById("tableType");
    if (tableTypeElement) {
        tableTypeElement.value = "STANDARD";
    }

    const statusElement = document.getElementById("status");
    if (statusElement) {
        statusElement.value = "AVAILABLE";
    }
}

function safe(value) {
    return value ?? "";
}
// Ẩn bàn (soft-delete)
async function inactiveTable(id, name) {
    const confirmed = await confirmDialog(`Bạn có chắc chắn muốn ẨN ${name}?`);
    if (!confirmed) return;

    try {
        await apiFetchJson(`/admin/api/tables/${encodeURIComponent(id)}`, { method: 'DELETE' });
        showToast('Đã ẩn bàn.', 'success');
        await loadTables(); // hãy chắc chắn loadTables() là hàm tải lại danh sách bàn
    } catch (e) {
        showToast(`Ẩn thất bại: ${e.message}`, 'danger');
    }
}

// Bật lại bàn (activate)
async function activateTable(id, name) {
    const confirmed = await confirmDialog(`Bạn có chắc chắn muốn BẬT LẠI ${name}?`);
    if (!confirmed) return;

    try {
        await apiFetchJson(`/admin/api/tables/${encodeURIComponent(id)}/activate`, { method: 'PATCH' });
        showToast('Đã bật lại bàn.', 'success');
        await loadTables();
    } catch (e) {
        showToast(`Bật lại thất bại: ${e.message}`, 'danger');
    }
}
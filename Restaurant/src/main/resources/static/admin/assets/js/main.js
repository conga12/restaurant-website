/**
 * RestaurantOS Admin Dashboard
 * Main JavaScript File
 * Clean version for admin pages
 */

const RESERVATION_UNREAD_URL = "/admin/api/reservations/unread-count";
const ORDER_UNREAD_URL = "/admin/api/orders/unread-count";
const WS_ENDPOINT = "/ws-notify";
const RESERVATION_MARK_ALL_SEEN_URL = "/admin/api/reservations/mark-all-seen";

let stompClient = null;

let lastReservationUnreadCount = 0;
let lastOrderUnreadCount = 0;

// ===============================
// Role/Modules-based UI (STAFF)
// ===============================

const ALL_ADMIN_MODULES = [
    "DASHBOARD", "CATEGORY", "DISH", "TABLE",
    "RESERVATION", "ORDER", "PAYMENT", "USER", "REVIEW"
];

// module -> href trong sidebar
const MODULE_TO_HREF = {
  DASHBOARD: "/admin/index.html",
  CATEGORY: "/admin/forms/category.html",
  DISH: "/admin/forms/product.html",
  TABLE: "/admin/forms/table.html",
  RESERVATION: "/admin/forms/reservation.html",
  ORDER: "/admin/forms/order.html",
  PAYMENT: "/admin/forms/payment.html",
  //USER: "/admin/forms/user.html",
  REVIEW: "/admin/forms/review.html"
};
function normalizeModuleList(modules) {
    if (!Array.isArray(modules)) return [];
    return modules
        .map(m => String(m || "").trim().toUpperCase())
        .filter(Boolean);
}

function hideNavItemByHref(href) {
    const link = document.querySelector(`#sidebar a.nav-link[href="${href}"]`);
    const navItem = link?.closest("li.nav-item");
    if (navItem) navItem.style.display = "none";
}

function getCurrentAdminPageName() {
    const path = window.location.pathname;
    return path.split("/").pop() || "index.html";
}

function redirectStaffToFirstAllowed(modulesSet) {
    const priority = ["RESERVATION", "ORDER", "TABLE", "PAYMENT", "DASHBOARD"];
    const first = priority.find(m => modulesSet.has(m));
    if (!first) return;

    const href = MODULE_TO_HREF[first];
    if (!href) return;

    window.location.href = href; // ✅ đi thẳng, khỏi /admin/forms/...
}

async function applyModulesUI() {
    try {
        const res = await fetch("/api/auth/me", { credentials: "include" });
        if (!res.ok) return;

        const me = await res.json();
        const role = me?.role || "ROLE_CUSTOMER";

        // ADMIN: hiển thị hết
        if (role === "ROLE_ADMIN") return;

        // STAFF: ẩn menu theo modules
        if (role === "ROLE_STAFF") {
            const modules = normalizeModuleList(me?.modules);
            const modulesSet = new Set(modules);

            // Ẩn những module không được cấp
            ALL_ADMIN_MODULES.forEach(mod => {
                const href = MODULE_TO_HREF[mod];
                if (!href) return;
                if (!modulesSet.has(mod)) hideNavItemByHref(href);
            });

            // Nếu đang đứng ở trang bị cấm -> redirect sang trang được phép
            const currentPage = getCurrentAdminPageName();
            const allowedPages = new Set(
                [...modulesSet]
                    .map(m => MODULE_TO_HREF[m])
                    .filter(Boolean)
                    .map(h => h.split("/").pop())
            );

            if (allowedPages.size > 0 && !allowedPages.has(currentPage)) {
                redirectStaffToFirstAllowed(modulesSet);
            }
        }
    } catch (e) {
        console.warn("applyModulesUI error:", e);
    }
}

async function applyTopbarProfile() {
  try {
    const res = await fetch("/api/auth/me", { credentials: "include" });
    if (!res.ok) return;

    const me = await res.json();

    const nameEl = document.getElementById("topbarName");
    const avatarEl = document.getElementById("topbarAvatar");

    const displayName = (me?.username || me?.email || "User").trim();

    if (nameEl) nameEl.textContent = displayName;

    if (avatarEl) {
      avatarEl.textContent = displayName.slice(0, 2).toUpperCase();
    }
  } catch (e) {
    console.warn("applyTopbarProfile error:", e);
  }
}

document.addEventListener("DOMContentLoaded", async function () {
    const sidebar = document.getElementById("sidebar");
    const sidebarToggle = document.getElementById("sidebarToggle");

    if (sidebar && sidebarToggle) {
        sidebarToggle.addEventListener("click", function (e) {
            e.preventDefault();

            if (window.innerWidth <= 991) {
                sidebar.classList.toggle("show");
            } else {
                sidebar.classList.toggle("collapsed");
            }
        });
    }

    document.addEventListener("click", function (e) {
        if (window.innerWidth <= 991 && sidebar && sidebarToggle) {
            const clickedInsideSidebar = sidebar.contains(e.target);
            const clickedToggle = sidebarToggle.contains(e.target);

            if (!clickedInsideSidebar && !clickedToggle) {
                sidebar.classList.remove("show");
            }
        }
    });

    initTooltips();
    initPopovers();
    highlightActiveNav();
    await applyTopbarProfile();
    // Ẩn/hiện menu theo modules của STAFF
    await applyModulesUI();

    preventEmptyLinks();
    handleMobileNavClose();
    setMinDateForDateInputs();

    await refreshSidebarBadges(true);
    await markReservationsAllSeenIfOnReservationPage();

    setInterval(async () => {
        if (document.visibilityState === "visible") {
            await refreshSidebarBadges(false);
        }
    }, 5000);
});

/**
 * Initialize Bootstrap tooltips
 */
function initTooltips() {
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function (tooltipTriggerEl) {
        new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

/**
 * Initialize Bootstrap popovers
 */
function initPopovers() {
    const popoverTriggerList = document.querySelectorAll('[data-bs-toggle="popover"]');
    popoverTriggerList.forEach(function (popoverTriggerEl) {
        new bootstrap.Popover(popoverTriggerEl);
    });
}

/**
 * Highlight active navigation item
 */
function highlightActiveNav() {
    const currentPath = window.location.pathname;
    const currentPage = currentPath.split("/").pop() || "index.html";
    const navLinks = document.querySelectorAll(".sidebar-nav .nav-link");

    navLinks.forEach(function (link) {
        const navItem = link.closest(".nav-item");
        if (!navItem) return;

        navItem.classList.remove("active");
        link.classList.remove("active");

        const href = link.getAttribute("href");
        if (!href || href === "#" || href.startsWith("javascript:")) return;

        const linkPage = href.split("/").pop();

        if (linkPage === currentPage) {
            navItem.classList.add("active");
            link.classList.add("active");
        }
    });
}
function connectReservationWS() {
    if (stompClient) return; // tránh connect nhiều lần

    try {
        const socket = new SockJS(WS_ENDPOINT);
        stompClient = Stomp.over(socket);

        // tắt log nếu bạn muốn: stompClient.debug = null;
        stompClient.debug = (str) => console.log("[STOMP]", str);

        stompClient.connect(
            {},
            function (frame) {
                console.log("✅ WS connected:", frame);

                stompClient.subscribe("/topic/reservation-unread", async function (message) {
                    console.log("WS reservation:", message.body);

                    // Realtime: refresh badge + topbar ngay lập tức
                    const reservationCount = await loadReservationUnreadBadge();
                    updateTopbarUnreadBadge(reservationCount, lastOrderUnreadCount);

                    // Nếu muốn có hiệu ứng/sound khi tăng:
                    if (reservationCount > lastReservationUnreadCount) {
                        pulseBadge("reservationUnreadBadge");
                        playNotificationSound();
                    }
                    lastReservationUnreadCount = reservationCount;
                });
            },
            function (error) {
                console.error("❌ WS connect error:", error);
                stompClient = null;
            }
        );
        stompClient.subscribe("/topic/review-alert", function (message) {
          try {
            const data = JSON.parse(message.body);
            if (data.type === "LOW_RATING_REVIEW") {
              showToast(`Có đánh giá thấp (${data.rating}/5). Review #${data.reviewId}`, "warning");
              // nếu muốn: playNotificationSound();
            }
          } catch (e) {
            console.warn("review-alert parse error:", e, message.body);
          }
        });
    } catch (e) {
        console.error("connectReservationWS error:", e);
        stompClient = null;
    }
}

async function markReservationsAllSeenIfOnReservationPage() {
  const page = (window.location.pathname.split("/").pop() || "").toLowerCase();
  if (page !== "reservation.html") return;

  try {
    const res = await fetch("/admin/api/reservations/mark-all-seen", {
      method: "POST",
      credentials: "include"
    });

    console.log("mark-all-seen status:", res.status);

    // refresh badge ngay sau khi mark
    const reservationCount = await loadReservationUnreadBadge();
    updateTopbarUnreadBadge(reservationCount, lastOrderUnreadCount);
    lastReservationUnreadCount = reservationCount;
  } catch (e) {
    console.warn("markReservationsAllSeenIfOnReservationPage error:", e);
  }
}
/**
 * Prevent empty links from jumping
 */
function preventEmptyLinks() {
    const dummyLinks = document.querySelectorAll('a[href="#"], a[href="javascript:void(0)"]');

    dummyLinks.forEach(function (link) {
        link.addEventListener("click", function (e) {
            e.preventDefault();
        });
    });
}

/**
 * Close sidebar on mobile after clicking nav link
 */
function handleMobileNavClose() {
    const sidebar = document.getElementById("sidebar");
    const navLinks = document.querySelectorAll(".sidebar-nav .nav-link");

    navLinks.forEach(function (link) {
        link.addEventListener("click", function () {
            if (window.innerWidth <= 991 && sidebar) {
                sidebar.classList.remove("show");
            }
        });
    });
}

/**
 * Set min date for all date inputs
 */
function setMinDateForDateInputs() {
    const dateInputs = document.querySelectorAll('input[type="date"]');
    const today = new Date().toISOString().split("T")[0];

    dateInputs.forEach(input => {
        input.setAttribute("min", today);
    });
}

/**
 * Format currency in VND
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND"
    }).format(amount);
}

/**
 * Format date
 */
function formatDate(date) {
    return new Intl.DateTimeFormat("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    }).format(new Date(date));
}

/**
 * Format time
 */
function formatTime(date) {
    return new Intl.DateTimeFormat("vi-VN", {
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(date));
}

/**
 * Show toast notification
 */
function showToast(message, type = "success") {
    let toastContainer = document.getElementById("toast-container");

    if (!toastContainer) {
        toastContainer = document.createElement("div");
        toastContainer.id = "toast-container";
        toastContainer.className = "toast-container position-fixed top-0 end-0 p-3";
        toastContainer.style.zIndex = "1100";
        document.body.appendChild(toastContainer);
    }

    const toast = document.createElement("div");
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.setAttribute("role", "alert");
    toast.setAttribute("aria-live", "assertive");
    toast.setAttribute("aria-atomic", "true");

    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;

    toastContainer.appendChild(toast);

    const bsToast = new bootstrap.Toast(toast, {
        autohide: true,
        delay: 3000
    });

    bsToast.show();

    toast.addEventListener("hidden.bs.toast", function () {
        toast.remove();
    });
}

/**
 * Confirm dialog
 */
function confirmDialog(message) {
    return new Promise((resolve) => {
        const modal = document.createElement("div");
        modal.className = "modal fade";
        modal.tabIndex = -1;

        modal.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Xác nhận</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>
                    <div class="modal-body">
                        <p class="mb-0">${message}</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="button" class="btn btn-danger" id="confirmBtn">Xác nhận</button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        const bsModal = new bootstrap.Modal(modal);
        let resolved = false;

        bsModal.show();

        modal.querySelector("#confirmBtn").addEventListener("click", function () {
            resolved = true;
            bsModal.hide();
            resolve(true);
        });

        modal.addEventListener("hidden.bs.modal", function () {
            modal.remove();
            if (!resolved) {
                resolve(false);
            }
        });
    });
}

/**
 * Delete item helper
 */
async function deleteItem(id, itemName) {
    const confirmed = await confirmDialog(`Bạn có chắc chắn muốn xóa ${itemName}?`);
    if (confirmed) {
        showToast(`Đã xóa ${itemName} thành công`, "success");
        return true;
    }
    return false;
}

/**
 * Form validation helper
 */
function validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return false;

    form.classList.add("was-validated");
    return form.checkValidity();
}

/**
 * Reset form
 */
function resetForm(formId) {
    const form = document.getElementById(formId);
    if (form) {
        form.reset();
        form.classList.remove("was-validated");
    }
}

/**
 * Handle image preview
 */
function handleImagePreview(input, previewId) {
    const preview = document.getElementById(previewId);
    if (input.files && input.files[0] && preview) {
        const reader = new FileReader();
        reader.onload = function (e) {
            preview.src = e.target.result;
        };
        reader.readAsDataURL(input.files[0]);
    }
}

/**
 * Search table rows
 */
function searchTable(searchInput, tableId) {
    const table = document.getElementById(tableId);
    if (!searchInput || !table) return;

    const tbody = table.getElementsByTagName("tbody")[0];
    if (!tbody) return;

    const filter = searchInput.value.toLowerCase();
    const rows = tbody.getElementsByTagName("tr");

    for (let i = 0; i < rows.length; i++) {
        const cells = rows[i].getElementsByTagName("td");
        let found = false;

        for (let j = 0; j < cells.length; j++) {
            const cellText = cells[j].textContent || cells[j].innerText;
            if (cellText.toLowerCase().indexOf(filter) > -1) {
                found = true;
                break;
            }
        }

        rows[i].style.display = found ? "" : "none";
    }
}

/**
 * Load reservation unread badge
 */
async function loadReservationUnreadBadge() {
    const badge = document.getElementById("reservationUnreadBadge");
    if (!badge) return 0;

    try {
        const response = await fetch(RESERVATION_UNREAD_URL, {
            credentials: "include"
        });

        if (!response.ok) {
            badge.classList.add("d-none");
            return 0;
        }

        const count = await response.json();

        if (count > 0) {
            badge.textContent = count > 99 ? "99+" : count;
            badge.classList.remove("d-none");
        } else {
            badge.textContent = "0";
            badge.classList.add("d-none");
        }

        return count;
    } catch (error) {
        console.error("Lỗi loadReservationUnreadBadge:", error);
        badge.classList.add("d-none");
        return 0;
    }
}

/**
 * Load order unread badge
 */
async function loadOrderUnreadBadge() {
    const badge = document.getElementById("orderUnreadBadge");
    if (!badge) return 0;

    try {
        const response = await fetch(ORDER_UNREAD_URL, {
            credentials: "include"
        });

        if (!response.ok) {
            badge.classList.add("d-none");
            return 0;
        }

        const count = await response.json();

        if (count > 0) {
            badge.textContent = count > 99 ? "99+" : count;
            badge.classList.remove("d-none");
        } else {
            badge.textContent = "0";
            badge.classList.add("d-none");
        }

        return count;
    } catch (error) {
        console.error("Lỗi loadOrderUnreadBadge:", error);
        badge.classList.add("d-none");
        return 0;
    }
}

/**
 * Pulse badge animation helper
 */
function pulseBadge(id) {
    const badge = document.getElementById(id);
    if (!badge) return;

    badge.classList.remove("badge-pulse");
    void badge.offsetWidth;
    badge.classList.add("badge-pulse");
}

/**
 * Play notification sound if available
 */
function playNotificationSound() {
    const audio = document.getElementById("notificationSound");
    if (!audio) return;

    audio.currentTime = 0;
    audio.play().catch(err => {
        console.log("Không thể phát âm thanh:", err);
    });
}
function updateTopbarUnreadBadge(reservationCount, orderCount) {
    const badge = document.getElementById("topbarUnreadBadge");
    if (!badge) return;

    const total = (Number(reservationCount) || 0) + (Number(orderCount) || 0);

    if (total > 0) {
        badge.textContent = total > 99 ? "99+" : String(total);
        badge.classList.remove("d-none");
    } else {
        badge.textContent = "0";
        badge.classList.add("d-none");
    }
}
/**
 * Refresh both sidebar badges
 */
async function refreshSidebarBadges(isFirstLoad = false) {
    const [reservationCount, orderCount] = await Promise.all([
        loadReservationUnreadBadge(),
        loadOrderUnreadBadge()
    ]);

    updateTopbarUnreadBadge(reservationCount, orderCount);

    if (!isFirstLoad && reservationCount > lastReservationUnreadCount) {
        pulseBadge("reservationUnreadBadge");
        playNotificationSound();
    }

    if (!isFirstLoad && orderCount > lastOrderUnreadCount) {
        pulseBadge("orderUnreadBadge");
        playNotificationSound();
    }

    lastReservationUnreadCount = reservationCount;
    lastOrderUnreadCount = orderCount;
}
const VERIFY_API = "/api/customer/orders/verify-reservation";
const CREATE_ORDER_API = "/api/customer/orders";
const DISH_API = "/api/dishes";
//const couponCodeInput = document.getElementById("couponCodeInput");

let verifiedReservation = null;
let dishes = [];
let filteredDishes = [];
let cart = [];

const reservationIdInput = document.getElementById("reservationIdInput");
const customerPhoneInput = document.getElementById("customerPhoneInput");
const verifyBtn = document.getElementById("verifyBtn");
const verifyMessage = document.getElementById("verifyMessage");

const bookingInfoPanel = document.getElementById("bookingInfoPanel");
const orderLayout = document.getElementById("orderLayout");

const infoReservationId = document.getElementById("infoReservationId");
const infoCustomerName = document.getElementById("infoCustomerName");
const infoReservationTime = document.getElementById("infoReservationTime");
const infoTableNumber = document.getElementById("infoTableNumber");

const searchInput = document.getElementById("searchInput");
const categoryFilter = document.getElementById("categoryFilter");
const menuGrid = document.getElementById("menuGrid");
const cartList = document.getElementById("cartList");
const orderNote = document.getElementById("orderNote");
const totalItems = document.getElementById("totalItems");
const subTotal = document.getElementById("subTotal");
const grandTotal = document.getElementById("grandTotal");
const submitOrderBtn = document.getElementById("submitOrderBtn");
const VERIFY_TOKEN_API = "/api/public/orders/verify-token";
const CREATE_ORDER_PUBLIC_API = "/api/public/orders";
let magicToken = null;

/**
 * Nếu API yêu cầu login mà user chưa login:
 * - redirect sang trang login
 * - kèm next để login xong quay lại đúng link email
 */
function redirectToLoginWithNext() {
  const next = encodeURIComponent(window.location.href);
  window.location.href = `/auth/login.html?next=${next}`;
}

function handleAuthRedirectIfNeeded(response) {
  if (response.status === 401 || response.status === 403) {
    redirectToLoginWithNext();
    return true;
  }
  return false;
}

verifyBtn.addEventListener("click", verifyReservation);
searchInput.addEventListener("input", filterMenu);
categoryFilter.addEventListener("change", filterMenu);
submitOrderBtn.addEventListener("click", submitOrder);

document.addEventListener("DOMContentLoaded", async function () {
  const params = new URLSearchParams(window.location.search);

  const token = params.get("token");
  const reservationId = params.get("reservationId");
  const phone = params.get("phone");

  if (token) {
    magicToken = token;

    try {
      setVerifyMessage("Đang xác minh link...", "");

      const res = await fetch(VERIFY_TOKEN_API, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify({ token: magicToken })
      });

      const text = await res.text();
      const data = text ? JSON.parse(text) : null;

      if (!res.ok) throw new Error((data && (data.message || data.error)) || text || "Link không hợp lệ");
      if (!data) throw new Error("Server không trả dữ liệu hợp lệ");

      verifiedReservation = data;
      showVerifiedReservation(data);
      setVerifyMessage("Xác minh thành công. B���n có thể chọn món.", "success");

      await loadDishes();
      orderLayout.classList.remove("hidden");
    } catch (e) {
      console.error("verifyToken error:", e);
      verifiedReservation = null;
      bookingInfoPanel.classList.add("hidden");
      orderLayout.classList.add("hidden");
      setVerifyMessage(e.message || "Link không hợp lệ hoặc đã hết hạn.", "error");
    }

    return; // có token rồi thì không cần prefill reservationId/phone
  }

  // fallback cũ: prefill theo query
  if (reservationIdInput && reservationId) reservationIdInput.value = reservationId;
  if (customerPhoneInput && phone) customerPhoneInput.value = phone;
});

function formatCurrency(value) {
  return Number(value).toLocaleString("vi-VN") + " đ";
}

function setVerifyMessage(message, type = "") {
  verifyMessage.textContent = message;
  verifyMessage.className = "verify-message";
  if (type) {
    verifyMessage.classList.add(type);
  }
}

async function verifyReservation() {
  const reservationId = reservationIdInput.value.trim();
  const customerPhone = customerPhoneInput.value.trim();

  if (!reservationId || !customerPhone) {
    setVerifyMessage("Vui lòng nhập mã đặt bàn và số điện thoại.", "error");
    return;
  }

  try {
    setVerifyMessage("Đang xác minh...", "");

    const response = await fetch(VERIFY_API, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify({
        reservationId: Number(reservationId),
        customerPhone: customerPhone // ✅ đổi key: customerPhone -> phone
      })
    });

    if (handleAuthRedirectIfNeeded(response)) return;

    const responseText = await response.text();
    let data = null;

    try {
      data = responseText ? JSON.parse(responseText) : null;
    } catch (e) {
      data = null;
    }

    if (!response.ok) {
      throw new Error((data && (data.message || data.error)) || responseText || "Xác minh thất bại");
    }

    // ✅ tránh crash nếu server trả body rỗng hoặc không phải JSON
    if (!data) {
      throw new Error("Server không trả dữ liệu hợp lệ khi xác minh đặt bàn.");
    }

    verifiedReservation = data;
    showVerifiedReservation(data);
    setVerifyMessage("Xác minh đặt bàn thành công.", "success");

    await loadDishes();
    orderLayout.classList.remove("hidden");
  } catch (error) {
    console.error("Lỗi verifyReservation:", error);
    verifiedReservation = null;
    bookingInfoPanel.classList.add("hidden");
    orderLayout.classList.add("hidden");
    setVerifyMessage(error.message || "Không thể xác minh đặt bàn.", "error");
  }
}

function showVerifiedReservation(data) {
  if (!data) return; // ✅ chống null

  bookingInfoPanel.classList.remove("hidden");

  infoReservationId.textContent = "#" + (data.reservationId ?? "-");
  infoCustomerName.textContent = data.customerName ?? "-";
  infoReservationTime.textContent = `${data.reservationDate ?? "-"} ${formatTime(data.reservationTime)}`;
  infoTableNumber.textContent = data.tableNumber ? `Bàn ${data.tableNumber}` : "Chưa có bàn";
}

async function loadDishes() {
  try {
    const response = await fetch(DISH_API, {
      credentials: "include", // <-- QUAN TRỌNG nếu endpoint yêu cầu login
      headers: {
        "Accept": "application/json"
      }
    });

    if (handleAuthRedirectIfNeeded(response)) return;

    if (!response.ok) {
      throw new Error("Không tải được danh sách món ăn");
    }

    dishes = await response.json();
    filteredDishes = [...dishes];

    renderCategoryOptions(dishes);
    renderMenu(filteredDishes);
  } catch (error) {
    console.error("Lỗi loadDishes:", error);
    menuGrid.innerHTML = `<div style="color:#f87171;padding:1rem;">Không thể tải danh sách món ăn.</div>`;
  }
}

function renderCategoryOptions(dishList) {
  const categories = [...new Set(dishList.map(item => item.categoryName || "Khác"))];

  categoryFilter.innerHTML = `<option value="all">Tất cả danh mục</option>`;

  categories.forEach(category => {
    categoryFilter.innerHTML += `<option value="${category}">${category}</option>`;
  });
}

function filterMenu() {
  const keyword = searchInput.value.trim().toLowerCase();
  const category = categoryFilter.value;

  filteredDishes = dishes.filter(dish => {
    const dishName = (dish.name || "").toLowerCase();
    const dishCategory = dish.categoryName || "Khác";

    const matchKeyword = !keyword || dishName.includes(keyword);
    const matchCategory = category === "all" || dishCategory === category;

    return matchKeyword && matchCategory;
  });

  renderMenu(filteredDishes);
}

function renderMenu(items) {
  menuGrid.innerHTML = "";

  if (!items.length) {
    menuGrid.innerHTML = `<div style="color:#94a3b8;padding:1rem;">Không tìm thấy món phù hợp.</div>`;
    return;
  }

  items.forEach(dish => {
    const dishId = dish.id;
    const dishName = dish.name || "Không tên";
    const dishCategory = dish.category?.name || dish.category || "Khác";
    const dishPrice = Number(dish.price || 0);
    const dishDescription = dish.description || "Chưa có mô tả";
    const dishImage = dish.imageUrl || dish.image || "https://via.placeholder.com/600x400?text=Dish";

    const card = document.createElement("div");
    card.className = "menu-card";

    card.innerHTML = `
      <img src="${dishImage}" alt="${dishName}">
      <div class="menu-body">
        <span class="badge">${dishCategory}</span>
        <div class="menu-top">
          <h4>${dishName}</h4>
          <span class="price">${formatCurrency(dishPrice)}</span>
        </div>
        <div class="menu-desc">${dishDescription}</div>
        <div class="menu-actions">
          <button class="btn btn-primary" onclick="addToCart(${dishId})">Thêm vào giỏ</button>
        </div>
      </div>
    `;

    menuGrid.appendChild(card);
  });
}

function addToCart(dishId) {
  const foundDish = dishes.find(d => d.id === dishId);
  if (!foundDish) return;

  const existing = cart.find(item => item.dishId === dishId);

  if (existing) {
    existing.quantity += 1;
  } else {
    cart.push({
      dishId: foundDish.id,
      dishName: foundDish.name,
      unitPrice: Number(foundDish.price || 0),
      quantity: 1,
      note: ""
    });
  }

  renderCart();
}

function updateQuantity(dishId, delta) {
  const item = cart.find(i => i.dishId === dishId);
  if (!item) return;

  item.quantity += delta;

  if (item.quantity <= 0) {
    cart = cart.filter(i => i.dishId !== dishId);
  }

  renderCart();
}

function removeItem(dishId) {
  cart = cart.filter(i => i.dishId !== dishId);
  renderCart();
}

function updateItemNote(dishId, value) {
  const item = cart.find(i => i.dishId === dishId);
  if (!item) return;
  item.note = value;
}

function renderCart() {
  if (!cart.length) {
    cartList.innerHTML = `<div class="empty-cart">Chưa có món nào trong giỏ.</div>`;
  } else {
    cartList.innerHTML = "";

    cart.forEach(item => {
      const subtotalValue = item.unitPrice * item.quantity;
      const div = document.createElement("div");
      div.className = "cart-item";

      div.innerHTML = `
        <div class="cart-item-top">
          <div>
            <h5>${item.dishName}</h5>
            <div style="color:#94a3b8;font-size:0.9rem;">${formatCurrency(item.unitPrice)} / món</div>
          </div>
          <div class="cart-item-price">${formatCurrency(subtotalValue)}</div>
        </div>

        <div class="cart-controls">
          <div class="qty-box">
            <button class="qty-btn" onclick="updateQuantity(${item.dishId}, -1)">−</button>
            <span class="qty-value">${item.quantity}</span>
            <button class="qty-btn" onclick="updateQuantity(${item.dishId}, 1)">+</button>
          </div>

          <button class="remove-btn" onclick="removeItem(${item.dishId})">Xóa</button>
        </div>

        <textarea class="cart-note" placeholder="Ghi chú cho món này..." oninput="updateItemNote(${item.dishId}, this.value)">${item.note || ""}</textarea>
      `;

      cartList.appendChild(div);
    });
  }

  updateSummary();
}

function updateSummary() {
  const itemsCount = cart.reduce((sum, item) => sum + item.quantity, 0);
  const totalValue = cart.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);

  totalItems.textContent = itemsCount;
  subTotal.textContent = formatCurrency(totalValue);
  grandTotal.textContent = formatCurrency(totalValue);
}

async function submitOrder() {
  if (!verifiedReservation) {
    alert("Vui lòng xác minh đặt bàn trước.");
    return;
  }

  if (!cart.length) {
    alert("Vui lòng chọn ít nhất 1 món.");
    return;
  }

  submitOrderBtn.disabled = true;
  submitOrderBtn.textContent = "Đang xử lý...";

  try {
    const paymentOption =
      document.querySelector('input[name="paymentOption"]:checked')?.value || "PAY_AT_RESTAURANT";

    const endpoint = magicToken ? CREATE_ORDER_PUBLIC_API : CREATE_ORDER_API;
    const couponCode = document.getElementById("couponCodeInput")?.value?.trim() || null;

    const payload = magicToken
      ? {
          token: magicToken,
          note: orderNote.value.trim(),
          paymentOption,
          couponCode,
          items: cart.map(item => ({
            dishId: item.dishId,
            quantity: item.quantity,
            note: item.note
          }))
        }
      : {
          reservationId: verifiedReservation.reservationId,
          customerPhone: verifiedReservation.customerPhone,
          note: orderNote.value.trim(),
          paymentOption,
          couponCode,
          items: cart.map(item => ({
            dishId: item.dishId,
            quantity: item.quantity,
            note: item.note
          }))
        };

    const fetchOptions = {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json" },
      body: JSON.stringify(payload)
    };

    if (!magicToken) fetchOptions.credentials = "include";

    const response = await fetch(endpoint, fetchOptions);

    if (!magicToken && handleAuthRedirectIfNeeded(response)) return;

    const responseText = await response.text();
    let data = null;
    try {
      data = responseText ? JSON.parse(responseText) : null;
    } catch (e) {
      data = null;
    }

    if (!response.ok) {
      throw new Error((data && (data.message || data.error)) || responseText || "Tạo order thất bại");
    }

    // PAY_NOW
    if (paymentOption === "PAY_NOW") {
      const payResponse = await fetch(`/api/payment/vnpay/create?orderId=${data.orderId}`, {
        method: "POST",
        credentials: "include",
        headers: { "Accept": "application/json" }
      });

      if (handleAuthRedirectIfNeeded(payResponse)) return;

      const payText = await payResponse.text();
      let payData = null;
      try {
        payData = payText ? JSON.parse(payText) : null;
      } catch (e) {
        payData = null;
      }

      if (!payResponse.ok || !payData?.paymentUrl) {
        throw new Error((payData && (payData.message || payData.error)) || payText || "Không tạo được link thanh toán VNPAY");
      }

      window.location.href = payData.paymentUrl;
      return;
    }

    // PAY_AT_RESTAURANT
    alert(`Gửi order thành công! Mã order: #${data.orderId}. Bạn sẽ thanh toán tại quán.`);

    cart = [];
    orderNote.value = "";
    renderCart();
  } catch (error) {
    console.error("Lỗi submitOrder:", error);
    alert(error.message || "Không thể gửi order.");
  } finally {
    submitOrderBtn.disabled = false;
    submitOrderBtn.textContent = "GỬI ĐƠN";
  }
}

// expose for inline onclick
window.addToCart = addToCart;
window.updateQuantity = updateQuantity;
window.removeItem = removeItem;
window.updateItemNote = updateItemNote;

function formatTime(time) {
  if (!time) return "";
  return String(time).length >= 5 ? String(time).substring(0, 5) : time;
}
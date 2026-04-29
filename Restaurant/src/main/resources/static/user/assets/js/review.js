console.log("REVIEW.JS ĐÃ NẠP! " + new Date());
(function () {
  const page = (location.pathname.split("/").pop() || "").toLowerCase();
  if (page !== "review.html") return;

  const $ = (id) => document.getElementById(id);

  function escapeHtml(s) {
    return (s ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#039;",
    }[c]));
  }
  function renderMediaGrid(urls) {
    if (!urls || !Array.isArray(urls) || urls.length === 0) return "";
    const imgs = urls.slice(0, 6).map(u => `
      <img src="${escapeHtml(u)}" alt="review image" loading="lazy">
    `).join("");
    return `<div class="review-media">${imgs}</div>`;
  }

  function formatDate(dt) {
    if (!dt) return "";
    return String(dt).split("T")[0]; // yyyy-mm-dd
  }

  function renderStars(rating) {
    const r = Number(rating || 0);
    let html = `<div class="stars">`;
    for (let i = 1; i <= 5; i++) html += `<span class="star">${i <= r ? "★" : "☆"}</span>`;
    html += `</div>`;
    return html;
  }

  async function fetchJson(url, opt) {
    const res = await fetch(url, { credentials: "include", ...(opt || {}) });
    if (!res.ok) {
      const text = await res.text().catch(() => "");
      throw new Error(`HTTP ${res.status} - ${text}`);
    }
    return res.json();
  }

  async function loadStats() {
    const stats = await fetchJson("/api/reviews/stats");
    if ($("avgRating")) $("avgRating").textContent = (stats.averageRating ?? 0).toFixed(1);
    if ($("totalReviews")) $("totalReviews").textContent = String(stats.totalReviews ?? 0);
    if ($("recommendedPercent")) $("recommendedPercent").textContent = String(stats.recommendedPercent ?? 0) + "%";
  }

  async function loadRecent() {
    const list = await fetchJson("/api/reviews/recent?limit=10");
    const container = $("reviewsList");
    if (!container) return;

    if (!Array.isArray(list) || list.length === 0) {
      container.innerHTML = `<p>No reviews yet. Be the first to review!</p>`;
      return;
    }

    function initials(name) {
      const n = (name || "G").trim();
      return n.length ? n[0].toUpperCase() : "G";
    }

    function stars(rating) {
      const r = Number(rating || 0);
      let html = `<div class="review-stars" aria-label="rating ${r}/5">`;
      for (let i = 1; i <= 5; i++) html += `<span>${i <= r ? "★" : "☆"}</span>`;
      html += `</div>`;
      return html;
    }

    function trimText(text, max = 220) {
      const t = (text || "").trim();
      if (t.length <= max) return { short: t, full: t, trimmed: false };
      return { short: t.slice(0, max) + "…", full: t, trimmed: true };
    }

    const itemsHtml = list.map(r => {
      const name = r.authorName || "Guest";
      const date = formatDate(r.createdAt);
      const t = trimText(r.comment || "", 240);

      const ownerReplyHtml = r.ownerReply
        ? `<div class="owner-reply-box">
             <div class="owner-reply-title">Phản hồi từ nhà hàng</div>
             <div>${escapeHtml(r.ownerReply)}</div>
           </div>`
        : "";

      // ✅ render media nếu backend trả về r.mediaUrls = [...]
      const mediaHtml = renderMediaGrid(r.mediaUrls);

      return `
        <div class="review-item" data-review-id="${r.id}">
          <div class="review-head">
            <div class="review-user">
              <div class="review-avatar">${escapeHtml(initials(name))}</div>
              <div style="min-width:0;">
                <div class="review-username">${escapeHtml(name)}</div>
                <div class="review-meta">${escapeHtml(date)}</div>
              </div>
            </div>
            ${stars(r.rating)}
          </div>

          <div class="review-body">
            <div class="review-text" data-full="${escapeHtml(t.full)}">${escapeHtml(t.short || "(No comment)")}</div>
            ${t.trimmed ? `<div class="review-more">Xem thêm</div>` : ""}
          </div>

          ${mediaHtml}
          ${ownerReplyHtml}
        </div>
      `;
    }).join("");

    container.innerHTML = `<div class="review-feed">${itemsHtml}</div>`;

    // “Xem thêm” toggle
    container.querySelectorAll(".review-item").forEach(item => {
      const more = item.querySelector(".review-more");
      if (!more) return;

      more.addEventListener("click", () => {
        const textEl = item.querySelector(".review-text");
        const full = textEl.getAttribute("data-full") || "";
        const isExpanded = more.getAttribute("data-expanded") === "1";

        if (!isExpanded) {
          textEl.textContent = full;
          more.textContent = "Thu gọn";
          more.setAttribute("data-expanded", "1");
        } else {
          const again = trimText(full, 240);
          textEl.textContent = again.short || "(No comment)";
          more.textContent = "Xem thêm";
          more.setAttribute("data-expanded", "0");
        }
      });
    });
  }

  async function tryLoadEligibleAndGateForm() {
    const hint = $("reviewLoginHint");
    const form = $("reviewForm");
    const target = $("reviewTarget");

    if (!form || !target) return;

    // helper: show/hide
    const showHint = (html) => {
      if (hint) {
        hint.style.display = "block";
        if (html) hint.innerHTML = html;
      }
      form.style.display = "none";
    };
    const showForm = () => {
      if (hint) hint.style.display = "none";
      form.style.display = "block";
    };

    try {
      const eligible = await fetchJson("/api/reviews/eligible");
      console.log("eligible response:", eligible);

      const items = Array.isArray(eligible?.items) ? eligible.items : [];

      // luôn show form nếu login OK (eligible 200)
      showForm();

      target.innerHTML = `<option value="">Chọn đơn hàng/đặt bàn đã hoàn thành</option>`;
      for (const it of items) {
        const opt = document.createElement("option");
        opt.value = it.value;
        opt.textContent = it.label;
        target.appendChild(opt);
      }

      // message khi rỗng
      const existing = document.getElementById("eligibleEmptyMsg");
      if (items.length === 0) {
        if (!existing) {
          const msg = document.createElement("div");
          msg.id = "eligibleEmptyMsg";
          msg.style.marginTop = "0.75rem";
          msg.style.color = "#666";
          msg.textContent = "Bạn chưa có đơn hàng/đặt bàn hoàn thành để đánh giá.";
          target.parentElement?.appendChild(msg);
        }
      } else {
        if (existing) existing.remove();
      }
    } catch (e) {
      console.error("eligible error:", e);

      // phân biệt status nếu fetchJson có gắn status
      const status = e?.status || e?.response?.status;

      if (status === 403) {
        showHint(`Bạn không có quyền đánh giá. Vui lòng đăng nhập bằng tài khoản khách hàng.`);
        return;
      }

      // mặc định coi như chưa login / session hết hạn
      showHint(`Vui lòng <a href="/auth/login.html?redirect=/user/review.html">đăng nhập</a> để gửi đánh giá.`);
    }
  }

  function parseTarget(value) {
    const [type, idStr] = String(value || "").split(":");
    const id = Number(idStr);
    if (!type || !id) return { orderId: null, reservationId: null };
    if (type === "order") return { orderId: id, reservationId: null };
    if (type === "reservation") return { orderId: null, reservationId: id };
    return { orderId: null, reservationId: null };
  }

  async function onSubmit(e) {
    console.log("REVIEW FORM SUBMIT RUNNING");
    e.preventDefault();

    const targetVal = $("reviewTarget")?.value;
    const ratingVal = $("rating")?.value;
    const commentVal = $("review-text")?.value;
    // const files = $("reviewImages")?.files;
    const { orderId, reservationId } = parseTarget(targetVal);

    if (!orderId && !reservationId) return alert("Vui lòng chọn đơn hàng/đặt bàn để đánh giá.");
    const rating = Number(ratingVal);
    if (!rating || rating < 1 || rating > 5) return alert("Rating không hợp lệ.");
    if (!commentVal || !commentVal.trim()) return alert("Vui lòng nhập nội dung đánh giá.");

    try {
      const payload = {
        orderId,
        reservationId,
        rating,
        comment: commentVal.trim(),
        mediaUrls: []
      };
      console.log("Sắp fetch /api/reviews với payload:", payload);

      await fetchJson("/api/reviews", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      alert("Cảm ơn bạn! Đánh giá đã được gửi.");
      const form = $("reviewForm");
      if (form) form.reset();

      if ($("reviewTarget")) $("reviewTarget").value = "";
      if ($("rating")) $("rating").value = "";
      if ($("review-text")) $("review-text").value = "";
      if ($("reviewImages")) $("reviewImages").value = "";

      await loadRecent();
      await loadStats();
      await tryLoadEligibleAndGateForm();
    } catch (e) {
      alert("Gửi đánh giá thất bại: " + e.message);
      console.error("LỖI GỬI REVIEW:", e);
    }
  }
  function connectReviewSocket() {
    const socket = new SockJS('/ws-notify');
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, function () {
      console.log("Review socket connected");

      stompClient.subscribe('/topic/reviews', async function (message) {
        console.log("New review event received:", message.body);
        await loadRecent();
        await loadStats();
        await tryLoadEligibleAndGateForm();
      });
    }, function (err) {
      console.error("Review socket error", err);
    });
  }

  document.addEventListener("DOMContentLoaded", async () => {
    try { await loadStats(); } catch (e) { console.warn("loadStats", e); }
    try { await loadRecent(); } catch (e) { console.warn("loadRecent", e); }
    await tryLoadEligibleAndGateForm();
    connectReviewSocket();
    const form = $("reviewForm");
    if (form) form.addEventListener("submit", onSubmit);
  });
})();

async function uploadReviewImages(files) {
  if (!files || files.length === 0) return [];

  const maxFiles = 6;
  const picked = Array.from(files).slice(0, maxFiles);

  const form = new FormData();
  for (const f of picked) form.append("files", f);

  const res = await fetch("/api/reviews/media", {
    method: "POST",
    body: form,
    credentials: "include",
  });

  const text = await res.text().catch(() => "");
  if (!res.ok) throw new Error(`Upload failed HTTP ${res.status} - ${text}`);

  const data = text ? JSON.parse(text) : null;
  return (data && Array.isArray(data.urls)) ? data.urls : [];
}





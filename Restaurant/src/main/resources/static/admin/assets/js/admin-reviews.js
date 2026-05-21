function escapeHtml(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function starsHtml(rating) {
  const r = Math.max(0, Math.min(5, Number(rating) || 0));
  let html = "";
  for (let i = 1; i <= 5; i++) {
    html += `<i class="bi ${i <= r ? "bi-star-fill" : "bi-star"}"></i>`;
  }
  return html;
}

function formatLocalDateTime(isoLike) {
  // "2026-04-18T16:15:46" -> hiển thị gọn
  if (!isoLike) return "";
  try {
    const d = new Date(isoLike);
    if (Number.isNaN(d.getTime())) return isoLike;
    return d.toLocaleString("vi-VN");
  } catch {
    return isoLike;
  }
}

async function loadStats() {
  const res = await fetch("/api/reviews/stats", { credentials: "include" });
  if (!res.ok) throw new Error("stats " + res.status);
  const s = await res.json();

  document.getElementById("adminAvgRating").textContent =
    (s.averageRating ?? 0).toFixed(1);

  document.getElementById("adminTotalReviews").textContent =
    s.totalReviews ?? 0;

  document.getElementById("adminRecommendedPercent").textContent =
    (s.recommendedPercent ?? 0) + "%";

  // API chưa có pending => để 0
  const pendingEl = document.getElementById("adminPendingCount");
  if (pendingEl) pendingEl.textContent = "0";

  // Card distribution (tạm show theo stats hiện có)
  const distAvg = document.getElementById("adminDistAvg");
  if (distAvg) distAvg.textContent = (s.averageRating ?? 0).toFixed(1);

  const distTotal = document.getElementById("adminDistTotal");
  if (distTotal) distTotal.textContent = `${s.totalReviews ?? 0} đánh giá`;
}
async function loadDistribution() {
  try {
    const res = await fetch('/api/reviews/distribution', { credentials: 'include' });
    if (!res.ok) {
      console.warn('distribution API not ok', res.status);
      return;
    }
    const d = await res.json();
    // d.distribution expected like { "5": 10, "4": 5, ... }
    const dist = d.distribution || {};
    const total = d.total || Object.values(dist).reduce((a,b)=>a+(Number(b||0)),0);
    // update total/avg if you want
    const distAvg = document.getElementById('adminDistAvg');
    if (distAvg && d.avg !== undefined) distAvg.textContent = (d.avg ?? 0).toFixed(1);
    const distTotal = document.getElementById('adminDistTotal');
    if (distTotal) distTotal.textContent = `${total} đánh giá`;

    for (let star = 5; star >= 1; star--) {
      const cnt = Number(dist[String(star)] || 0);
      const pct = total ? Math.round((cnt * 100) / total) : 0;
      const bar = document.getElementById(`adminBar${star}`);
      const cntEl = document.getElementById(`adminCount${star}`);
      if (bar) bar.style.width = pct + '%';
      if (cntEl) cntEl.textContent = cnt;
    }
  } catch (err) {
    console.error('loadDistribution error', err);
  }
}
async function loadRecentReviews() {
  const res = await fetch("/api/reviews/recent", { credentials: "include" });
  if (!res.ok) throw new Error("recent " + res.status);
  const items = await res.json(); // API trả array

  const el = document.getElementById("adminReviewsList");
  if (!items || items.length === 0) {
    el.innerHTML = `<div class="p-3 text-muted">Chưa có đánh giá nào.</div>`;
    return;
  }

  el.innerHTML = items.map(r => {
    const name = escapeHtml(r.authorName || "Guest");
    const comment = escapeHtml(r.comment || "");
    const created = formatLocalDateTime(r.createdAt);
    const initials = name.trim().slice(0, 2).toUpperCase();

    return `
      <div class="review-item p-3 border-bottom" style="border-color: var(--gray-800) !important;">
        <div class="d-flex justify-content-between mb-2">
          <div class="d-flex align-items-center gap-2">
            <div class="avatar avatar-sm">${escapeHtml(initials)}</div>
            <div>
              <strong>${name}</strong>
              <div class="text-warning" style="font-size: 0.75rem;">
                ${starsHtml(r.rating)}
              </div>
            </div>
          </div>
          <small class="text-muted">${escapeHtml(created)}</small>
        </div>

        <p class="mb-2 text-muted">${comment}</p>

        ${
          r.ownerReply
            ? `<div class="alert alert-secondary py-2 mb-2"><strong>Phản hồi:</strong> ${escapeHtml(r.ownerReply)}</div>`
            : ""
        }

        <div class="d-flex gap-2">
           <button class="btn btn-sm btn-outline-primary btn-reply" data-id="${r.id}">
             <i class="bi bi-reply me-1"></i> Phản hồi
           </button>
        </div>
      </div>
    `;
  }).join("");
}

document.addEventListener("DOMContentLoaded", async () => {
  try {
    await Promise.all([loadStats(), loadRecentReviews(), loadDistribution()]);
  } catch (e) {
    console.error(e);
    const el = document.getElementById("adminReviewsList");
    if (el) el.innerHTML = `<div class="p-3 text-danger">Lỗi tải đánh giá. Mở Console để xem chi tiết.</div>`;
  }
});
// Khởi tạo đối tượng Modal của Bootstrap
const replyModal = new bootstrap.Modal(document.getElementById('replyModal'));

// Lắng nghe sự kiện click trong khu vực chứa danh sách đánh giá
document.getElementById('adminReviewsList').addEventListener('click', function(e) {
    // Tìm phần tử chứa chữ "Phản hồi" vừa được click
    const replyBtn = e.target.closest('.btn-outline-primary');

    if (replyBtn && replyBtn.innerText.includes('Phản hồi')) {
        e.preventDefault();

        // Đi ngược lên DOM để tìm phần tử bọc toàn bộ review này
        const reviewCard = replyBtn.closest('div[style*="border-bottom"], div.border-bottom') || replyBtn.parentElement.parentElement;

        // Bóc tách dữ liệu (Lấy text tên khách và nội dung đánh giá)
        const customerName = reviewCard.querySelector('strong')?.innerText || 'Guest';
        // Giả sử nội dung đánh giá nằm ở thẻ p hoặc div ngay trên nút
        const reviewContent = reviewCard.querySelector('p')?.innerText || reviewCard.children[1]?.innerText || 'Không có nội dung';

        // Lấy ID của đánh giá
        const reviewId = replyBtn.getAttribute('data-id') || '';

        // Đổ dữ liệu vào Modal
        document.getElementById('modalCustomerName').innerText = customerName;
        document.getElementById('modalReviewContent').innerText = reviewContent;
        document.getElementById('currentReviewId').value = reviewId;
        document.getElementById('replyText').value = ''; // Xóa trắng ô nhập liệu cũ

        // Hiển thị Modal
        replyModal.show();
    }
});
document.getElementById('btnSubmitReply').addEventListener('click', async function() {
    const reviewId = document.getElementById('currentReviewId').value;
    const replyContent = document.getElementById('replyText').value.trim();

    if (!replyContent) {
        alert('Vui lòng nhập nội dung phản hồi!');
        return;
    }

    // Đổi trạng thái nút lúc đang chờ API để UX tốt hơn
    const btnSubmit = this;
    const originalText = btnSubmit.innerHTML;
    btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Đang gửi...';
    btnSubmit.disabled = true;

    try {
            const response = await fetch(`/api/reviews/${reviewId}/reply`, {
                method: 'POST', // Hoặc PUT tùy bạn thiết kế API
                headers: {
                    'Content-Type': 'application/json',
                    // 'Authorization': 'Bearer ' + localStorage.getItem('token') // Nhớ mở comment nếu có dùng JWT nhé
                },
                // ĐỔI TÊN BIẾN Ở ĐÂY:
                body: JSON.stringify({ ownerReply: replyContent })
            });

        if (response.ok) {
            alert('Gửi phản hồi thành công!');
            replyModal.hide();
            // Khuyên dùng: Gọi lại hàm render danh sách để UI tự động cập nhật
            // loadReviews();
        } else {
            alert('Có lỗi xảy ra khi gửi phản hồi.');
        }
    } catch (error) {
        console.error('Lỗi:', error);
        alert('Không thể kết nối đến server!');
    } finally {
        // Khôi phục trạng thái nút
        btnSubmit.innerHTML = originalText;
        btnSubmit.disabled = false;
    }
});

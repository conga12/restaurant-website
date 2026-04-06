/* ============================================
   RestaurantOS - Main JavaScript
   ============================================ */

// ============================================
// MOBILE MENU TOGGLE
// ============================================

const hamburger = document.querySelector('.hamburger');
const nav = document.querySelector('nav');

if (hamburger) {
  hamburger.addEventListener('click', () => {
    hamburger.classList.toggle('active');
    nav.classList.toggle('active');
  });
}

// Close menu when clicking on a link
const navLinks = document.querySelectorAll('nav a');
navLinks.forEach(link => {
  link.addEventListener('click', () => {
    if (hamburger && hamburger.classList.contains('active')) {
      hamburger.classList.remove('active');
      nav.classList.remove('active');
    }
  });
});

// Close menu when clicking outside
document.addEventListener('click', (e) => {
  if (hamburger && !hamburger.contains(e.target) && !nav.contains(e.target)) {
    if (hamburger.classList.contains('active')) {
      hamburger.classList.remove('active');
      nav.classList.remove('active');
    }
  }
});

// ============================================
// MODAL HANDLING
// ============================================

function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
  }
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('show');
    document.body.style.overflow = 'auto';
  }
}

// Close modal when clicking the close button
document.querySelectorAll('.modal-close').forEach(btn => {
  btn.addEventListener('click', function() {
    this.closest('.modal').classList.remove('show');
    document.body.style.overflow = 'auto';
  });
});

// Close modal when clicking outside the modal content
document.querySelectorAll('.modal').forEach(modal => {
  modal.addEventListener('click', function(e) {
    if (e.target === this) {
      this.classList.remove('show');
      document.body.style.overflow = 'auto';
    }
  });
});

// ============================================
// FORM VALIDATION
// ============================================

function validateEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
}

function validatePhoneNumber(phone) {
  const re = /^[0-9\-\+\(\)\s]{10,}$/;
  return re.test(phone.replace(/\s/g, ''));
}

function validateForm(formId) {
  const form = document.getElementById(formId);
  if (!form) return false;

  let isValid = true;
  const inputs = form.querySelectorAll('input, select, textarea');

  inputs.forEach(input => {
    const error = input.nextElementSibling;
    const errorMsg = error && error.classList.contains('form-error') ? error : null;

    if (errorMsg) {
      errorMsg.classList.remove('show');
    }

    // Validate required fields
    if (input.hasAttribute('required') && !input.value.trim()) {
      if (errorMsg) {
        errorMsg.textContent = 'This field is required';
        errorMsg.classList.add('show');
      }
      isValid = false;
    }

    // Validate email
    if (input.type === 'email' && input.value && !validateEmail(input.value)) {
      if (errorMsg) {
        errorMsg.textContent = 'Please enter a valid email';
        errorMsg.classList.add('show');
      }
      isValid = false;
    }

    // Validate phone
    if (input.classList.contains('phone-input') && input.value && !validatePhoneNumber(input.value)) {
      if (errorMsg) {
        errorMsg.textContent = 'Please enter a valid phone number';
        errorMsg.classList.add('show');
      }
      isValid = false;
    }

    // Validate min date (for reservation)
    if (input.type === 'date' && input.value) {
      const selectedDate = new Date(input.value);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      if (selectedDate < today) {
        if (errorMsg) {
          errorMsg.textContent = 'Please select a future date';
          errorMsg.classList.add('show');
        }
        isValid = false;
      }
    }
  });

  return isValid;
}

// ============================================
// RESERVATION FORM HANDLING
// ============================================

const reservationForm = document.getElementById('reservationForm');
if (reservationForm) {
  reservationForm.addEventListener('submit', function(e) {
    e.preventDefault();

    if (!validateForm('reservationForm')) {
      return;
    }

    // Get form data
    const formData = {
      name: document.getElementById('name')?.value,
      email: document.getElementById('email')?.value,
      phone: document.getElementById('phone')?.value,
      date: document.getElementById('date')?.value,
      time: document.getElementById('time')?.value,
      guests: document.getElementById('guests')?.value,
      specialRequests: document.getElementById('specialRequests')?.value,
      id: 'RES' + Date.now()
    };

    // Save to localStorage
    let reservations = JSON.parse(localStorage.getItem('restaurantReservations')) || [];
    reservations.push(formData);
    localStorage.setItem('restaurantReservations', JSON.stringify(reservations));

    // Show success message
    alert('Reservation booked successfully! Your reservation ID is: ' + formData.id);

    // Reset form
    this.reset();
  });
}

// ============================================
// CONTACT FORM HANDLING
// ============================================

const contactForm = document.getElementById('contactForm');
if (contactForm) {
  contactForm.addEventListener('submit', function(e) {
    e.preventDefault();

    if (!validateForm('contactForm')) {
      return;
    }

    const message = {
      name: document.getElementById('contact-name')?.value,
      email: document.getElementById('contact-email')?.value,
      subject: document.getElementById('contact-subject')?.value,
      message: document.getElementById('contact-message')?.value,
      date: new Date().toLocaleString()
    };

    // Save to localStorage
    let messages = JSON.parse(localStorage.getItem('restaurantMessages')) || [];
    messages.push(message);
    localStorage.setItem('restaurantMessages', JSON.stringify(messages));

    alert('Thank you for your message! We will get back to you soon.');
    this.reset();
  });
}

// ============================================
// REVIEW FORM HANDLING
// ============================================

const reviewForm = document.getElementById('reviewForm');
if (reviewForm) {
  reviewForm.addEventListener('submit', function(e) {
    e.preventDefault();

    if (!validateForm('reviewForm')) {
      return;
    }

    const rating = document.getElementById('rating')?.value || 5;
    const review = {
      author: document.getElementById('reviewer-name')?.value,
      email: document.getElementById('reviewer-email')?.value,
      rating: parseInt(rating),
      text: document.getElementById('review-text')?.value,
      date: new Date().toLocaleDateString(),
      id: 'REV' + Date.now()
    };

    // Save to localStorage
    let reviews = JSON.parse(localStorage.getItem('restaurantReviews')) || [];
    reviews.unshift(review);
    localStorage.setItem('restaurantReviews', JSON.stringify(reviews));

    alert('Thank you for your review!');
    this.reset();

    // Reload reviews display if on review page
    if (typeof displayReviews === 'function') {
      displayReviews();
    }
  });
}

// ============================================
// MENU FILTER
// ============================================

function filterMenuItems(category) {
  const items = document.querySelectorAll('.menu-item');

  items.forEach(item => {
    if (category === 'all' || item.dataset.category === category) {
      item.style.display = 'block';
      setTimeout(() => item.style.opacity = '1', 10);
    } else {
      item.style.opacity = '0';
      setTimeout(() => item.style.display = 'none', 300);
    }
  });

  // Update active filter button
  document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.classList.remove('active');
  });
  event.target.classList.add('active');
}

// ============================================
// RESERVATION HISTORY
// ============================================

function displayReservationHistory() {
  const container = document.getElementById('reservationHistory');
  if (!container) return;

  const reservations = JSON.parse(localStorage.getItem('restaurantReservations')) || [];

  if (reservations.length === 0) {
    container.innerHTML = '<p class="text-center">No reservations found.</p>';
    return;
  }

  let html = '<div class="grid grid-2">';

  reservations.forEach(res => {
    html += `
      <div class="card">
        <h3>Reservation ${res.id}</h3>
        <p><strong>Name:</strong> ${res.name}</p>
        <p><strong>Email:</strong> ${res.email}</p>
        <p><strong>Phone:</strong> ${res.phone}</p>
        <p><strong>Date:</strong> ${new Date(res.date).toLocaleDateString()}</p>
        <p><strong>Time:</strong> ${res.time}</p>
        <p><strong>Guests:</strong> ${res.guests}</p>
        ${res.specialRequests ? `<p><strong>Special Requests:</strong> ${res.specialRequests}</p>` : ''}
        <button class="btn btn-danger" onclick="cancelReservation('${res.id}')">Cancel</button>
      </div>
    `;
  });

  html += '</div>';
  container.innerHTML = html;
}

function cancelReservation(resId) {
  if (!confirm('Are you sure you want to cancel this reservation?')) return;

  let reservations = JSON.parse(localStorage.getItem('restaurantReservations')) || [];
  reservations = reservations.filter(res => res.id !== resId);
  localStorage.setItem('restaurantReservations', JSON.stringify(reservations));

  displayReservationHistory();
  alert('Reservation cancelled.');
}

// ============================================
// REVIEW DISPLAY
// ============================================

function displayReviews() {
  const container = document.getElementById('reviewsList');
  if (!container) return;

  const reviews = JSON.parse(localStorage.getItem('restaurantReviews')) || [];

  if (reviews.length === 0) {
    container.innerHTML = '<p class="text-center">No reviews yet. Be the first to review!</p>';
    return;
  }

  let html = '<div class="grid grid-2">';

  reviews.forEach(review => {
    const stars = '★'.repeat(review.rating) + '☆'.repeat(5 - review.rating);
    html += `
      <div class="testimonial-card">
        <div class="stars">
          ${[...Array(5)].map((_, i) => `<span class="star ${i < review.rating ? '' : 'empty'}">★</span>`).join('')}
        </div>
        <p class="testimonial-text">"${review.text}"</p>
        <p class="testimonial-author">- ${review.author}</p>
        <p style="font-size: 0.85rem; color: #999; margin: 0;">${review.date}</p>
      </div>
    `;
  });

  html += '</div>';
  container.innerHTML = html;
}

// ============================================
// QUANTITY SELECTOR
// ============================================

function updateQuantity(id, change) {
  const input = document.getElementById('qty-' + id);
  if (!input) return;

  let value = parseInt(input.value) || 1;
  value += change;

  if (value < 1) value = 1;
  if (value > 99) value = 99;

  input.value = value;
}

// ============================================
// SMOOTH SCROLL
// ============================================

document.querySelectorAll('a[href^="#"]').forEach(anchor => {
  anchor.addEventListener('click', function (e) {
    const href = this.getAttribute('href');
    if (href !== '#') {
      e.preventDefault();
      const target = document.querySelector(href);
      if (target) {
        target.scrollIntoView({ behavior: 'smooth' });
      }
    }
  });
});

// ============================================
// INITIALIZE ON PAGE LOAD
// ============================================

document.addEventListener('DOMContentLoaded', function() {
  // Display reservation history if on that page
  if (document.getElementById('reservationHistory')) {
    displayReservationHistory();
  }

  // Display reviews if on review page
  if (document.getElementById('reviewsList')) {
    displayReviews();
  }

  // Set minimum date for date input to today
  const dateInputs = document.querySelectorAll('input[type="date"]');
  const today = new Date().toISOString().split('T')[0];
  dateInputs.forEach(input => {
    input.setAttribute('min', today);
  });

  // Add animation on scroll
  const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -100px 0px'
  };

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.style.animation = 'fadeIn 0.6s ease forwards';
        observer.unobserve(entry.target);
      }
    });
  }, observerOptions);

  document.querySelectorAll('.card, .testimonial-card, section').forEach(el => {
    el.style.opacity = '0';
    observer.observe(el);
  });
});

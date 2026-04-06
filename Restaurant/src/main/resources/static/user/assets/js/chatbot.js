/* ============================================
   RestaurantOS - Floating Chatbot Widget
   ============================================ */

(function () {
  'use strict';

  /* ---- Configuration ---- */
  var CHAT_API_URL = '/api/ai/chat';
  var STORAGE_KEY  = 'ros_chat_history';
  var MAX_HISTORY  = 30;

  var QUICK_REPLIES = [
    'View menu 🍽️',
    'Make a reservation 📅',
    'Opening hours 🕐',
    'Contact info 📞'
  ];

  var WELCOME_MSG =
    'Hello! 👋 I\'m the RestaurantOS assistant. How can I help you today?\n' +
    'You can ask about our menu, reservations, or anything else!';

  /* ---- State ---- */
  var isOpen       = false;
  var isTyping     = false;
  var hasNewMsg    = false;
  var msgHistory   = [];

  /* ---- DOM References ---- */
  var panel, toggleBtn, messagesEl, inputEl, sendBtn,
      typingEl, badgeEl, quickRepliesEl;

  /* ---- Init ---- */
  function init() {
    injectStyles();
    buildDOM();
    bindEvents();
    loadHistory();

    if (msgHistory.length === 0) {
      addBotMessage(WELCOME_MSG, false);
    } else {
      restoreHistory();
    }

    showQuickReplies();
  }

  /* ---- Inject global-style.css if not already loaded ---- */
  function injectStyles() {
    var base = getBasePath();
    var href = base + 'assets/css/global-style.css';
    var links = document.querySelectorAll('link[rel="stylesheet"]');
    for (var i = 0; i < links.length; i++) {
      if (links[i].href.indexOf('global-style.css') !== -1) return;
    }
    var link = document.createElement('link');
    link.rel  = 'stylesheet';
    link.href = href;
    document.head.appendChild(link);
  }

  /* ---- Determine base path for assets ---- */
  function getBasePath() {
    var scripts = document.querySelectorAll('script[src]');
    for (var i = 0; i < scripts.length; i++) {
      var src = scripts[i].getAttribute('src') || '';
      if (src.indexOf('chatbot.js') !== -1) {
        return src.replace(/assets\/js\/chatbot\.js.*$/, '');
      }
    }
    return '/user/';
  }

  /* ---- Build DOM ---- */
  function buildDOM() {
    /* Toggle button */
    toggleBtn = document.createElement('button');
    toggleBtn.className    = 'chatbot-toggle';
    toggleBtn.setAttribute('aria-label', 'Open chat support');
    toggleBtn.setAttribute('title', 'Chat with us');
    toggleBtn.innerHTML    =
      '<span class="chatbot-toggle-icon">💬</span>' +
      '<span class="chatbot-badge gs-hidden">1</span>';
    badgeEl = toggleBtn.querySelector('.chatbot-badge');

    /* Panel */
    panel = document.createElement('div');
    panel.className = 'chatbot-panel';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Customer support chat');
    panel.innerHTML =
      /* Header */
      '<div class="chatbot-header">' +
        '<div class="chatbot-header-avatar">🤖</div>' +
        '<div class="chatbot-header-info">' +
          '<div class="chatbot-header-name">RestaurantOS Assistant</div>' +
          '<div class="chatbot-header-status">Online now</div>' +
        '</div>' +
        '<button class="chatbot-close-btn" aria-label="Close chat">✕</button>' +
      '</div>' +
      /* Messages */
      '<div class="chatbot-messages" id="chatbotMessages"></div>' +
      /* Quick Replies */
      '<div class="chatbot-quick-replies" id="chatbotQuickReplies"></div>' +
      /* Input */
      '<div class="chatbot-input-area">' +
        '<input class="chatbot-input" id="chatbotInput"' +
          ' type="text" placeholder="Type a message..." autocomplete="off"' +
          ' aria-label="Chat message" />' +
        '<button class="chatbot-send-btn" id="chatbotSendBtn" aria-label="Send message">' +
          '➤' +
        '</button>' +
      '</div>';

    document.body.appendChild(toggleBtn);
    document.body.appendChild(panel);

    /* Cache sub-elements */
    messagesEl     = panel.querySelector('#chatbotMessages');
    inputEl        = panel.querySelector('#chatbotInput');
    sendBtn        = panel.querySelector('#chatbotSendBtn');
    quickRepliesEl = panel.querySelector('#chatbotQuickReplies');
  }

  /* ---- Bind Events ---- */
  function bindEvents() {
    toggleBtn.addEventListener('click', togglePanel);
    panel.querySelector('.chatbot-close-btn').addEventListener('click', closePanel);

    sendBtn.addEventListener('click', handleSend);
    inputEl.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
    });

    /* Close when clicking outside */
    document.addEventListener('click', function (e) {
      if (isOpen && !panel.contains(e.target) && !toggleBtn.contains(e.target)) {
        closePanel();
      }
    });
  }

  /* ---- Toggle Panel ---- */
  function togglePanel() {
    if (isOpen) {
      closePanel();
    } else {
      openPanel();
    }
  }

  function openPanel() {
    isOpen = true;
    panel.classList.add('open');
    toggleBtn.querySelector('.chatbot-toggle-icon').textContent = '✕';
    toggleBtn.setAttribute('aria-label', 'Close chat support');
    hideBadge();
    scrollToBottom();
    setTimeout(function () { inputEl.focus(); }, 300);
  }

  function closePanel() {
    isOpen = false;
    panel.classList.remove('open');
    toggleBtn.querySelector('.chatbot-toggle-icon').textContent = '💬';
    toggleBtn.setAttribute('aria-label', 'Open chat support');
  }

  /* ---- Badge ---- */
  function showBadge() {
    if (!isOpen) {
      badgeEl.classList.remove('gs-hidden');
      hasNewMsg = true;
    }
  }

  function hideBadge() {
    badgeEl.classList.add('gs-hidden');
    hasNewMsg = false;
  }

  /* ---- Message Handling ---- */
  function handleSend() {
    var text = inputEl.value.trim();
    if (!text || isTyping) return;
    sendMessage(text);
    inputEl.value = '';
  }

  function sendMessage(text) {
    addUserMessage(text);
    hideQuickReplies();
    setTyping(true);

    fetch(CHAT_API_URL, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ message: text })
    })
    .then(function (res) {
      if (!res.ok) throw new Error('HTTP ' + res.status);
      return res.json();
    })
    .then(function (data) {
      setTyping(false);
      var reply = (data && data.reply) ? data.reply : 'Sorry, I could not get a response.';
      addBotMessage(reply, true);
    })
    .catch(function () {
      setTyping(false);
      addBotMessage('Sorry, there was a connection issue. Please try again later.', true);
    });
  }

  function addUserMessage(text) {
    var msg = { role: 'user', text: text, time: Date.now() };
    msgHistory.push(msg);
    saveHistory();
    renderMessage(msg);
    scrollToBottom();
  }

  function addBotMessage(text, notify) {
    var msg = { role: 'bot', text: text, time: Date.now() };
    msgHistory.push(msg);
    saveHistory();
    renderMessage(msg);
    scrollToBottom();
    if (notify) showBadge();
  }

  function renderMessage(msg) {
    var wrapper = document.createElement('div');
    wrapper.className = 'chatbot-msg ' + msg.role;

    var avatar = document.createElement('div');
    avatar.className = 'chatbot-msg-avatar';
    avatar.textContent = msg.role === 'bot' ? '🤖' : '👤';

    var bubble = document.createElement('div');
    bubble.className = 'chatbot-msg-bubble';
    bubble.textContent = msg.text;

    wrapper.appendChild(avatar);
    wrapper.appendChild(bubble);
    messagesEl.appendChild(wrapper);
  }

  /* ---- Typing Indicator ---- */
  function setTyping(active) {
    isTyping = active;
    sendBtn.disabled = active;

    if (active) {
      if (!typingEl) {
        var wrapper = document.createElement('div');
        wrapper.className = 'chatbot-msg bot';
        wrapper.id = 'chatbotTyping';

        var avatar = document.createElement('div');
        avatar.className = 'chatbot-msg-avatar';
        avatar.textContent = '🤖';

        typingEl = document.createElement('div');
        typingEl.className = 'chatbot-typing';
        typingEl.innerHTML = '<span></span><span></span><span></span>';

        wrapper.appendChild(avatar);
        wrapper.appendChild(typingEl);
        messagesEl.appendChild(wrapper);
        typingEl = wrapper;
      }
    } else {
      if (typingEl && typingEl.parentNode) {
        typingEl.parentNode.removeChild(typingEl);
      }
      typingEl = null;
    }

    scrollToBottom();
  }

  /* ---- Quick Replies ---- */
  function showQuickReplies() {
    quickRepliesEl.innerHTML = '';
    QUICK_REPLIES.forEach(function (label) {
      var btn = document.createElement('button');
      btn.className = 'chatbot-quick-btn';
      btn.textContent = label;
      btn.addEventListener('click', function () {
        sendMessage(label);
      });
      quickRepliesEl.appendChild(btn);
    });
    quickRepliesEl.style.display = 'flex';
  }

  function hideQuickReplies() {
    quickRepliesEl.style.display = 'none';
  }

  /* ---- History ---- */
  function saveHistory() {
    if (msgHistory.length > MAX_HISTORY) {
      msgHistory = msgHistory.slice(-MAX_HISTORY);
    }
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(msgHistory));
    } catch (e) { /* ignore */ }
  }

  function loadHistory() {
    try {
      var raw = sessionStorage.getItem(STORAGE_KEY);
      if (raw) msgHistory = JSON.parse(raw) || [];
    } catch (e) { msgHistory = []; }
  }

  function restoreHistory() {
    msgHistory.forEach(function (msg) {
      renderMessage(msg);
    });
    scrollToBottom();
  }

  /* ---- Helpers ---- */
  function scrollToBottom() {
    if (messagesEl) {
      messagesEl.scrollTop = messagesEl.scrollHeight;
    }
  }

  /* ---- Bootstrap ---- */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

})();

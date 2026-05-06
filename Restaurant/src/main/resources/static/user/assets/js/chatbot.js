// Chatbot functionality

function toggleChatbot() {
  const chatbotBody = document.getElementById('chatbot-body');
  const chatbotInput = document.querySelector('.chatbot-input-area');
  const toggleBtn = document.getElementById('chatbot-toggle');

  if (!chatbotBody || !chatbotInput || !toggleBtn) return;

  if (chatbotBody.style.display === 'none' || chatbotBody.style.display === '') {
    chatbotBody.style.display = 'flex';
    chatbotInput.style.display = 'flex';
    toggleBtn.textContent = '−';
  } else {
    chatbotBody.style.display = 'none';
    chatbotInput.style.display = 'none';
    toggleBtn.textContent = '+';
  }
}

async function sendChatMessage() {
  const input = document.getElementById('chatbot-input');
  const message = (input?.value || '').trim();
  if (!message) return;

  const sessionId = localStorage.getItem("chatSessionId") || crypto.randomUUID();
  localStorage.setItem("chatSessionId", sessionId);

  // Add user message
  addChatMessage(message, 'user');

  // Clear input + disable input while waiting
  input.value = '';
  input.disabled = true;

  try {
    const res = await fetch('/api/chat/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ message, sessionId }) // <-- GỬI CẢ sessionId
    });

    const contentType = res.headers.get('content-type') || '';
    const data = contentType.includes('application/json') ? await res.json() : null;
    const text = !data ? await res.text() : null;

    if (!res.ok) {
      const errMsg =
        data?.reply ||
        data?.message ||
        text ||
        `Chat failed (HTTP ${res.status})`;
      addChatMessage(errMsg, 'bot');
      return;
    }

    const reply = data?.reply || text || 'Mình chưa có phản hồi, bạn thử lại giúp mình nhé.';
    addChatMessage(reply, 'bot');

    // (Optional) nếu bạn muốn debug flow:
    // console.log("nextField:", data?.nextField, "done:", data?.done, "reservationId:", data?.reservationId);

  } catch (e) {
    console.error('Chatbot error:', e);
    addChatMessage('Không kết nối được tới server. Bạn thử lại sau nhé.', 'bot');
  } finally {
    input.disabled = false;
    input.focus();
  }
}

function addChatMessage(message, sender) {
  const messagesContainer = document.getElementById('chat-messages');
  if (!messagesContainer) return;

  const messageElement = document.createElement('div');
  messageElement.className = `chat-message ${sender}-message`;

  // Tránh XSS: không dùng innerHTML với nội dung từ user/AI
  const bubble = document.createElement('div');
  bubble.className = 'message-bubble';
  bubble.textContent = message;

  messageElement.appendChild(bubble);
  messagesContainer.appendChild(messageElement);

  // Scroll to bottom
  const chatbotBody = document.getElementById('chatbot-body');
  if (chatbotBody) chatbotBody.scrollTop = chatbotBody.scrollHeight;
}

function handleChatInput(event) {
  if (event.key === 'Enter') {
    event.preventDefault();
    sendChatMessage();
  }
}
// Chatbot functionality

function toggleChatbot() {
  const chatbotBody = document.getElementById('chatbot-body');
  const chatbotInput = document.querySelector('.chatbot-input-area');
  const toggleBtn = document.getElementById('chatbot-toggle');

  if (chatbotBody.style.display === 'none') {
    chatbotBody.style.display = 'flex';
    chatbotInput.style.display = 'flex';
    toggleBtn.textContent = '−';
  } else {
    chatbotBody.style.display = 'none';
    chatbotInput.style.display = 'none';
    toggleBtn.textContent = '+';
  }
}

function sendChatMessage() {
  const input = document.getElementById('chatbot-input');
  const message = input.value.trim();

  if (message === '') return;

  // Add user message
  addChatMessage(message, 'user');

  // Clear input
  input.value = '';

  // Simulate bot response (replace with real API later)
  setTimeout(() => {
    const responses = [
      '👍 Cảm ơn bạn! Chúng tôi sẽ xử lý yêu cầu của bạn.',
      '✅ Đã ghi nhận. Vui lòng liên hệ với bộ phận hỗ trợ.',
      '📞 Bạn có thể gọi cho chúng tôi 24/7 để được hỗ trợ tốt hơn.',
      '🍽️ Đặc biệt hôm nay có các món ăn mới. Bạn có quan tâm không?',
      '💳 Chúng tôi chấp nhận tất cả các hình thức thanh toán phổ biến.'
    ];

    const randomResponse = responses[Math.floor(Math.random() * responses.length)];
    addChatMessage(randomResponse, 'bot');
  }, 500);
}

function addChatMessage(message, sender) {
  const messagesContainer = document.getElementById('chat-messages');

  const messageElement = document.createElement('div');
  messageElement.className = `chat-message ${sender}-message`;
  messageElement.innerHTML = `<div class="message-bubble">${message}</div>`;

  messagesContainer.appendChild(messageElement);

  // Scroll to bottom
  const chatbotBody = document.getElementById('chatbot-body');
  chatbotBody.scrollTop = chatbotBody.scrollHeight;
}

function handleChatInput(event) {
  if (event.key === 'Enter') {
    sendChatMessage();
  }
}
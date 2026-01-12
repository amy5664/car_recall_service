document.addEventListener("DOMContentLoaded", function () {

    // --- API 경로 통일 ---
    const API_SEND = "/api/support";              
    const API_STREAM = "/api/support/stream";     // ← 여기가 핵심 수정!!

    // --- 상담원 채팅 버튼 ---
    const chatButton = document.createElement("button");
    chatButton.innerText = "💬 상담원창";
    chatButton.style.position = "fixed";
    chatButton.style.bottom = "24px";
    chatButton.style.right = "24px";
    chatButton.style.width = "140px";
    chatButton.style.height = "50px";
    chatButton.style.borderRadius = "12px";
    chatButton.style.background = "linear-gradient(145deg, #6366F1, #4F46E5)";
    chatButton.style.color = "white";
    chatButton.style.fontSize = "16px";
    chatButton.style.border = "none";
    chatButton.style.cursor = "pointer";
    chatButton.style.zIndex = "1000";
    document.body.appendChild(chatButton);

    // --- 채팅창 ---
    const chatWindow = document.createElement("div");
    chatWindow.style.position = "fixed";
    chatWindow.style.bottom = "100px";
    chatWindow.style.right = "24px";
    chatWindow.style.width = "360px";
    chatWindow.style.height = "520px";
    chatWindow.style.background = "#1E1E2F";
    chatWindow.style.border = "1px solid #2D2D3A";
    chatWindow.style.borderRadius = "16px";
    chatWindow.style.display = "none";
    chatWindow.style.flexDirection = "column";
    chatWindow.style.overflow = "hidden";
    chatWindow.style.boxShadow = "0 10px 25px rgba(0,0,0,0.5)";
    chatWindow.style.zIndex = "1000";
    chatWindow.style.color = "#E4E4E7";
    chatWindow.style.fontFamily = "Inter, Pretendard, sans-serif";
    document.body.appendChild(chatWindow);

    // --- 헤더 ---
    const header = document.createElement("div");
    header.style.background = "#2A2A3C";
    header.style.color = "#E4E4E7";
    header.style.padding = "12px 16px";
    header.style.display = "flex";
    header.style.justifyContent = "space-between";
    header.style.alignItems = "center";

    const titleSpan = document.createElement("span");
    titleSpan.innerText = "고객 상담 채팅";

    const closeBtn = document.createElement("button");
    closeBtn.innerText = "✕";
    closeBtn.style.background = "none";
    closeBtn.style.border = "none";
    closeBtn.style.color = "#E4E4E7";
    closeBtn.style.fontSize = "20px";
    closeBtn.style.cursor = "pointer";

    header.appendChild(titleSpan);
    header.appendChild(closeBtn);
    chatWindow.appendChild(header);

    // --- 메시지 영역 ---
    const messageArea = document.createElement("div");
    messageArea.style.flex = "1";
    messageArea.style.padding = "12px";
    messageArea.style.overflowY = "auto";
    chatWindow.appendChild(messageArea);

    // --- 입력창 ---
    const inputBox = document.createElement("div");
    inputBox.style.display = "flex";
    inputBox.style.borderTop = "1px solid #2D2D3A";
    inputBox.style.background = "#2A2A3C";
    inputBox.innerHTML = `
        <input type="text" placeholder="고객에게 보낼 메시지"
               style="flex:1;padding:10px 12px;background:transparent;color:#E4E4E7;
                      border:none;outline:none;font-size:14px;">
        <button style="background:#6366F1;color:white;border:none;padding:10px 16px;
                       cursor:pointer;font-weight:500;">전송</button>
    `;
    chatWindow.appendChild(inputBox);

    const sendBtn = inputBox.querySelector("button");
    const inputField = inputBox.querySelector("input");

    // --- 말풍선 출력 ---
    function appendMessage(role, text) {
        const msg = document.createElement("div");
        msg.style.margin = "8px 0";
        msg.style.display = "flex";
        msg.style.justifyContent = role === "agent" ? "flex-end" : "flex-start";

        const bubble = document.createElement("div");
        bubble.style.padding = "10px 14px";
        bubble.style.borderRadius = "16px";
        bubble.style.maxWidth = "75%";
        bubble.style.fontSize = "14px";
        bubble.style.lineHeight = "1.5";

        if (role === "agent") {
            bubble.style.background = "#6366F1";
            bubble.style.color = "white";
        } else {
            bubble.style.background = "#2D2D3A";
            bubble.style.color = "#E4E4E7";
        }

        bubble.innerHTML = text.replace(/\n/g, "<br>");

        msg.appendChild(bubble);
        messageArea.appendChild(msg);
        messageArea.scrollTop = messageArea.scrollHeight;
    }

    // --- 상담원 메시지 전송 ---
    async function sendMessage() {
        const msg = inputField.value.trim();
        if (!msg) return;

        appendMessage("agent", msg);
        inputField.value = "";

        try {
            await fetch(API_SEND, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    sender: "agent",
                    msg: msg
                })
            });
        } catch (e) {
            appendMessage("system", "❌ 전송 실패");
        }
    }

    // --- SSE 메시지 수신 ---
    function startSSE() {
        const eventSource = new EventSource(API_STREAM);

        eventSource.onmessage = function (event) {
            const data = JSON.parse(event.data);

            if (data.sender === "customer") {
                appendMessage("customer", data.msg);
            }
        };

        eventSource.onerror = function () {
            console.log("❌ SSE 연결 끊김 → 재연결 시도중…");
        };
    }

    // --- 버튼 이벤트 ---
    chatButton.addEventListener("click", () => {
        chatWindow.style.display = "flex";
        chatButton.style.display = "none";

        appendMessage("system", "👉 상담원 채팅창이 열렸습니다.");
        startSSE();
    });

    closeBtn.addEventListener("click", () => {
        chatWindow.style.display = "none";
        chatButton.style.display = "block";
    });

    sendBtn.addEventListener("click", sendMessage);
    inputField.addEventListener("keydown", (e) => {
        if (e.key === "Enter") sendMessage();
    });

});

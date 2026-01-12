document.addEventListener("DOMContentLoaded", function () {
    // ===== WebSocket 기반 통합 채팅 =====
    // main.css 색상 스킴 적용
    
    let customerSessionId = null;
    let webSocket = null;
    let chatMode = "gpt";
    let isConsultationEnded = false;  // 상담 종료 상태 추적
    
    const PRIMARY_COLOR = "#8ECFFB"; // pastel sky blue
    const SECONDARY_COLOR = "#BEE7FF"; // lighter sky tone
    const TEXT_COLOR = "#0b1020";
    const LIGHT_BG = "#f7f9fc";
    const CARD_BG = "#ffffff";
    const BORDER_COLOR = "#e6e8ee";
    const MUTED_TEXT = "#6b7280";
    
    // --- 채팅 버튼 (플로팅 버튼) ---
    const chatButton = document.createElement("button");
    chatButton.innerText = "💬";
    chatButton.style.position = "fixed";
    chatButton.style.bottom = "24px";
    chatButton.style.right = "24px";
    chatButton.style.width = "64px";
    chatButton.style.height = "64px";
    chatButton.style.borderRadius = "50%";
    chatButton.style.background = `linear-gradient(135deg, ${PRIMARY_COLOR}, ${SECONDARY_COLOR})`;
    chatButton.style.color = "white";
    chatButton.style.fontSize = "26px";
    chatButton.style.border = "none";
    chatButton.style.cursor = "pointer";
    chatButton.style.zIndex = "1000";
    chatButton.style.boxShadow = "0 8px 20px rgba(142, 207, 251, 0.35)";
    chatButton.style.transition = "all 0.3s ease";
    chatButton.addEventListener("mouseenter", () => {
        chatButton.style.transform = "scale(1.1)";
        chatButton.style.boxShadow = "0 12px 24px rgba(142, 207, 251, 0.45)";
    });
    chatButton.addEventListener("mouseleave", () => {
        chatButton.style.transform = "scale(1)";
        chatButton.style.boxShadow = "0 8px 20px rgba(142, 207, 251, 0.35)";
    });
    document.body.appendChild(chatButton);

    // --- 채팅창 ---
    const chatWindow = document.createElement("div");
    chatWindow.style.position = "fixed";
    chatWindow.style.bottom = "100px";
    chatWindow.style.right = "24px";
    chatWindow.style.width = "360px";
    chatWindow.style.height = "520px";
    chatWindow.style.background = LIGHT_BG;
    chatWindow.style.border = `1px solid ${BORDER_COLOR}`;
    chatWindow.style.borderRadius = "14px";
    chatWindow.style.display = "none";
    chatWindow.style.flexDirection = "column";
    chatWindow.style.overflow = "hidden";
    chatWindow.style.boxShadow = "0 8px 20px rgba(16, 33, 80, 0.08)";
    chatWindow.style.zIndex = "1000";
    chatWindow.style.color = TEXT_COLOR;
    chatWindow.style.fontFamily = '"Noto Sans KR", sans-serif';
    document.body.appendChild(chatWindow);

    // --- 헤더 ---
    const header = document.createElement("div");
    header.style.background = `linear-gradient(to right, ${PRIMARY_COLOR}, ${SECONDARY_COLOR})`;
    header.style.color = "white";
    header.style.padding = "16px 20px";
    header.style.display = "flex";
    header.style.justifyContent = "space-between";
    header.style.alignItems = "center";

    const titleSpan = document.createElement("span");
    titleSpan.innerText = "실시간 상담 채팅";
    titleSpan.style.fontWeight = "600";
    titleSpan.style.fontSize = "16px";

    const buttonContainer = document.createElement("div");
    buttonContainer.style.display = "flex";
    buttonContainer.style.gap = "8px";
    buttonContainer.style.alignItems = "center";

    const agentButton = document.createElement("button");
    agentButton.innerText = "상담사 연결";
    agentButton.id = "agent-request-btn";
    agentButton.style.background = "rgba(255,255,255,0.2)";
    agentButton.style.border = "1px solid rgba(255,255,255,0.4)";
    agentButton.style.color = "white";
    agentButton.style.padding = "6px 12px";
    agentButton.style.cursor = "pointer";
    agentButton.style.borderRadius = "6px";
    agentButton.style.fontSize = "12px";
    agentButton.style.transition = "all 0.3s ease";
    agentButton.style.fontFamily = '"Noto Sans KR", sans-serif';
    agentButton.addEventListener("mouseenter", () => {
        agentButton.style.background = "rgba(255,255,255,0.3)";
    });
    agentButton.addEventListener("mouseleave", () => {
        agentButton.style.background = "rgba(255,255,255,0.2)";
    });

    const endChatButton = document.createElement("button");
    endChatButton.innerText = "상담 종료";
    endChatButton.id = "end-chat-btn";
    endChatButton.style.background = "rgba(239, 68, 68, 0.3)";
    endChatButton.style.border = "1px solid rgba(239, 68, 68, 0.5)";
    endChatButton.style.color = "white";
    endChatButton.style.padding = "6px 12px";
    endChatButton.style.cursor = "pointer";
    endChatButton.style.borderRadius = "6px";
    endChatButton.style.fontSize = "12px";
    endChatButton.style.transition = "all 0.3s ease";
    endChatButton.style.fontFamily = '"Noto Sans KR", sans-serif';
    endChatButton.style.display = "none";
    endChatButton.addEventListener("mouseenter", () => {
        endChatButton.style.background = "rgba(239, 68, 68, 0.4)";
    });
    endChatButton.addEventListener("mouseleave", () => {
        endChatButton.style.background = "rgba(239, 68, 68, 0.3)";
    });

    const closeBtn = document.createElement("button");
    closeBtn.innerText = "✕";
    closeBtn.style.background = "none";
    closeBtn.style.border = "none";
    closeBtn.style.color = "white";
    closeBtn.style.fontSize = "20px";
    closeBtn.style.cursor = "pointer";
    closeBtn.style.padding = "0";
    closeBtn.style.width = "24px";
    closeBtn.style.height = "24px";

    header.appendChild(titleSpan);
    buttonContainer.appendChild(agentButton);
    buttonContainer.appendChild(endChatButton);
    header.appendChild(buttonContainer);
    header.appendChild(closeBtn);
    chatWindow.appendChild(header);

    // --- 메시지 영역 ---
    const messageArea = document.createElement("div");
    messageArea.style.flex = "1";
    messageArea.style.padding = "16px";
    messageArea.style.overflowY = "auto";
    messageArea.style.overflowX = "hidden";
    messageArea.style.display = "flex";
    messageArea.style.flexDirection = "column";
    messageArea.style.gap = "8px";
    messageArea.style.background = CARD_BG;
    messageArea.id = "messageArea";
    chatWindow.appendChild(messageArea);

    // --- 입력창 ---
    const inputBox = document.createElement("div");
    inputBox.style.display = "flex";
    inputBox.style.borderTop = `1px solid ${BORDER_COLOR}`;
    inputBox.style.background = CARD_BG;
    inputBox.style.padding = "12px";
    inputBox.style.gap = "8px";
    
    const inputField = document.createElement("input");
    inputField.type = "text";
    inputField.placeholder = "메시지를 입력하세요";
    inputField.style.flex = "1";
    inputField.style.padding = "10px 12px";
    inputField.style.background = LIGHT_BG;
    inputField.style.color = TEXT_COLOR;
    inputField.style.border = `1px solid ${BORDER_COLOR}`;
    inputField.style.outline = "none";
    inputField.style.fontSize = "14px";
    inputField.style.borderRadius = "6px";
    inputField.style.fontFamily = '"Noto Sans KR", sans-serif';
    inputField.style.transition = "border-color 0.3s ease";
    inputField.addEventListener("focus", () => {
        inputField.style.borderColor = PRIMARY_COLOR;
    });
    inputField.addEventListener("blur", () => {
        inputField.style.borderColor = BORDER_COLOR;
    });
    
    const sendBtn = document.createElement("button");
    sendBtn.innerText = "전송";
    sendBtn.style.background = PRIMARY_COLOR;
    sendBtn.style.color = "white";
    sendBtn.style.border = "none";
    sendBtn.style.padding = "10px 16px";
    sendBtn.style.cursor = "pointer";
    sendBtn.style.fontWeight = "500";
    sendBtn.style.borderRadius = "6px";
    sendBtn.style.fontSize = "14px";
    sendBtn.style.fontFamily = '"Noto Sans KR", sans-serif';
    sendBtn.style.transition = "all 0.3s ease";
    sendBtn.addEventListener("mouseenter", () => {
        sendBtn.style.background = SECONDARY_COLOR;
        sendBtn.style.transform = "translateY(-2px)";
        sendBtn.style.boxShadow = "0 4px 12px rgba(142, 207, 251, 0.35)";
    });
    sendBtn.addEventListener("mouseleave", () => {
        sendBtn.style.background = PRIMARY_COLOR;
        sendBtn.style.transform = "translateY(0)";
        sendBtn.style.boxShadow = "none";
    });

    inputBox.appendChild(inputField);
    inputBox.appendChild(sendBtn);
    chatWindow.appendChild(inputBox);

    // --- 메시지 출력 함수 ---
    function appendMessage(role, text, timestamp = null) {
        const msg = document.createElement("div");
        msg.style.display = "flex";
        msg.style.justifyContent = role === "user" ? "flex-end" : "flex-start";
        msg.style.alignItems = "flex-end";
        msg.style.animation = "slideIn 0.3s ease";
        msg.style.gap = "8px";

        const bubble = document.createElement("div");
        bubble.style.padding = "12px 14px";
        bubble.style.borderRadius = "12px";
        bubble.style.maxWidth = "70%";
        bubble.style.fontSize = "14px";
        bubble.style.lineHeight = "1.6";
        bubble.style.wordWrap = "break-word";
        bubble.style.whiteSpace = "pre-wrap";

        if (role === "user") {
            bubble.style.background = PRIMARY_COLOR;
            bubble.style.color = "white";
            bubble.style.borderBottomRightRadius = "4px";
        } else if (role === "system") {
            bubble.style.background = LIGHT_BG;
            bubble.style.color = MUTED_TEXT;
            bubble.style.fontSize = "13px";
            bubble.style.textAlign = "center";
            bubble.style.width = "100%";
        } else {
            bubble.style.background = CARD_BG;
            bubble.style.color = TEXT_COLOR;
            bubble.style.border = `1px solid ${BORDER_COLOR}`;
            bubble.style.boxShadow = "0 4px 10px rgba(16,33,80,0.04)";
            bubble.style.borderBottomLeftRadius = "4px";
        }

        bubble.innerHTML = text.replace(/\n/g, "<br>");
        msg.appendChild(bubble);
        messageArea.appendChild(msg);
        messageArea.scrollTop = messageArea.scrollHeight;
    }

    // --- WebSocket 연결 ---
    function connectWebSocket() {
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        const url = `${protocol}//${window.location.host}/ws/consultation`;
        
        try {
            webSocket = new WebSocket(url);
            
            webSocket.onopen = function(event) {
                console.log("WebSocket 연결 성공");
                appendMessage("system", "연결 완료. 안녕하세요! 😊 리콜 서비스 센터 입니다 도움이 필요하시면 도움이라고 보내주세요!");
            };
            
            webSocket.onmessage = function(event) {
                const message = JSON.parse(event.data);
                
                if (message.type === "SESSION_ID") {
                    customerSessionId = message.sessionId;
                    console.log("고객 세션 ID:", customerSessionId);
                } else if (message.type === "MESSAGE") {
                    appendMessage("assistant", message.message);
                } else if (message.type === "AGENT_WAITING") {
                    appendMessage("system", "상담사 연결을 요청했습니다. 잠시만 기다려주세요...");
                } else if (message.type === "AGENT_CONNECTED") {
                    chatMode = "agent";
                    titleSpan.innerText = "상담사 채팅";
                    agentButton.style.display = "none";
                    endChatButton.style.display = "block";
                    appendMessage("system", "상담사가 연결되었습니다!");
                } else if (message.type === "CONSULTATION_ENDED") {
                    // 상담 종료 상태 설정: 상담사는 더 이상 메시지 전송 불가
                    isConsultationEnded = true;
                    // 고객은 GPT와 계속 대화할 수 있도록 chatMode를 GPT로 전환하고 입력은 유지
                    chatMode = "gpt";
                    titleSpan.innerText = "실시간 상담 채팅";
                    agentButton.style.display = "block";
                    endChatButton.style.display = "none";
                    // 고객 입력은 허용(입력 비활성화 제거)
                    if (typeof inputField !== 'undefined') {
                        inputField.disabled = false;
                        inputField.style.opacity = "1";
                        inputField.style.cursor = "text";
                    }
                    if (typeof sendBtn !== 'undefined') {
                        sendBtn.disabled = false;
                        sendBtn.style.opacity = "1";
                        sendBtn.style.cursor = "pointer";
                    }
                    appendMessage("system", "상담사가 상담을 종료했습니다. 다시 상담을 요청할 수 있습니다.");
                }
            };
            
            webSocket.onerror = function(event) {
                console.error("WebSocket 오류:", event);
                appendMessage("system", "연결 오류가 발생했습니다. 😞");
            };
            
            webSocket.onclose = function(event) {
                console.log("WebSocket 연결 해제");
                chatMode = "gpt";
                titleSpan.innerText = "실시간 상담 채팅";
                agentButton.style.display = "block";
                endChatButton.style.display = "none";
            };
        } catch (e) {
            console.error("WebSocket 연결 실패:", e);
            appendMessage("system", "연결할 수 없습니다.");
        }
    }

    // --- 메시지 전송 ---
    async function sendMessage() {
        const msg = inputField.value.trim();
        if (!msg) return;
        if (isConsultationEnded && chatMode === "agent") {
            appendMessage("system", "상담이 종료되었습니다. 메시지를 전송할 수 없습니다.");
            return;
        }

        appendMessage("user", msg);
        inputField.value = "";

        // 모든 메시지는 WebSocket을 통해 전송
        if (webSocket && webSocket.readyState === WebSocket.OPEN) {
            const message = {
                type: "MESSAGE",
                sessionId: customerSessionId,
                message: msg
            };
            console.log("메시지 전송:", message);
            webSocket.send(JSON.stringify(message));
        } else {
            console.error("WebSocket이 연결되지 않음");
            appendMessage("system", "연결이 끊어졌습니다. 페이지를 새로고침해주세요.");
        }
    }

    // --- 상담사 연결 요청 ---
    agentButton.addEventListener("click", () => {
        if (webSocket && webSocket.readyState === WebSocket.OPEN) {
            const message = {
                type: "REQUEST_AGENT",
                sessionId: customerSessionId
            };
            webSocket.send(JSON.stringify(message));
            agentButton.disabled = true;
            isConsultationEnded = false;  // 새 상담 시작 시 상태 초기화
            inputField.disabled = false;  // 입력창 활성화
            sendBtn.disabled = false;
            inputField.style.opacity = "1";
            inputField.style.cursor = "text";
            sendBtn.style.opacity = "1";
            sendBtn.style.cursor = "pointer";
        }
    });

    // --- 상담 종료 ---
    endChatButton.addEventListener("click", () => {
        if (webSocket && webSocket.readyState === WebSocket.OPEN) {
            const message = {
                type: "END_CONSULTATION",
                sessionId: customerSessionId
            };
            webSocket.send(JSON.stringify(message));
        }
        chatMode = "gpt";
        titleSpan.innerText = "실시간 상담 채팅";
        agentButton.style.display = "block";
        agentButton.disabled = false;
        endChatButton.style.display = "none";
    })

    // --- 채팅 창 열기 ---
    chatButton.addEventListener("click", () => {
        chatWindow.style.display = "flex";
        chatButton.style.display = "none";
        
        if (!webSocket || webSocket.readyState === WebSocket.CLOSED) {
            connectWebSocket();
        }
    });

    // --- 채팅 창 닫기 ---
    closeBtn.addEventListener("click", () => {
        chatWindow.style.display = "none";
        chatButton.style.display = "block";
        
        if (webSocket && webSocket.readyState === WebSocket.OPEN) {
            const message = {
                type: "DISCONNECT",
                sessionId: customerSessionId
            };
            webSocket.send(JSON.stringify(message));
            webSocket.close();
            webSocket = null;
        }
    });

    // --- 메시지 전송 이벤트 ---
    sendBtn.addEventListener("click", sendMessage);
    inputField.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // CSS 애니메이션 추가
    const style = document.createElement("style");
    style.innerHTML = `
        @keyframes slideIn {
            from {
                opacity: 0;
                transform: translateY(10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        
        input:focus {
            border-color: ${PRIMARY_COLOR} !important;
        }
        
        /* 메시지 영역 스크롤바 스타일 */
        #messageArea::-webkit-scrollbar {
            width: 8px;
        }
        #messageArea::-webkit-scrollbar-track {
            background: ${LIGHT_BG};
            border-radius: 4px;
        }
        #messageArea::-webkit-scrollbar-thumb {
            background: ${BORDER_COLOR};
            border-radius: 4px;
        }
        #messageArea::-webkit-scrollbar-thumb:hover {
            background: ${MUTED_TEXT};
        }
        
        /* Firefox 스크롤바 */
        #messageArea {
            scrollbar-width: thin;
            scrollbar-color: ${BORDER_COLOR} ${LIGHT_BG};
        }
    `;
    document.head.appendChild(style);
});

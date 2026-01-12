/**
 * 관리자 상담 대시보드 - 상담사가 고객을 관리하고 채팅하는 페이지
 */
document.addEventListener("DOMContentLoaded", function() {
    // 현재 로그인한 상담사 정보 (서버에서 전달)
    const agentId = document.querySelector("[data-agent-id]")?.getAttribute("data-agent-id") || "agent-" + Date.now();
    const agentName = document.querySelector("[data-agent-name]")?.getAttribute("data-agent-name") || "상담사";
    
    let selectedCustomerSessionId = null;
    let agentWebSocket = null;

    // --- 상담사 WebSocket 연결 (실시간 수신) ---
    function connectAgentWebSocket() {
        try {
            const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
            const url = `${protocol}//${window.location.host}/ws/agent?agentId=${encodeURIComponent(agentId)}`;
            agentWebSocket = new WebSocket(url);

            agentWebSocket.onopen = () => {
                console.log("Agent WebSocket 연결됨");
            };

            agentWebSocket.onmessage = (evt) => {
                try {
                    const data = JSON.parse(evt.data);
                    // MessageNotificationDTO: { sessionId, senderType, message }
                    const sessionId = data.sessionId;
                    const senderType = data.senderType;
                    const message = data.message;

                    // 현재 선택된 고객과 같으면 바로 채팅창에 추가
                    if (sessionId && sessionId === selectedCustomerSessionId) {
                        const role = senderType === "CUSTOMER" ? "user" : (senderType === "SYSTEM" ? "system" : "assistant");
                        appendChatMessage(role, message);
                    }
                } catch (e) {
                    console.error("Agent WebSocket 메시지 파싱 오류", e);
                }
            };

            agentWebSocket.onclose = () => {
                console.log("Agent WebSocket 연결 종료");
                // 재연결 시도: 3초 후
                setTimeout(connectAgentWebSocket, 3000);
            };

            agentWebSocket.onerror = (e) => {
                console.error("Agent WebSocket 오류", e);
            };
        } catch (e) {
            console.error("Agent WebSocket 연결 중 예외", e);
        }
    }
    
    // --- 상담 대시보드 컨테이너 생성 ---
    const dashboard = document.createElement("div");
    dashboard.style.display = "flex";
    dashboard.style.height = "100vh";
    dashboard.style.background = "#0F172A";
    dashboard.style.color = "#E2E8F0";
    dashboard.style.fontFamily = "Inter, Pretendard, sans-serif";
    dashboard.style.fontSize = "14px";
    dashboard.style.zIndex = "10000";
    
    // --- 왼쪽 고객 리스트 패널 ---
    const leftPanel = document.createElement("div");
    leftPanel.style.width = "300px";
    leftPanel.style.background = "#1E293B";
    leftPanel.style.borderRight = "1px solid #334155";
    leftPanel.style.display = "flex";
    leftPanel.style.flexDirection = "column";
    leftPanel.style.overflow = "hidden";
    
    // 헤더
    const leftHeader = document.createElement("div");
    leftHeader.style.padding = "16px";
    leftHeader.style.borderBottom = "1px solid #334155";
    leftHeader.style.background = "#0F172A";
    leftHeader.innerHTML = `<h2 style="margin:0;font-size:16px;font-weight:600;">대기 고객 (${agentName})</h2>`;
    leftPanel.appendChild(leftHeader);
    
    // 고객 목록
    const customerList = document.createElement("div");
    customerList.style.flex = "1";
    customerList.style.overflowY = "auto";
    customerList.style.padding = "8px";
    leftPanel.appendChild(customerList);
    
    // --- 오른쪽 채팅 패널 ---
    const rightPanel = document.createElement("div");
    rightPanel.style.flex = "1";
    rightPanel.style.display = "flex";
    rightPanel.style.flexDirection = "column";
    rightPanel.style.background = "#1E293B";
    
    // 채팅 헤더
    const chatHeader = document.createElement("div");
    chatHeader.style.padding = "16px";
    chatHeader.style.borderBottom = "1px solid #334155";
    chatHeader.style.background = "#0F172A";
    chatHeader.style.display = "flex";
    chatHeader.style.justifyContent = "space-between";
    chatHeader.style.alignItems = "center";
    chatHeader.innerHTML = `
        <h3 style="margin:0;font-size:16px;font-weight:600;" id="chat-title">고객 선택</h3>
        <button id="end-chat-btn" style="display:none;background:#EF4444;color:white;border:none;
                 padding:8px 12px;border-radius:6px;cursor:pointer;font-size:12px;">상담 종료</button>
    `;
    rightPanel.appendChild(chatHeader);
    
    // 메시지 영역
    const messageArea = document.createElement("div");
    messageArea.style.flex = "1";
    messageArea.style.overflowY = "auto";
    messageArea.style.padding = "16px";
    messageArea.style.display = "flex";
    messageArea.style.flexDirection = "column";
    messageArea.style.gap = "8px";
    rightPanel.appendChild(messageArea);
    
    // 입력 영역
    const inputArea = document.createElement("div");
    inputArea.style.padding = "12px";
    inputArea.style.borderTop = "1px solid #334155";
    inputArea.style.background = "#0F172A";
    inputArea.style.display = "flex";
    inputArea.style.gap = "8px";
    inputArea.innerHTML = `
        <input type="text" id="message-input" placeholder="메시지 입력..."
               style="flex:1;padding:10px 12px;background:#334155;color:#E2E8F0;
                      border:1px solid #475569;border-radius:6px;outline:none;font-size:14px;">
        <button id="send-btn" style="background:#6366F1;color:white;border:none;
                 padding:10px 16px;border-radius:6px;cursor:pointer;font-weight:500;
                 transition:all 0.3s ease;">전송</button>
    `;
    rightPanel.appendChild(inputArea);
    
    // 대시보드 조립
    dashboard.appendChild(leftPanel);
    dashboard.appendChild(rightPanel);
    document.body.appendChild(dashboard);
    
    const messageInput = inputArea.querySelector("#message-input");
    const sendBtn = inputArea.querySelector("#send-btn");
    const endChatBtn = chatHeader.querySelector("#end-chat-btn");
    const chatTitle = chatHeader.querySelector("#chat-title");
    
    // --- 고객 목록 새로고침 ---
    async function refreshCustomerList() {
        try {
            const response = await fetch(`/api/admin/consultation/waiting-customers`, {
                method: "GET",
                headers: {"Content-Type": "application/json"}
            });
            
            const data = await response.json();
            customerList.innerHTML = "";
            
            if (data.customers.length === 0) {
                customerList.innerHTML = '<div style="padding:16px;color:#94A3B8;text-align:center;">대기 중인 고객이 없습니다</div>';
                return;
            }
            
            data.customers.forEach(customer => {
                const customerItem = document.createElement("div");
                customerItem.style.padding = "12px";
                customerItem.style.margin = "4px 0";
                customerItem.style.background = "#334155";
                customerItem.style.borderRadius = "6px";
                customerItem.style.cursor = "pointer";
                customerItem.style.transition = "all 0.3s ease";
                customerItem.style.borderLeft = "3px solid #6366F1";
                customerItem.innerHTML = `
                    <div style="font-weight:500;margin-bottom:4px;">고객 ${customer.sessionId.substring(0, 8)}</div>
                    <div style="font-size:12px;color:#CBD5E1;">상태: ${customer.status}</div>
                `;
                
                customerItem.addEventListener("mouseenter", () => {
                    customerItem.style.background = "#475569";
                });
                customerItem.addEventListener("mouseleave", () => {
                    if (selectedCustomerSessionId !== customer.sessionId) {
                        customerItem.style.background = "#334155";
                    }
                });
                
                customerItem.addEventListener("click", async () => {
                    selectCustomer(customer.sessionId);
                });
                
                customerList.appendChild(customerItem);
            });
        } catch (e) {
            console.error("고객 목록 조회 오류:", e);
        }
    }
    
    // --- 고객 선택 ---
    async function selectCustomer(sessionId) {
        selectedCustomerSessionId = sessionId;
        messageArea.innerHTML = "";
        chatTitle.innerText = `고객 ${sessionId.substring(0, 8)}과 채팅 중`;
        endChatBtn.style.display = "block";
        messageInput.disabled = false;
        sendBtn.disabled = false;
        
        // 기존 메시지 로드
        try {
            const response = await fetch(`/api/admin/consultation/messages/${sessionId}`);
            const messages = await response.json();
            
            messages.forEach(msg => {
                const role = msg.messageType === "CUSTOMER" ? "user" : "assistant";
                appendChatMessage(role, msg.message);
            });
        } catch (e) {
            console.error("메시지 조회 오류:", e);
        }
        
        // 고객 수락
        try {
            await fetch(`/api/admin/consultation/accept-customer/${sessionId}?agentId=${agentId}&agentName=${encodeURIComponent(agentName)}`, {
                method: "POST"
            });
            appendChatMessage("system", "이 고객과의 상담을 시작했습니다.");
        } catch (e) {
            console.error("고객 수락 오류:", e);
        }
    }
    
    // --- 채팅 메시지 추가 ---
    function appendChatMessage(role, text) {
        const msgDiv = document.createElement("div");
        msgDiv.style.display = "flex";
        msgDiv.style.justifyContent = role === "user" ? "flex-end" : "flex-start";
        msgDiv.style.marginBottom = "8px";
        msgDiv.style.animation = "slideIn 0.3s ease";
        
        const bubble = document.createElement("div");
        bubble.style.maxWidth = "70%";
        bubble.style.padding = "10px 14px";
        bubble.style.borderRadius = "12px";
        bubble.style.wordWrap = "break-word";
        bubble.style.lineHeight = "1.5";
        
        if (role === "user") {
            bubble.style.background = "#3B82F6";
            bubble.style.color = "white";
        } else if (role === "system") {
            bubble.style.background = "#475569";
            bubble.style.color = "#CBD5E1";
            bubble.style.fontSize = "12px";
            bubble.style.textAlign = "center";
        } else {
            bubble.style.background = "#475569";
            bubble.style.color = "#E2E8F0";
        }
        
        bubble.innerHTML = text.replace(/\n/g, "<br>");
        msgDiv.appendChild(bubble);
        messageArea.appendChild(msgDiv);
        messageArea.scrollTop = messageArea.scrollHeight;
    }
    
    // --- 메시지 전송 ---
    async function sendMessage() {
        const text = messageInput.value.trim();
        if (!text || !selectedCustomerSessionId) return;
        
        appendChatMessage("assistant", text);
        messageInput.value = "";
        
        // 백엔드에 메시지 저장
        try {
            const messageDto = {
                sessionId: selectedCustomerSessionId,
                messageType: "AGENT",
                message: text,
                agentId: agentId
            };
            
            await fetch(`/api/admin/consultation/send-message`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(messageDto)
            });
        } catch (e) {
            console.error("메시지 전송 오류:", e);
        }
    }
    
    // --- 상담 종료 ---
    endChatBtn.addEventListener("click", async () => {
        if (!selectedCustomerSessionId) return;
        
        try {
            await fetch(`/api/admin/consultation/end-consultation/${selectedCustomerSessionId}`, {
                method: "POST"
            });
            
            selectedCustomerSessionId = null;
            messageArea.innerHTML = "";
            chatTitle.innerText = "고객 선택";
            endChatBtn.style.display = "none";
            messageInput.disabled = true;
            sendBtn.disabled = true;
            
            refreshCustomerList();
        } catch (e) {
            console.error("상담 종료 오류:", e);
        }
    });
    
    // 이벤트 리스너
    sendBtn.addEventListener("click", sendMessage);
    messageInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    
    // 초기화
    messageInput.disabled = true;
    sendBtn.disabled = true;
    
    // 주기적으로 고객 목록 새로고침
    refreshCustomerList();
    setInterval(refreshCustomerList, 3000);
    // 상담사 WebSocket 연결 시도
    connectAgentWebSocket();
    
    // CSS 추가
    const style = document.createElement("style");
    style.innerHTML = `
        @keyframes slideIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        
        #send-btn:hover {
            background: #4F46E5;
            transform: translateY(-1px);
        }
        
        #send-btn:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }
    `;
    document.head.appendChild(style);
});

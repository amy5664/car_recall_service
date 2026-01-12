/**
 * 관리자 상담 대시보드 - 상담사가 고객을 관리하고 채팅하는 페이지
 */
document.addEventListener("DOMContentLoaded", function() {
    // 현재 로그인한 상담사 정보 (서버에서 전달)
    const agentId = document.querySelector("[data-agent-id]")?.getAttribute("data-agent-id") || "agent-" + Date.now();
    const agentName = document.querySelector("[data-agent-name]")?.getAttribute("data-agent-name") || "상담사";
    
    let selectedCustomerSessionId = null;
    let agentWebSocket = null;
    let customerWebSockets = new Map(); // sessionId -> WebSocket
    let isConsultationEnded = false;  // 상담 종료 상태 추적
    let chatHistoryCache = new Map(); // sessionId -> 채팅 내용 HTML 캐시
    let acceptedCustomers = new Set(); // 이미 수락한 고객 목록

    // 컬러 스킴 (고객 챗 UI와 일치시키기)
    const PRIMARY_COLOR = "#8ECFFB"; // pastel sky blue
    const SECONDARY_COLOR = "#BEE7FF"; // lighter sky tone
    const LIGHT_BG = "#f7f9fc";
    const BORDER_COLOR = "#e6e8ee";

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
                    
                    console.log("Agent WebSocket 메시지 수신:", data);
                    
                    // 상담 종료 이벤트 처리
                    if (data.type === "CONSULTATION_ENDED") {
                        const sessionId = data.sessionId;
                        if (sessionId && sessionId === selectedCustomerSessionId) {
                            isConsultationEnded = true;  // 상담 종료 상태 설정
                            appendChatMessage("system", "고객이 상담을 종료했습니다.");
                            messageInput.disabled = true;
                            sendBtn.disabled = true;
                            endChatBtn.style.display = "none";
                            messageInput.style.opacity = "0.5";
                            messageInput.style.cursor = "not-allowed";
                            sendBtn.style.opacity = "0.5";
                            sendBtn.style.cursor = "not-allowed";
                        }
                        return;
                    }
                    
                    // MessageNotificationDTO: { sessionId, senderType, message }
                    const sessionId = data.sessionId;
                    const senderType = data.senderType;
                    const message = data.message;

                    // 현재 선택된 고객과 같으면 바로 채팅창에 추가
                    if (sessionId && sessionId === selectedCustomerSessionId) {
                        const role = senderType === "CUSTOMER" ? "user" : (senderType === "SYSTEM" ? "system" : "assistant");
                        appendChatMessage(role, message);
                    } else if (sessionId) {
                        // 선택되지 않은 고객의 메시지는 캐시에 추가
                        console.log("선택되지 않은 고객의 메시지 - 캐시에 추가:", sessionId);
                        const role = senderType === "CUSTOMER" ? "user" : (senderType === "SYSTEM" ? "system" : "assistant");
                        
                        // 임시로 메시지를 생성하여 캐시에 추가
                        const tempDiv = document.createElement("div");
                        tempDiv.style.display = "flex";
                        tempDiv.style.justifyContent = role === "user" ? "flex-start" : "flex-end";
                        tempDiv.style.alignItems = "flex-end";
                        tempDiv.style.animation = "slideIn 0.3s ease";
                        tempDiv.style.gap = "8px";

                        const bubble = document.createElement("div");
                        bubble.style.padding = "10px 14px";
                        bubble.style.borderRadius = "12px";
                        bubble.style.wordWrap = "break-word";
                        bubble.style.lineHeight = "1.5";

                        if (role === "user") {
                            bubble.style.background = "#E6EEF9";
                            bubble.style.color = "#0b1020";
                        } else if (role === "system") {
                            bubble.style.background = "#475569";
                            bubble.style.color = "#CBD5E1";
                            bubble.style.fontSize = "12px";
                            bubble.style.textAlign = "center";
                        } else {
                            bubble.style.background = "#3B82F6";
                            bubble.style.color = "white";
                        }

                        bubble.innerHTML = message.replace(/\n/g, "<br>");
                        tempDiv.appendChild(bubble);
                        
                        // 캐시에 메시지 추가
                        const existingCache = chatHistoryCache.get(sessionId) || "";
                        chatHistoryCache.set(sessionId, existingCache + tempDiv.outerHTML);
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
    // 고객 챗 UI와 동일한 스타일 적용
    const dashboard = document.createElement("div");
    dashboard.style.display = "none";
    dashboard.style.position = "fixed";
    dashboard.style.bottom = "100px";
    dashboard.style.right = "24px";
    dashboard.style.width = "640px";
    dashboard.style.height = "600px";
    dashboard.style.background = LIGHT_BG;
    dashboard.style.border = `1px solid ${BORDER_COLOR}`;
    dashboard.style.borderRadius = "16px";
    dashboard.style.boxShadow = "0 12px 32px rgba(16, 33, 80, 0.13)";
    dashboard.style.zIndex = "10000";
    dashboard.style.flexDirection = "row";
    dashboard.style.overflow = "hidden";
    
    // --- 왼쪽 고객 리스트 패널 ---
    const leftPanel = document.createElement("div");
    leftPanel.style.width = "220px";
    leftPanel.style.background = LIGHT_BG;
    leftPanel.style.borderRight = `1.5px solid ${BORDER_COLOR}`;
    leftPanel.style.display = "flex";
    leftPanel.style.flexDirection = "column";
    leftPanel.style.overflow = "hidden";
    leftPanel.style.height = "100%";
    
    // 헤더
    const leftHeader = document.createElement("div");
    leftHeader.style.padding = "18px 12px";
    leftHeader.style.borderBottom = `1.5px solid ${BORDER_COLOR}`;
    leftHeader.style.background = `linear-gradient(135deg, ${PRIMARY_COLOR}, ${SECONDARY_COLOR})`;
    leftHeader.innerHTML = `<h2 style="margin:0;font-size:15px;font-weight:600;color:white;letter-spacing:0.5px;">대기 고객</h2>`;
    leftPanel.appendChild(leftHeader);
    
    // 고객 목록
    const customerList = document.createElement("div");
    customerList.style.flex = "1";
    customerList.style.overflowY = "auto";
    customerList.style.padding = "10px 6px";
    customerList.style.background = LIGHT_BG;
    leftPanel.appendChild(customerList);
    
    // --- 오른쪽 채팅 패널 ---
    const rightPanel = document.createElement("div");
    rightPanel.style.flex = "1 1 0%";
    rightPanel.style.minWidth = "0";
    rightPanel.style.display = "flex";
    rightPanel.style.flexDirection = "column";
    rightPanel.style.background = LIGHT_BG;
    rightPanel.style.height = "100%";

    // 채팅 헤더 (고객 챗과 동일한 그라데이션, 컨트롤 버튼)
    const chatHeader = document.createElement("div");
    chatHeader.style.background = `linear-gradient(135deg, ${PRIMARY_COLOR}, ${SECONDARY_COLOR})`;
    chatHeader.style.color = "white";
    chatHeader.style.padding = "16px 20px";
    chatHeader.style.display = "flex";
    chatHeader.style.justifyContent = "space-between";
    chatHeader.style.alignItems = "center";

    // 타이틀
    const titleSpan = document.createElement("span");
    titleSpan.id = "chat-title";
    titleSpan.innerText = "고객 선택";
    titleSpan.style.fontWeight = "600";
    titleSpan.style.fontSize = "16px";

    // 컨트롤 버튼 영역
    const headerBtnBox = document.createElement("div");
    headerBtnBox.style.display = "flex";
    headerBtnBox.style.gap = "8px";
    headerBtnBox.style.alignItems = "center";

    // 최소화 버튼
    const minimizeBtn = document.createElement("button");
    minimizeBtn.innerText = "−";
    minimizeBtn.title = "최소화";
    minimizeBtn.style.background = "rgba(255,255,255,0.2)";
    minimizeBtn.style.border = "none";
    minimizeBtn.style.color = "white";
    minimizeBtn.style.padding = "4px 10px";
    minimizeBtn.style.borderRadius = "6px";
    minimizeBtn.style.cursor = "pointer";
    minimizeBtn.style.fontSize = "18px";
    minimizeBtn.addEventListener("click", () => {
        dashboard.style.display = "none";
        toggleBtn.innerText = "🙂";
    });

    // 닫기 버튼
    const closeBtn = document.createElement("button");
    closeBtn.innerText = "✕";
    closeBtn.title = "닫기";
    closeBtn.style.background = "rgba(255,255,255,0.2)";
    closeBtn.style.border = "none";
    closeBtn.style.color = "white";
    closeBtn.style.padding = "4px 10px";
    closeBtn.style.borderRadius = "6px";
    closeBtn.style.cursor = "pointer";
    closeBtn.style.fontSize = "18px";
    closeBtn.addEventListener("click", () => {
        dashboard.style.display = "none";
        toggleBtn.style.display = "none";
        setTimeout(() => { toggleBtn.style.display = "flex"; }, 500); // 다시 보이게
    });

    // 상담 종료 버튼
    const endChatBtn = document.createElement("button");
    endChatBtn.id = "end-chat-btn";
    endChatBtn.innerText = "상담 종료";
    endChatBtn.style.display = "none";
    endChatBtn.style.background = "#EF4444";
    endChatBtn.style.color = "white";
    endChatBtn.style.border = "none";
    endChatBtn.style.padding = "8px 12px";
    endChatBtn.style.borderRadius = "6px";
    endChatBtn.style.cursor = "pointer";
    endChatBtn.style.fontSize = "12px";

    headerBtnBox.appendChild(minimizeBtn);
    headerBtnBox.appendChild(closeBtn);
    headerBtnBox.appendChild(endChatBtn);
    chatHeader.appendChild(titleSpan);
    chatHeader.appendChild(headerBtnBox);
    rightPanel.appendChild(chatHeader);
    
    // 메시지 영역
    const messageArea = document.createElement("div");
    messageArea.style.flex = "1 1 0%";
    messageArea.style.overflowY = "auto";
    messageArea.style.padding = "16px";
    messageArea.style.display = "flex";
    messageArea.style.flexDirection = "column";
    messageArea.style.gap = "8px";
    rightPanel.appendChild(messageArea);
    
    // 입력 영역
    const inputArea = document.createElement("div");
        inputArea.style.padding = "12px";
        inputArea.style.borderTop = `1.5px solid ${BORDER_COLOR}`;
        inputArea.style.background = LIGHT_BG;
        inputArea.style.display = "flex";
        inputArea.style.gap = "8px";
        inputArea.innerHTML = `
         <input type="text" id="message-input" placeholder="메시지 입력..."
             style="flex:1;padding:10px 12px;background:white;color:#222;
                 border:1.5px solid ${PRIMARY_COLOR};border-radius:7px;outline:none;font-size:14px;box-shadow:0 1px 4px rgba(142,207,251,0.08);">
         <button id="send-btn" style="background:${PRIMARY_COLOR};color:#1e293b;border:none;
               padding:10px 16px;border-radius:7px;cursor:pointer;font-weight:600;
               transition:all 0.3s ease;box-shadow:0 2px 8px rgba(142,207,251,0.13);">전송</button>
        `;
    rightPanel.appendChild(inputArea);
    
    // 플로팅 토글 버튼(관리자용)
    const toggleBtn = document.createElement("button");
    toggleBtn.id = "agent-dashboard-toggle";
    toggleBtn.innerText = "👩‍💼";
    toggleBtn.title = "상담 대시보드 열기/닫기";
    toggleBtn.style.position = "relative";
    toggleBtn.style.bottom = "24px";
    toggleBtn.style.right = "24px";
    // 고객 챗 버튼과 동일한 스타일
    toggleBtn.style.width = "64px";
    toggleBtn.style.height = "64px";
    toggleBtn.style.borderRadius = "50%";
    toggleBtn.style.background = `linear-gradient(135deg, ${PRIMARY_COLOR}, ${SECONDARY_COLOR})`;
    toggleBtn.style.color = "white";
    toggleBtn.style.border = "none";
    toggleBtn.style.cursor = "pointer";
    toggleBtn.style.zIndex = "10001";
    toggleBtn.style.boxShadow = "0 8px 20px rgba(142, 207, 251, 0.35)";
    toggleBtn.style.fontSize = "26px";
    toggleBtn.style.display = "flex";
    toggleBtn.style.alignItems = "center";
    toggleBtn.style.justifyContent = "center";
    toggleBtn.style.transition = "all 0.3s ease";
    toggleBtn.addEventListener("mouseenter", () => { toggleBtn.style.transform = "scale(1.1)"; toggleBtn.style.boxShadow = "0 12px 24px rgba(142, 207, 251, 0.45)"; });
    toggleBtn.addEventListener("mouseleave", () => { toggleBtn.style.transform = "scale(1)"; toggleBtn.style.boxShadow = "0 8px 20px rgba(142, 207, 251, 0.35)"; });

    // 알림 뱃지 생성
    const notificationBadge = document.createElement("div");
    notificationBadge.id = "agent-notification-badge";
    notificationBadge.style.position = "absolute";
    notificationBadge.style.top = "-4px";
    notificationBadge.style.right = "-4px";
    notificationBadge.style.minWidth = "22px";
    notificationBadge.style.height = "22px";
    notificationBadge.style.borderRadius = "11px";
    notificationBadge.style.background = "#EF4444";
    notificationBadge.style.color = "white";
    notificationBadge.style.fontSize = "12px";
    notificationBadge.style.fontWeight = "700";
    notificationBadge.style.display = "none";
    notificationBadge.style.alignItems = "center";
    notificationBadge.style.justifyContent = "center";
    notificationBadge.style.padding = "0 6px";
    notificationBadge.style.boxShadow = "0 2px 8px rgba(239, 68, 68, 0.4)";
    notificationBadge.style.border = "2px solid white";
    toggleBtn.appendChild(notificationBadge);
    
    // 버튼 컨테이너 (fixed positioning용)
    const toggleBtnContainer = document.createElement("div");
    toggleBtnContainer.style.position = "fixed";
    toggleBtnContainer.style.bottom = "24px";
    toggleBtnContainer.style.right = "24px";
    toggleBtnContainer.style.zIndex = "10001";
    toggleBtnContainer.appendChild(toggleBtn);

    toggleBtn.addEventListener("click", () => {
        if (dashboard.style.display === "none") {
            dashboard.style.display = "flex";
            toggleBtn.innerText = "✕";
            notificationBadge.style.display = "none"; // 대시보드 열면 뱃지 숨김
            // WebSocket 연결이 아직 안되어 있으면 시도
            if (!agentWebSocket || agentWebSocket.readyState === WebSocket.CLOSED) {
                connectAgentWebSocket();
            }
        } else {
            dashboard.style.display = "none";
            toggleBtn.innerText = "🙂";
            // 대시보드 닫을 때 대기 고객 있으면 다시 뱃지 표시
            refreshCustomerList();
        }
    });

    // 대시보드 조립
    dashboard.appendChild(leftPanel);
    dashboard.appendChild(rightPanel);
    document.body.appendChild(dashboard);
    document.body.appendChild(toggleBtnContainer);
    
    const messageInput = inputArea.querySelector("#message-input");
    const sendBtn = inputArea.querySelector("#send-btn");
    // endChatBtn은 chatHeader 생성 시 직접 생성된 객체를 그대로 사용
    const chatTitle = titleSpan;
    
    // --- 고객 목록 새로고침 ---
    async function refreshCustomerList() {
        try {
            const response = await fetch(`/api/admin/consultation/waiting-customers?t=${Date.now()}`, {
                method: "GET",
                headers: {"Content-Type": "application/json"}
            });
            
            const data = await response.json();
            console.log("고객 목록 조회 결과:", data); // 디버깅용
            customerList.innerHTML = "";
            
            // 대기 중인 고객 수 업데이트 (대시보드가 닫혀있을 때만 뱃지 표시)
            const waitingCount = data.customers.length;
            if (dashboard.style.display === "none" && waitingCount > 0) {
                notificationBadge.innerText = waitingCount > 99 ? "99+" : waitingCount.toString();
                notificationBadge.style.display = "flex";
            } else {
                notificationBadge.style.display = "none";
            }
            
            if (data.customers.length === 0) {
                customerList.innerHTML = `<div style="padding:18px 0;color:#b0b8c1;text-align:center;font-size:14px;">대기 중인 고객이 없습니다</div>`;
                return;
            }
            
            data.customers.forEach(customer => {
                const customerItem = document.createElement("div");
                customerItem.style.padding = "13px 10px 11px 14px";
                customerItem.style.margin = "7px 0";
                // 선택된 고객은 진한 파랑, 나머지는 연한 파랑
                if (selectedCustomerSessionId === customer.sessionId) {
                    customerItem.style.background = "#b6e0fe";
                } else {
                    customerItem.style.background = "#eaf6fd";
                }
                customerItem.style.borderRadius = "8px";
                customerItem.style.cursor = "pointer";
                customerItem.style.transition = "all 0.2s";
                customerItem.style.border = `1.5px solid ${BORDER_COLOR}`;
                customerItem.style.boxShadow = "0 2px 8px rgba(142,207,251,0.07)";
                customerItem.innerHTML = `
                    <div style="font-weight:600;margin-bottom:2px;color:#1e293b;font-size:14px;">고객 ${customer.sessionId.substring(0, 8)}</div>
                    <div style="font-size:12px;color:#3b82f6;">상태: ${customer.status}</div>
                `;
                customerItem.addEventListener("mouseenter", () => {
                    customerItem.style.background = selectedCustomerSessionId === customer.sessionId ? "#b6e0fe" : "#d2eafd";
                });
                customerItem.addEventListener("mouseleave", () => {
                    if (selectedCustomerSessionId === customer.sessionId) {
                        customerItem.style.background = "#b6e0fe";
                    } else {
                        customerItem.style.background = "#eaf6fd";
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
        // 현재 선택된 고객의 채팅 내용 저장
        if (selectedCustomerSessionId && messageArea.innerHTML) {
            chatHistoryCache.set(selectedCustomerSessionId, messageArea.innerHTML);
        }
        
        selectedCustomerSessionId = sessionId;
        // 고객 선택 시 대기목록 색상 동기화
        Array.from(customerList.children).forEach(item => {
            const label = item.querySelector('div');
            if (label && label.textContent.includes(sessionId.substring(0, 8))) {
                item.style.background = "#b6e0fe";
            } else {
                item.style.background = "#eaf6fd";
            }
        });
        isConsultationEnded = false;  // 고객 선택 시 상태 초기화
        
        // 캐시된 채팅 내용이 있으면 복원, 없으면 서버에서 로드
        if (chatHistoryCache.has(sessionId)) {
            messageArea.innerHTML = chatHistoryCache.get(sessionId);
            messageArea.scrollTop = messageArea.scrollHeight;
        } else {
            messageArea.innerHTML = "";
            // 기존 메시지 로드
            try {
                const response = await fetch(`/api/admin/consultation/messages/${sessionId}`);
                const messages = await response.json();
                
                messages.forEach(msg => {
                    const role = msg.messageType === "CUSTOMER" ? "user" : "assistant";
                    appendChatMessage(role, msg.message);
                });
                // 로드 후 캐시에 저장
                chatHistoryCache.set(sessionId, messageArea.innerHTML);
            } catch (e) {
                console.error("메시지 조회 오류:", e);
            }
        }
        
        chatTitle.innerText = `고객 ${sessionId.substring(0, 8)}과 채팅 중`;
        endChatBtn.style.display = "block";
        messageInput.disabled = false;
        sendBtn.disabled = false;
        messageInput.style.opacity = "1";
        messageInput.style.cursor = "text";
        sendBtn.style.opacity = "1";
        sendBtn.style.cursor = "pointer";
        
        // 고객 수락 (첫 선택 시에만)
        if (!acceptedCustomers.has(sessionId)) {
            try {
                await fetch(`/api/admin/consultation/accept-customer/${sessionId}?agentId=${agentId}&agentName=${encodeURIComponent(agentName)}`, {
                    method: "POST"
                });
                acceptedCustomers.add(sessionId);
                appendChatMessage("system", "이 고객과의 상담을 시작했습니다.");
            } catch (e) {
                console.error("고객 수락 오류:", e);
            }
        }
    }
    
    // --- 채팅 메시지 추가 ---
    function appendChatMessage(role, text) {
        const msgDiv = document.createElement("div");
        msgDiv.style.display = "flex";
        // 고객(user)은 왼쪽, 상담사(assistant)는 오른쪽에 표시
        msgDiv.style.justifyContent = role === "user" ? "flex-start" : "flex-end";
        msgDiv.style.marginBottom = "8px";
        msgDiv.style.animation = "slideIn 0.3s ease";
        
        const bubble = document.createElement("div");
        bubble.style.maxWidth = "70%";
        bubble.style.padding = "10px 14px";
        bubble.style.borderRadius = "12px";
        bubble.style.wordWrap = "break-word";
        bubble.style.lineHeight = "1.5";
        
        if (role === "user") {
            // 고객: 왼쪽, 연한 회색 배경
            bubble.style.background = "#E6EEF9";
            bubble.style.color = "#0b1020";
        } else if (role === "system") {
            bubble.style.background = "#475569";
            bubble.style.color = "#CBD5E1";
            bubble.style.fontSize = "12px";
            bubble.style.textAlign = "center";
        } else {
            // 상담사: 오른쪽, 파란색 배경
            bubble.style.background = "#3B82F6";
            bubble.style.color = "white";
        }
        
        bubble.innerHTML = text.replace(/\n/g, "<br>");
        msgDiv.appendChild(bubble);
        messageArea.appendChild(msgDiv);
        messageArea.scrollTop = messageArea.scrollHeight;
        
        // 현재 선택된 고객의 채팅 내용 캐시 업데이트
        if (selectedCustomerSessionId) {
            chatHistoryCache.set(selectedCustomerSessionId, messageArea.innerHTML);
        }
    }
    
    // --- 메시지 전송 ---
    async function sendMessage() {
        const text = messageInput.value.trim();
        if (!text || !selectedCustomerSessionId) return;
        
        // 상담 종료되었으면 전송 불가
        if (isConsultationEnded) {
            appendChatMessage("system", "상담이 종료되었습니다. 메시지를 전송할 수 없습니다.");
            return;
        }
        
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
        
        console.log("상담 종료 버튼 클릭:", selectedCustomerSessionId);
        
        try {
            const endedSessionId = selectedCustomerSessionId;
            const response = await fetch(`/api/admin/consultation/end-consultation/${endedSessionId}`, {
                method: "POST"
            });
            
            console.log("상담 종료 응답:", response.status, response.statusText);
            
            if (!response.ok) {
                console.error("상담 종료 실패:", response.status);
                return;
            }
            
            // 캐시에서 해당 고객의 채팅 내용 삭제
            chatHistoryCache.delete(endedSessionId);
            // 수락한 고객 목록에서 제거
            acceptedCustomers.delete(endedSessionId);
            
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

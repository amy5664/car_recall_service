package com.boot.OpenAiClient;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.boot.dto.OpenAiMessageDTO;
import com.boot.dto.OpenAiRequestDTO;
import com.boot.dto.OpenAiResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {
	private final RestTemplate restTemplate;
	 
    @Value("${openai.api-url}")
    private String apiUrl;
    @Value("${openai.model}")
    private String model;
 
    public OpenAiResponseDTO getChatCompletion(String prompt) {
        try {
            OpenAiRequestDTO openAiRequest = getOpenAiRequest(prompt);
            log.info("OpenAI API 호출 시작 - URL: {}, Model: {}", apiUrl, model);
            
            ResponseEntity<OpenAiResponseDTO> chatResponse = restTemplate.postForEntity(
                    apiUrl,
                    openAiRequest,
                    OpenAiResponseDTO.class
            );
     
            if (!chatResponse.getStatusCode().is2xxSuccessful() || chatResponse.getBody() == null) {
                log.error("OpenAI API 호출 실패 - 상태 코드: {}", chatResponse.getStatusCode());
                throw new RuntimeException("OpenAI API 호출 실패 - 상태: " + chatResponse.getStatusCode());
            }
     
            log.info("OpenAI API 호출 성공");
            return chatResponse.getBody();
        } catch (Exception e) {
            log.error("OpenAI API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }
 
    private OpenAiRequestDTO getOpenAiRequest(String prompt) {
        OpenAiMessageDTO systemMessage = new OpenAiMessageDTO(
                "system",
                "항상 사용자의 이해를 돕기 위해 리스트를 출력할 때는 숫자 뒤에 줄바꿈을 넣어"
                +"그리고 넌 친절한 비서야"
                +"사용자에게 보내는 모든 내용은 맞춤법을 준수 하고 사용자가 읽기에 가독성 좋게 만들어."
                +"사용자가 도움이라고 치면 아래 예시를 보내"		
                + "예시:"
                + "1️⃣ 내 차량 리콜 가능성 검토<br>"
                + "2️⃣ 차량 등록 방법<br>"
                + "3️⃣ 리콜 절차 안내<br>"
                + "4️⃣ 가까운 서비스센터 찾기<br>"
                + "5️⃣ 결함 접수/신고 내역 안내<br>"
                + "6️⃣ 리콜 통계 확인 <br>"
                + "7️⃣ 상담사 연결<br>"
                + "줄바꿈은 \\\\n 대신 반드시 <br> 을 사용하세요.<br>"
                + "출력 시 사용할 수 있는 HTML 태그는 <br>, <a>,<b>만 허용합니다.<br>"
                + "그 외 모든 HTML 태그는 절대 포함하지 마세요."
                + "상대방이 1, 리콜 가능성 검토 와 관련된 말을하면 먼저 아래 첫번쨰 링크로 들어가서 <b>결함 신고</b>를 작성해주세요! 만약 결함 신고를 하셨다면 두번째 링크로 들어가셔서 <b>신고내역</b>을 확인해 주세요"
                + "👉 <a href=\"http://localhost:8484/report/write\">내 차량 결함 신고 하기</a><br>"
                + "👉 <a href=\"http://localhost:8484/report/history\">내 차량 결함 신고 내역 확인하기</a>라고 보내"
                + "상대방이 2, 차량 등록 등의 말을하면 차량 등록은 <b>로그인 후</b>에 이용 하실 수 있습니다 만약 로그인을 하셨다면 아래 링크를 클릭 하시면 바로 도와드릴게요"
                + "👉 <a href=\"http://localhost:8484/my-vehicles\">내 차량 등록하기</a>라고 보내"
                + "상대방이 3, 리콜 절차와 같은 말을 하면"
                + "자동차 리콜은 총 5단계로 진행됩니다.<br>"
                + "1️⃣ 접수 — 리콜 대상 여부 확인 및 신청<br>"
                + "2️⃣ 조사 — 결함 원인 분석<br>"
                + "3️⃣ 공지 — 제조사에서 고객에게 통지<br>"
                + "4️⃣ 무상수리 — 지정 서비스센터 방문 후 수리<br>"
                + "5️⃣ 종결 — 사후 점검 및 리콜 종료<br><br>"
                + "자세한 절차는 아래 링크에서 확인하실 수 있어요.<br>"
                + "👉 <a href=\"http://localhost:8484/centers/about\">리콜 절차 자세히 보기</a>라고 보내"
                +"상대방이 4, 서비스 센터 찾기와 같은 말을 하면 가까운 서비스 센터를 찾으시나요? 아래 링크를 클릭하시면 바로 도와드릴게요! 라고 보내"
                + "👉 <a href=\"http://localhost:8484/centers/map\">가까운 서비스 센터 찾기</a>도 같이 보내"
                +"상대방이 5, 리콜 접수와 같은 말을 하면 결함접수/신고는 메인페이지 상단에 보시면 <b>결함신고</b> 버튼이 있어요! 아래 링크를 클릭하시면 바로 도와드릴게요 라고 보내"
                + "👉 <a href=\"http://localhost:8484/report/write\">결함 신고</a>도 같이 보내"
                +"상대방이 6, 리콜 통계 와 같은 말을 하면 리콜통계는 메인페이지 상단에 보시면 <b>리콜정보</b> 버튼이 있어요! 클릭하시면 <b>리콜현황</b>과 <b>리콜통계</b>, <b>리콜관련 보도자료</b>를 확인하실수 있어요! 아래 링크를 클릭하시면 바로 도와드릴게요 라고 보내"
                + "👉 <a href=\"http://localhost:8484/recall-status\">리콜 현황</a><br>"
                + "👉 <a href=\"http://localhost:8484/recall/stats\">리콜 통계</a><br>"
                + "👉 <a href=\"http://localhost:8484/board/list\">리콜 보도자료</a><br>링크 3개를 같이 보내"
                + "상대방이 7, 삼담사 연결과 같은 말을 하면 체팅방 <b>상단 부분</b>응 보시면 <b>상담원 연결</b>이 있습니다! 버튼을 누르시면 상단원연결을 도와드릴게요 라고 보내"
                + "위 내용과 관련없는 질문이 나오면 정중하게 리콜관련 질문이 아니면 알 수 없다고 말해"
        );
        OpenAiMessageDTO userMessage = new OpenAiMessageDTO("user", prompt);
        List<OpenAiMessageDTO> messages = List.of(systemMessage, userMessage);
        return new OpenAiRequestDTO(model, messages);
    }
}
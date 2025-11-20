# queue-system-mvp

## 1. 프로젝트 개요 & 목표

### 프로젝트 개요
- 예매 대기열 시스템

### 해결하려는 문제
- 예매를 하는 과정에서의 시스템 분석과 서버 부하를 확인하고 전반적인 시스템 분석

### 범위
- 결제/좌석에 대한 도메인은 제외하고, 대기열 관리 및 입장 토큰 발급을 중점적으로 개발합니다.

## 2. 기술 스택

### 백엔드
- Spring, Redis, Java, JPA , K6

## 3. 아키텍처
<img width="849" height="593" alt="image" src="https://github.com/user-attachments/assets/33f5e012-9488-4561-873e-421680c3a446" />

- 해당 빨간 Loop는 이벤트 처리 방식으로 동작할 예정이며, 사용자 편의성을 위해 그려진 내용입니다.
- 놀이공원을 생각해본다면 입장 줄을 서고 기다리는 방식이라 생각했더니 위에 아키텍처를 변경해야겠다라는 생각이 들어 재구성 할 예정이다.


<img width="693" height="515" alt="image" src="https://github.com/user-attachments/assets/ccf0279a-1dbf-4551-8438-6e3ecd26ffc1" />
- 기존 방식으로 하나의 모듈로 작업을 진행하려했지만, 서버 부하의 테스트를 정확하게 해보고싶고 역할을 나누고싶다는 생각때문에 해당 아키텍처를 구성했습니다.

## 4. API 설계
### 4.1. Queue Server
#### 대기열 입장 API
요청
- 메서드 : POST
- URL : /api/v1/queue/enter
- 헤더 : Content-Type : application/json

본문
```
  EnterQueueRequest {
     userUuid : String,
     contentId : String,
     deviceId: String
  }
```

응답


Case 1 : 대기열 없이 입장하는 경우
- 상태 코드 : 200 OK
  ```
    EnterQueueResponse {
      "resultCode": "ENTRY_GRANTED"
      "message": "입장 가능!",
      "data": {
          "currentRank": 0,          
          "accessToken": accessToken,
          "redirectUrl": "https://ticket.api.com/"
      } 
    }
  ```
Case 2 : 대기열 입장하는 경우
- 상태 코드 : 200 OK
  ```
  EnterQueueResponse {
    "resultCode": "QUEUE_WAIT"
    "message": "대기열에 등록되었습니다.",
    "data": {
        "queueToken": "abc-123-def-456",  // ★ 핵심: SSE 연결용 토큰
        "currentRank": 150,             
        "sseUrl": "https://queue.api.com/queue/connect"
    } 
  }
  ```



- 순번 확인 API
### 4.2. Ticket Server
- 예매 페이지 입장 API

## 5. 컴포넌트 설계서
### 5.1. Ticket Server
- 타임아웃 감지
- 세션 체크
- 이탈 이벤트
## 6. 데이터 스토리지 설계
## 7. 부하 테스트 및 모니터링 시나리오

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
          "redirectUrl": "https://ticket.api.com/api/v1/ticket/entry"
      } 
    }
  ```
Case 2 : 대기열 입장하는 경우
- 상태 코드 : 200 OK
  ```
  EnterQueueResponse {
    "resultCode": "QUEUE_WAIT"
    "message": "대기열 등록이 필요합니다.",
    "data": {
        "queueToken": "abc-123-def-456",
        "currentRank": 150,             
        "sseUrl": "https://queue.api.com/api/v1/queue/connect"
    } 
  }
  ```

#### 순번 확인 API

대기 중인 유저가 서버와 지속적인 연결을 맺고 자신의 순번을 실시간으로 수신합니다.

요청
- 메서드 : GET
- URL : /api/v1/queue/connect
- 헤더 :
  - Authorization: Bearer {queueToken}
  - Accept : text/event-stream

응답
  - 상태 코드 : 200 OK
  - Content-Type : text/event-stream
  

  Case 1 : 대기 중 (event: waiting)
  ```
  ConnectQueueResponse {
     "rank": 150
  }
  ```

  Case 2 : 입장 허가 (event: admission)
  ```
  ConnectQueueResponse {
     "accessToken": "final-access-token-xyz",
     "redirectUrl": "https://ticket.api.com/api/v1/ticket/entry"
  }
  ```

  Case 3 : 에러 (event: error)
  ```
   ProblemDetail {
      "code": "INVALID_TOKEN",
      "message": "대기열 토큰이 만료되었습니다."
   }
  ```
  

### 4.2. Ticket Server
#### 예매 페이지 입장 API
요청
- 메서드 : POST
- URL : /api/v1/ticket/entry
- 헤더 :
  - Authorization: Bearer {accessToken}
  - Content-Type: application/json


응답 

  Case 1 : 정상 진입 (유효한 토큰)
  - 상태 코드 : 200 OK
  - 설명 : 토큰 검증 성공. Redis의 활성 유저(Active User) TTL을 갱신하고 남은 시간을 반환합니다.
  ```
  EnterTicketResponse {
    "resultCode": "ACCESS_ALLOWED",
    "message": "예매 페이지 진입 성공",
    "data": {
        "userUuid": "user-1234",
        "contentId": "concert-iu-2025",
        "validUntil": "2025-11-23T12:40:00", 
        "remainingSeconds": 600        
    } 
  }
  ```

  Case 2 : 진입 거부 (토큰 만료/위조/접근 불가)
  - 상태 코드 : 401 Unauthorized
  - 설명 : 대기열을 거치지 않았거나, 유효 시간이 만료된 경우 대기열로 다시 보냅니다.
  ```
    ProblemDetail {
      "code": "ACCESS_DENIED",
      "message": "유효하지 않은 접근이거나 세션이 만료되었습니다.",
      "data": {
        "redirectUrl": "https://queue.api.com/api/v1/queue/enter
      }
    }
  ```
  
  
## 5. 컴포넌트 설계서
### 5.1. Ticket Server
- 타임아웃 감지
- 세션 체크
- 이탈 이벤트
## 6. 데이터 스토리지 설계
## 7. 부하 테스트 및 모니터링 시나리오

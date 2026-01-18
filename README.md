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

이 문서는 대기열 시스템의 성능과 안정성을 검증하기 위한 부하 테스트 시나리오 및 모니터링 방법을 정의합니다. 테스트 도구는 **k6**를 사용하며, 실시간 상태 관제는 자체 구현한 **모니터링 대시보드**를 활용 및 프로메테우스 + 그라파나를 활용하여 측정했습니다.

---

### 7.1 테스트 환경 및 도구

*   **Load Testing Tool**: [k6](https://k6.io/) (Open Source Load Testing Tool)
*   **Target Server**: `localhost:8080` (Local Development Environment)
*   **Monitoring**: `/monitor` (Thymeleaf + Chart.js 기반 실시간 대시보드)

---

### 7.2 시나리오 설계: 대규모 트래픽 순간 유입

실제 인기 콘서트 티켓팅 오픈 상황을 가정하여, 짧은 시간에 대규모 인원이 동시에 접속하고 대기열이 소진될 때까지 기다리는 시나리오를 구성했습니다.

#### 테스트 목표
1.  **동시성 제어 확인**: 수천 명의 유저가 동시에 진입(`ENTER`)할 때 DB 락이나 데이터 정합성 문제가 없는지 검증.
2.  **대기열 동작 검증**: 모든 인원이 정상적으로 `WAIT` 상태를 거쳐 순차적으로 `PROCEED` → `DONE`으로 처리되는지 확인.
3.  **처리량(Throughput) 측정**: 시스템이 설정된 처리 속도(초당 N명)를 안정적으로 유지하는지 모니터링.

### 🛠 k6 시나리오 설정 (`k6/load-test.js`)

```javascript
export const options = {
  scenarios: {
    burst_traffic: {
      executor: 'per-vu-iterations', // 각 VU별로 정해진 횟수만큼 실행
      vus: 3000,                     // 동시 접속 가상 유저(VU) 수: 3,000명
      iterations: 1,                 // 1인당 1회 실행 (재진입 없음)
      maxDuration: '10m',            // 최대 테스트 지속 시간 (10분)
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],  // 에러율 1% 미만 허용
  },
};
```

#### 유저 행동 패턴
1.  **Enter (진입)**:
    *   `POST /api/v1/queue/enter` 요청으로 대기열 토큰 발급.
    *   3,000명의 유저가 거의 동시에 이 API를 호출.
2.  **Wait & Poll (대기)**:
    *   입장 허가(`ENTRY_GRANTED`)를 받을 때까지 반복문(While Loop) 진입.
    *   1초 간격으로 상태를 확인(Polling)하며 대기.
    *   *Note: 실제 클라이언트는 SSE를 사용하지만, k6 HTTP 부하 테스트에서는 Polling 방식으로 상태 확인을 시뮬레이션.*
3.  **Proceed (입장)**:
    *   서버 스케줄러에 의해 상태가 변경되면 루프를 탈출하고 테스트 종료.

---

## 7.3 모니터링 및 결과 해석

테스트 진행 중 `http://localhost:8080/monitor` 페이지를 통해 시스템 상태를 실시간으로 관제합니다.

#### 그래프 패턴 해석 (정상 동작 예시)

1.  **급상승 구간 (Traffic Burst)**
    *   **🟡 WAITING (노란색)** 그래프가 시작과 동시에 3,000까지 수직 상승합니다.
    *   모든 유저가 정상적으로 대기열에 등록되었음을 의미합니다.

2.  **안정적 처리 구간 (Stable Processing)**
    *   **🟡 WAITING**은 초당 일정량(예: 10명)씩 감소합니다.
    *   **🟢 PROCEEDING (초록색)**은 일정 수치(예: 50~100명)를 유지하며 평탄한 그래프를 그립니다.
    *   서버가 과부하 걸리지 않고 일정한 속도로 유저를 들여보내고 있음을 의미합니다.

3.  **완료 및 소진 (Drain)**
    *   **🔵 DONE (파란색)** 그래프가 우상향하며 누적 처리량을 보여줍니다.
    *   결국 **WAITING**이 0이 되면 테스트가 성공적으로 종료됩니다.

#### 주의해야 할 이상 징후
*   **Waiting이 줄지 않음**: 스케줄러가 죽었거나 DB 락으로 인해 처리가 멈춘 상태.
*   **Proceeding이 급증함**: 처리 용량 제한 로직이 깨져서 서버가 폭주하는 상태.
*   **k6 에러율 증가**: DB Connection Pool 고갈 또는 타임아웃 발생.

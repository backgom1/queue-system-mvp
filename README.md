# queue-system

## 1. 프로젝트 개요 & 목표

### 프로젝트 개요
- 대규모 트래픽이 몰리는 예매 상황을 가정해 Redis 기반 대기열 시스템을 구현하고 검증하는 MVP입니다.

### 해결하려는 문제
- 짧은 시간에 많은 사용자가 동시에 진입할 때 애플리케이션이 대기열을 안정적으로 관리할 수 있는지 확인합니다.
- 입장 가능 사용자와 대기 사용자를 분리하고, 순번 조회 방식에 따른 운영 특성을 비교합니다.
- 부하 테스트와 모니터링을 통해 처리량, 대기열 소진 속도, 활성 사용자 수를 관찰합니다.

### 현재 구현 범위
- `v1`: SSE 기반 대기열 진입 및 순번 알림 실험
- `v2`: 토큰 기반 polling 대기열 API
- Redis 기반 대기열, 활성 토큰, 입장 티켓 관리
- Thymeleaf 기반 테스트 페이지 및 모니터링 대시보드
- k6 기반 부하 테스트 스크립트

## 2. 기술 스택

### 백엔드
- Java 21
- Spring Boot 4.0.1
- Spring Web MVC
- Spring Data JPA
- Spring Data Redis
- Thymeleaf
- Micrometer + Prometheus

### 데이터 저장소
- Redis
- H2
- PostgreSQL 드라이버 포함

### 테스트/관측
- JUnit 5
- k6
- Spring Boot Actuator

## 3. 아키텍처

현재 저장소는 단일 Spring Boot 애플리케이션으로 구성되어 있으며, 대기열 처리와 UI, 스케줄러, Redis 연동을 한 프로젝트 안에서 검증합니다.

### 처리 흐름
1. 사용자가 대기열 진입 API를 호출합니다.
2. 서버는 Redis ZSET 기반 대기열에 사용자를 등록하고 순번 또는 토큰을 반환합니다.
3. 스케줄러가 주기적으로 대기열 상위 사용자를 입장 가능 상태로 승격합니다.
4. 사용자는 SSE 또는 polling으로 자신의 상태를 조회합니다.
5. `/monitor` 화면에서 대기 인원, 활성 티켓, 활성 토큰 수를 확인합니다.

### 버전별 차이
- `v1`
  - `POST /api/v1/queue/enter`
  - `GET /api/v1/queue/connect`
  - SSE 기반 실험용 흐름
- `v2`
  - `POST /api/v2/queue/enter`
  - `GET /api/v2/queue/status`
  - 토큰 기반 polling 흐름
  - 현재 부하 테스트와 모니터링은 `v2` 기준으로 맞춰져 있습니다.

## 4. 주요 기능

### 4.1 대기열 진입
- 사용자를 콘텐츠별 대기열에 등록합니다.
- `v2`에서는 토큰을 발급하고 순번에 따라 다음 polling 간격을 다르게 반환합니다.

### 4.2 순번 조회
- `v1`은 SSE로 이벤트를 수신합니다.
- `v2`는 `status`, `rank`, `nextPollMs`, `redirectUrl`을 반환합니다.

### 4.3 스케줄링 기반 입장 처리
- 스케줄러가 1초 간격으로 대기열 상위 사용자를 꺼내 활성 티켓을 발급합니다.
- 현재 `v2` 스케줄러는 한 번에 최대 200명을 승격합니다.

### 4.4 모니터링
- `/monitor` 페이지에서 `WAITING`, `ACTIVE TICKETS`, `ACTIVE TOKENS`를 시각화합니다.
- Actuator와 Prometheus 연동을 위한 설정이 포함되어 있습니다.

## 5. API 설계

## 5.1 Queue API v1

### 대기열 진입
- 메서드: `POST`
- URL: `/api/v1/queue/enter`
- Content-Type: `application/json`

요청 본문

```json
{
  "userUuid": "1bda9804-9f4f-42c7-9054-c4d2df6375a5",
  "contentId": "concert-iu-2025",
  "deviceId": "device-001"
}
```

응답 예시: 바로 입장

```json
{
  "resultCode": "ENTRY_GRANTED",
  "message": "입장 가능!",
  "data": {
    "currentRank": 0,
    "queueToken": null,
    "accessToken": "temp-access-token",
    "redirectUrl": "https://ticket.api.com/api/v1/ticket/entry",
    "sseUrl": null
  }
}
```

응답 예시: 대기열 등록

```json
{
  "resultCode": "QUEUE_WAIT",
  "message": "대기열 등록이 필요합니다.",
  "data": {
    "currentRank": 151,
    "queueToken": "temp-queue-token",
    "accessToken": null,
    "redirectUrl": null,
    "sseUrl": "http://localhost:8080/api/v1/queue/connect"
  }
}
```

### SSE 연결
- 메서드: `GET`
- URL: `/api/v1/queue/connect?userUuid={userUuid}`
- 응답 타입: `text/event-stream`

### 순번 확인
- 메서드: `GET`
- URL: `/api/v1/queue/rank?userUuid={userUuid}&contentId={contentId}`

### 통계 조회
- 메서드: `GET`
- URL: `/api/v1/queue/stats`

## 5.2 Queue API v2

### 대기열 진입
- 메서드: `POST`
- URL: `/api/v2/queue/enter`
- Content-Type: `application/json`

요청 본문

```json
{
  "userUuid": "1bda9804-9f4f-42c7-9054-c4d2df6375a5",
  "contentId": "concert-iu-2025",
  "deviceId": "device-001"
}
```

응답 예시

```json
{
  "resultCode": "SUCCESS",
  "message": "Request successfully processed",
  "data": {
    "rank": 151,
    "nextPollMs": 3000,
    "token": "issued-token",
    "redirectUrl": "http://localhost:8080/api/v2/queue/status"
  }
}
```

### 상태 조회
- 메서드: `GET`
- URL: `/api/v2/queue/status?token={token}`

응답 예시

```json
{
  "resultCode": "SUCCESS",
  "message": "Request successfully processed",
  "data": {
    "status": "WAIT",
    "rank": 151,
    "nextPollMs": 3000,
    "token": "issued-token",
    "redirectUrl": "http://localhost:8080/api/v2/queue/status"
  }
}
```

상태 값
- `WAIT`: 대기 중
- `DONE`: 입장 가능 상태
- `EXPIRED`: 토큰 만료

### 통계 조회
- 메서드: `GET`
- URL: `/api/v2/queue/stats?contentId=concert-iu-2025`

응답 예시

```json
{
  "resultCode": "SUCCESS",
  "message": "Request successfully processed",
  "data": {
    "waiting": 1200,
    "activeTickets": 200,
    "activeTokens": 1200
  }
}
```

## 6. 프로젝트 구조

```text
src/main/java/learn/queuesystem
├── application
│   ├── dto
│   └── service
├── config
├── domain
│   ├── queue
│   └── ticket
├── infra
│   ├── redis
│   ├── repository
│   └── util
└── presentation
    ├── api
    ├── dto
    ├── exception
    └── ui
```

### 핵심 컴포넌트
- `QueueServiceV1`, `QueueServiceV2`: 대기열 진입과 상태 조회를 담당합니다.
- `WaitingQueueRepository`: Redis 자료구조 접근을 추상화합니다.
- `QueueSchedulerV2`: 대기열 상위 사용자를 주기적으로 활성 티켓 상태로 전환합니다.
- `SseEmitterService`: `v1` SSE 연결을 관리합니다.
- `IndexController`: 테스트 UI와 모니터 페이지를 제공합니다.

## 7. 실행 방법

### 사전 준비
- Java 21
- Redis 실행

### 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션 기본 주소
- `http://localhost:8080/`
- `http://localhost:8080/v2`
- `http://localhost:8080/monitor`

### 참고 사항
- 애플리케이션 시작 시 `ContentCreateStarter`가 Redis의 `queue:*`, `token:*`, `active:*` 키를 정리한 뒤 `concert-iu-2025` 콘텐츠를 등록합니다.
- 기본 Redis 설정은 `localhost:6379`입니다.

## 8. 부하 테스트 및 모니터링

이 프로젝트는 `k6`와 `/monitor` 화면을 사용해 대기열 시스템의 처리 흐름을 검증합니다.

### k6 실행

```bash
k6 run k6/load-test.js
k6 run k6/load-test-v2.js
k6 run k6/load-test-v2-ramp.js
```

환경변수 예시

```bash
API_ROOT=http://localhost:8080 REQUEST_TIMEOUT=30s k6 run k6/load-test-v2.js
API_ROOT=http://localhost:8080 REQUEST_TIMEOUT=30s k6 run k6/load-test-v2-ramp.js
```

### 모니터링 지표
- `WAITING`: 현재 대기열 인원 수
- `ACTIVE TICKETS`: 입장 가능 상태로 승격된 사용자 수
- `ACTIVE TOKENS`: 아직 유효한 queue polling 토큰 수

### v2 polling 간격 정책
- 순번 `1~100`: 1초
- 순번 `101~5000`: 3초
- 순번 `5001~10000`: 10초
- 순번 `10001+`: 15초

## 9. 테스트

```bash
./gradlew test
```

포함된 테스트 예시
- 대기열 도메인 테스트
- `QueueServiceV1` 테스트
- SSE 통합 테스트

## 10. 향후 개선 방향

- Ticket 서버와의 실제 연동 API 분리
- 토큰 및 redirect URL의 외부 설정화
- 콘텐츠별 정책 분리와 운영 파라미터화
- Redis/DB 기반 메트릭 분석 고도화

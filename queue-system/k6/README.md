# K6 Load Test Guide

## Prerequisites
- [K6](https://k6.io/docs/get-started/installation/)가 설치되어 있어야 합니다.
  - macOS: `brew install k6`
  - Windows: `choco install k6`

## Running the Test
서버(`QueueSystemApplication`)를 먼저 실행한 후, 아래 명령어를 실행하세요.

```bash
k6 run k6/load-test.js
```

v2(토큰 기반 status polling) 테스트는 아래 명령어를 사용하세요.

```bash
k6 run k6/load-test-v2.js
```

## Scenarios
1. **Token Issuance**: 가상 유저가 대기열 토큰을 발급받습니다 (`POST /token`).
2. **Status Polling**: 발급 후 1초 간격으로 3회 상태를 조회합니다 (`GET /token`).
3. **Load Profile**: 
   - 30초 동안 0 -> 50 VU 증가
   - 1분간 50 VU 유지
   - 30초 동안 0 VU 감소

## V2 Scenario
1. **Queue Enter**: 가상 유저가 `POST /api/v2/queue/enter`로 대기열 진입을 요청합니다.
2. **Token Polling**: 대기 상태면 `GET /api/v2/queue/status?token=...`을 반복 호출합니다.
3. **Dynamic Interval**: 응답의 `nextPollMs` 값을 사용해 다음 polling 간격을 동적으로 조절합니다.

## V2 Monitor Metrics
`/monitor` 화면(v2 기준)에서 아래 지표를 확인할 수 있습니다.

1. **WAITING**
   - `queue:wait{contentId}` 대기열(ZSET) 인원 수
2. **ACTIVE TICKETS**
   - `queue:contents:active:{contentId}:{userId}` 키 개수
   - 스케줄러가 승격해 입장 가능 상태가 된 사용자 수(짧은 TTL)
3. **ACTIVE TOKENS**
   - `token:wait:*` 키 개수
   - 현재 유효한 queue polling 토큰 수

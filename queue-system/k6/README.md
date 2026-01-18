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

## Scenarios
1. **Token Issuance**: 가상 유저가 대기열 토큰을 발급받습니다 (`POST /token`).
2. **Status Polling**: 발급 후 1초 간격으로 3회 상태를 조회합니다 (`GET /token`).
3. **Load Profile**: 
   - 30초 동안 0 -> 50 VU 증가
   - 1분간 50 VU 유지
   - 30초 동안 0 VU 감소
```

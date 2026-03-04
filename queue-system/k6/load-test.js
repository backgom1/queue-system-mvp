import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  scenarios: {
    burst_traffic: {
      executor: 'per-vu-iterations',
      vus: 10000,               // 500명의 유저가 동시에 (거의 동시에)
      iterations: 1,          // 딱 1번만 실행하고 종료 (계속 새로운 유저 생성 X)
      maxDuration: '10m',     // 테스트 최대 시간 (모두 처리될 때까지 넉넉히)
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v1/queue';
const CONTENT_ID = 'concert-iu-2025';

export default function () {
  const userUuid = uuidv4();
  
  // 1. 대기열 진입 (Enter)
  const payload = JSON.stringify({
    userUuid: userUuid,
    contentId: CONTENT_ID,
    deviceId: 'k6-test-device'
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const enterRes = http.post(`${BASE_URL}/enter`, payload, params);

  if (enterRes.status !== 200) {
    console.error(`Failed to enter queue: ${enterRes.status}`);
    return;
  }

  let body = enterRes.json();
  
  // 만약 바로 입장되었다면 종료
  if (body.resultCode === 'ENTRY_GRANTED') {
    return;
  }

  // 2. 대기 상태라면 입장이 될 때까지 폴링 (Waiting Loop)
  // 실제 유저는 SSE를 유지하지만, k6 HTTP 모듈 한계상 주기적 호출로 대체
  let isWaiting = true;
  let attempt = 0;

  while (isWaiting) {
    sleep(1); // 1초 대기
    attempt++;

    // 순번 확인 (새로 추가된 /rank API 사용)
    const pollRes = http.get(`${BASE_URL}/rank?userUuid=${userUuid}&contentId=${CONTENT_ID}`, params);
    
    if (pollRes.status === 200) {
      const pollBody = pollRes.json();
      
      // 입장 허가!
      if (pollBody.resultCode === 'ENTRY_GRANTED') {
        isWaiting = false;
        // console.log(`User ${userUuid} entered after ${attempt} seconds`);
      } 
      // 여전히 대기 중
      else if (pollBody.resultCode === 'QUEUE_WAIT') {
         // 계속 대기
         // 만약 rank가 0이고 resultCode가 WAIT라면? -> 재진입 로직 필요할 수도 있지만 일단 대기
      }
    } else {
      // 에러 발생 시 루프 탈출
      isWaiting = false;
    }

    // 무한 루프 방지 (예: 5분 지나면 포기)
    if (attempt > 300) {
      isWaiting = false;
    }
  }
}

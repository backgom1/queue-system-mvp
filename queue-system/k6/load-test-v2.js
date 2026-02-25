import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  scenarios: {
    burst_traffic_v2: {
      executor: 'per-vu-iterations',
      vus: 5000,
      iterations: 1,
      maxDuration: '10m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v2/queue';
const CONTENT_ID = 'concert-iu-2025';
const MAX_POLL_ATTEMPTS = 300;

function toSeconds(nextPollMs) {
  if (!Number.isFinite(nextPollMs) || nextPollMs <= 0) return 1;
  return Math.max(nextPollMs / 1000, 0.2);
}

export default function () {
  const userUuid = uuidv4();
  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const payload = JSON.stringify({
    userUuid,
    contentId: CONTENT_ID,
    deviceId: 'k6-test-device',
  });

  // 1) Enter queue
  const enterRes = http.post(`${BASE_URL}/enter`, payload, params);
  check(enterRes, {
    'v2 enter status is 200': (r) => r.status === 200,
  });

  if (enterRes.status !== 200) {
    return;
  }

  const enterBody = enterRes.json();
  const enterData = enterBody && enterBody.data ? enterBody.data : null;
  if (!enterData) {
    return;
  }

  // rank=0 or nextPollMs=0이면 즉시 입장으로 간주
  if (enterData.rank === 0 || enterData.nextPollMs === 0) {
    return;
  }

  let token = enterData.token;
  let nextPollMs = enterData.nextPollMs;
  let attempts = 0;

  // 2) Poll status until granted / done / invalid
  while (attempts < MAX_POLL_ATTEMPTS && token) {
    sleep(toSeconds(nextPollMs));
    attempts += 1;

    const statusRes = http.get(
      `${BASE_URL}/status?token=${encodeURIComponent(token)}`,
      params
    );

    check(statusRes, {
      'v2 status is 200': (r) => r.status === 200,
    });

    if (statusRes.status !== 200) {
      break;
    }

    const statusBody = statusRes.json();
    const statusData = statusBody && statusBody.data ? statusBody.data : null;
    if (!statusData) {
      break;
    }

    if (statusData.token) {
      token = statusData.token;
    }

    // 현재 구현 기준 DONE이면 입장 가능 상태
    if (statusData.status === 'DONE') {
      break;
    }

    if (statusData.rank === 0 || statusData.nextPollMs === 0) {
      break;
    }

    nextPollMs = statusData.nextPollMs;
  }
}

package learn.queuesystem.presentation.api.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    // UserUuid -> SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 60 * 1000L; // 1분

    public SseEmitter connect(String userUuid) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userUuid, emitter);

        // 연결 종료/타임아웃 시 제거
        emitter.onCompletion(() -> emitters.remove(userUuid));
        emitter.onTimeout(() -> emitters.remove(userUuid));
        emitter.onError((e) -> emitters.remove(userUuid));

        // 초기 연결 성공 메시지 전송 (명세에 맞게 JSON 형태가 좋지만 일단 텍스트)
        send(userUuid, "connected", "Connection Established");

        return emitter;
    }

    public void send(String userUuid, String name, Object data) {
        SseEmitter emitter = emitters.get(userUuid);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(name)
                    .data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE to user {}", userUuid);
                emitters.remove(userUuid);
            }
        }
    }
    
    // 현재 접속중인 모든 사용자 ID 반환 (알림 발송용)
    public Map<String, SseEmitter> getEmitters() {
        return emitters;
    }
}

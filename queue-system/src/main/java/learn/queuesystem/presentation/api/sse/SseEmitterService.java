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

    // UserId -> SseEmitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 60 * 1000L; // 1분

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);

        // 연결 종료/타임아웃 시 제거
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        // 초기 연결 성공 메시지 전송
        send(userId, "Connected", "Connection Established");

        return emitter;
    }

    public void send(Long userId, String name, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(name)
                    .data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE to user {}", userId);
                emitters.remove(userId);
            }
        }
    }
    
    // 현재 접속중인 모든 사용자 ID 반환 (알림 발송용)
    public Map<Long, SseEmitter> getEmitters() {
        return emitters;
    }
}

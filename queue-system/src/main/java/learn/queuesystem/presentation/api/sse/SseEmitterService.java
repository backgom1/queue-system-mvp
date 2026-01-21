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

    /**
     * 사용자 정보를 담는 내부 클래스
     */
    public record UserSession(String userUuid, String contentId, SseEmitter emitter) {}

    // UserUuid -> UserSession
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 60 * 1000L; // 1분

    public SseEmitter connect(String userUuid, String contentId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        sessions.put(userUuid, new UserSession(userUuid, contentId, emitter));

        emitter.onCompletion(() -> sessions.remove(userUuid));
        emitter.onTimeout(() -> sessions.remove(userUuid));
        emitter.onError((e) -> sessions.remove(userUuid));

        send(userUuid, "connected", "Connection Established");

        return emitter;
    }

    public void send(String userUuid, String name, Object data) {
        UserSession session = sessions.get(userUuid);
        if (session != null) {
            try {
                session.emitter().send(SseEmitter.event()
                    .name(name)
                    .data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE to user {}", userUuid);
                sessions.remove(userUuid);
            }
        }
    }
    
    public Map<String, UserSession> getSessions() {
        return sessions;
    }
}
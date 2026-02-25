package learn.queuesystem.config;


import jakarta.annotation.PostConstruct;
import learn.queuesystem.domain.ticket.ContentRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class ContentCreateStarter {

    private final StringRedisTemplate redisTemplate;
    private final ContentRepository contentRepository;

    public ContentCreateStarter(StringRedisTemplate redisTemplate, ContentRepository contentRepository) {
        this.redisTemplate = redisTemplate;
        this.contentRepository = contentRepository;
    }


    @PostConstruct
    public void init() {

        Set<String> keysToDelete = new HashSet<>();
        addKeys(keysToDelete, "queue:*");
        addKeys(keysToDelete, "token:*");
        addKeys(keysToDelete, "active:*");

        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }

        contentRepository.saveContent("concert-iu-2025");
    }

    private void addKeys(Set<String> keysToDelete, String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            keysToDelete.addAll(keys);
        }
    }
}

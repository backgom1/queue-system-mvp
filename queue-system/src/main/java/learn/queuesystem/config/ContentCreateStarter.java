package learn.queuesystem.config;


import jakarta.annotation.PostConstruct;
import learn.queuesystem.domain.ticket.ContentRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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

        Set<String> keys = redisTemplate.keys("queue:*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        contentRepository.saveContent("concert-iu-2025");
    }
}

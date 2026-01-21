package learn.queuesystem.domain.ticket;

import java.util.Set;

public interface ContentRepository {
    void saveContent(String content);
    Set<String> getActiveContentIds();
    void removeContent(String contentId);
}

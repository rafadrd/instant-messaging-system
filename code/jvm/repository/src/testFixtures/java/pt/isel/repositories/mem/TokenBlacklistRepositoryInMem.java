package pt.isel.repositories.mem;

import pt.isel.repositories.security.TokenBlacklistRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBlacklistRepositoryInMem implements TokenBlacklistRepository {
    private final Map<String, LocalDateTime> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String jti, LocalDateTime expiresAt) {
        blacklist.put(jti, expiresAt);
    }

    @Override
    public boolean exists(String jti) {
        return blacklist.containsKey(jti);
    }

    @Override
    public void cleanupExpired(LocalDateTime now) {
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    @Override
    public void clear() {
        blacklist.clear();
    }
}
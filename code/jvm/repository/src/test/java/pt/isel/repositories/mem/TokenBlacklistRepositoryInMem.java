package pt.isel.repositories.mem;

import pt.isel.repositories.security.TokenBlacklistRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class TokenBlacklistRepositoryInMem implements TokenBlacklistRepository {
    private final Set<String> blacklist = new HashSet<>();

    @Override
    public void add(String jti, LocalDateTime expiresAt) {
        blacklist.add(jti);
    }

    @Override
    public boolean exists(String jti) {
        return blacklist.contains(jti);
    }

    @Override
    public void cleanupExpired() {
    }

    @Override
    public void clear() {
        blacklist.clear();
    }
}
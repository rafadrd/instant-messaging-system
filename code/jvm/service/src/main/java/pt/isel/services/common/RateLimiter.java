package pt.isel.services.common;

import java.time.Duration;

public interface RateLimiter {
    boolean isRateLimited(String action, String identifier, int limit, Duration window);
}
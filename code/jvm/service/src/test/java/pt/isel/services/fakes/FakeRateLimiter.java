package pt.isel.services.fakes;

import pt.isel.services.common.RateLimiter;

import java.time.Duration;

public class FakeRateLimiter implements RateLimiter {
    private boolean rateLimited = false;

    public void setRateLimited(boolean rateLimited) {
        this.rateLimited = rateLimited;
    }

    @Override
    public boolean isRateLimited(String action, String identifier, int limit, Duration window) {
        return rateLimited;
    }
}
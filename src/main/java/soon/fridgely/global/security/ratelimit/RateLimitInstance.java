package soon.fridgely.global.security.ratelimit;

public enum RateLimitInstance {
    LOGIN, REGISTER, AUTH;

    public String key() {
        return name().toLowerCase();
    }

}
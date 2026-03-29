package soon.fridgely.global.security.ratelimit;

import java.util.Locale;

public enum RateLimitInstance {
    LOGIN, REGISTER, AUTH;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

}
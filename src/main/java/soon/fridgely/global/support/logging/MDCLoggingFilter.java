package soon.fridgely.global.support.logging;

import jakarta.servlet.*;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MDCLoggingFilter implements Filter {

    private static final String TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            String traceId = generateTraceId();
            MDC.put(TRACE_ID, traceId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

}
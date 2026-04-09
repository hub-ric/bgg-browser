package com.bggbrowser.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = request.getRemoteAddr();
        RequestCounter counter = counters.computeIfAbsent(ip, k -> new RequestCounter());

        if (counter.increment() > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            return;
        }
        chain.doFilter(req, res);
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        int increment() {
            long now = System.currentTimeMillis();
            if (now - windowStart.get() > 60_000) {
                count.set(0);
                windowStart.set(now);
            }
            return count.incrementAndGet();
        }
    }
}

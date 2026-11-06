package com.sams.filter;

import com.sams.dao.ApiKeyDAO;
import com.sams.model.ApiKey;
import com.sams.util.ApiKeyUtil;
import com.sams.util.JsonUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Guards the REST API.  A client must send its key in the header
 *     X-API-KEY: sams_xxxxxxxx      (or  Authorization: Bearer sams_xxxxxxxx)
 * The key is hashed with SHA-256 and compared with api_keys.key_hash.
 * Each key may call the API at most 120 times per minute.
 */
@WebFilter(urlPatterns = "/api/v1/*")
public class ApiKeyFilter implements Filter {
    private static final int LIMIT_PER_MINUTE = 120;
    private final ApiKeyDAO dao = new ApiKeyDAO();
    private final Map<Long, long[]> windows = new ConcurrentHashMap<>();   // keyId -> {windowStart, count}

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;

        if (req.getRequestURI().endsWith("/api/v1/health")) {       // public heartbeat
            chain.doFilter(rq, rs);
            return;
        }
        String key = req.getHeader("X-API-KEY");
        if (key == null) {
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) key = auth.substring(7).trim();
        }
        if (key == null || key.isEmpty()) { error(resp, 401, "Missing API key. Send it in the X-API-KEY header."); return; }

        try {
            ApiKey k = dao.findActiveByHash(ApiKeyUtil.sha256(key));
            if (k == null) { error(resp, 401, "Invalid or revoked API key."); return; }
            if (!allow(k.getId())) {
                resp.setHeader("Retry-After", "60");
                error(resp, 429, "Rate limit exceeded (" + LIMIT_PER_MINUTE + " requests per minute).");
                return;
            }
            dao.touch(k.getId());
            req.setAttribute("apiKey", k);
        } catch (SQLException e) {
            error(resp, 500, "Database error.");
            return;
        }
        chain.doFilter(rq, rs);
    }

    private boolean allow(long keyId) {
        long now = System.currentTimeMillis();
        long[] w = windows.computeIfAbsent(keyId, id -> new long[]{now, 0});
        synchronized (w) {
            if (now - w[0] > 60_000) { w[0] = now; w[1] = 0; }
            return ++w[1] <= LIMIT_PER_MINUTE;
        }
    }

    static void error(HttpServletResponse resp, int status, String msg) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", msg);
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(body));
    }
}

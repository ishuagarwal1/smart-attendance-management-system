package com.sams.filter;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Runs on every request:
 *  1. forces UTF-8,
 *  2. adds security headers (clickjacking, MIME sniffing, CSP),
 *  3. protects every POST form with a per-session CSRF token.
 * The REST API (/api/*) is not cookie based, so it is exempt from CSRF.
 */
@WebFilter(urlPatterns = "/*")
public class SecurityFilter implements Filter {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) rq;
        HttpServletResponse resp = (HttpServletResponse) rs;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");
        resp.setHeader("Content-Security-Policy",
            "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; frame-ancestors 'none'");

        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean isApi = path.startsWith("/api/");
        boolean isStatic = path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");

        if (!isApi && !isStatic) {
            HttpSession session = req.getSession(true);
            String token = (String) session.getAttribute("csrfToken");
            if (token == null) {
                token = new BigInteger(192, RANDOM).toString(36);
                session.setAttribute("csrfToken", token);
            }
            if ("POST".equalsIgnoreCase(req.getMethod())) {
                String sent = req.getParameter("_csrf");
                if (sent == null || !MessageDigest.isEqual(sent.getBytes(), token.getBytes())) {
                    resp.sendError(403, "Security token missing or invalid. Please go back, reload the page and try again.");
                    return;
                }
            }
        }
        if (!isStatic) {                       // dynamic pages must never be cached (back-button after logout)
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        }
        chain.doFilter(rq, rs);
    }
}

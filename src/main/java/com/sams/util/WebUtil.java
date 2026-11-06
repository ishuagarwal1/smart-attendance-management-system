package com.sams.util;

import com.sams.model.User;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** Small helpers shared by the servlets. */
public final class WebUtil {
    private WebUtil() { }

    public static User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute("user");
    }

    public static String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isEmpty()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }

    /** Flash message shown once on the next page (type = success | error | info). */
    public static void flash(HttpServletRequest req, String type, String message) {
        HttpSession s = req.getSession();
        s.setAttribute("flashType", type);
        s.setAttribute("flashMsg", message);
    }

    public static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    public static LocalDate parseDate(String s, LocalDate def) {
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException | NullPointerException e) { return def; }
    }

    public static Date sqlDate(LocalDate d) { return Date.valueOf(d); }

    public static String trim(String s) { return s == null ? "" : s.trim(); }

    public static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** Escapes one CSV cell (used by the export servlet). */
    public static String csv(Object v) {
        String s = v == null ? "" : v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        // neutralise spreadsheet formula injection
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+(\\.\\d+)?")) s = "'" + s;
        return s;
    }
}

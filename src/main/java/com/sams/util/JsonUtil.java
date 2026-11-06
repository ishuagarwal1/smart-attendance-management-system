package com.sams.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON writer + parser (no external library needed).
 * parse() returns Map / List / String / Double / Boolean / null.
 */
public final class JsonUtil {
    private JsonUtil() { }

    // --------------------------------------------------------------- writer
    public static String toJson(Object o) {
        StringBuilder sb = new StringBuilder();
        write(sb, o);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object o) {
        if (o == null) { sb.append("null"); }
        else if (o instanceof Number || o instanceof Boolean) { sb.append(o); }
        else if (o instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Collection) {
            sb.append('[');
            boolean first = true;
            for (Object x : (Collection<?>) o) {
                if (!first) sb.append(',');
                first = false;
                write(sb, x);
            }
            sb.append(']');
        } else { quote(sb, o.toString()); }      // String, Date, Timestamp, enums ...
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        sb.append('"');
    }

    // --------------------------------------------------------------- parser
    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (p.pos != text.length()) throw new IllegalArgumentException("Unexpected trailing data at " + p.pos);
        return v;
    }

    private static final class Parser {
        final String s;
        int pos;
        Parser(String s) { this.s = s; }

        void skipWs() { while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++; }

        char peek() {
            if (pos >= s.length()) throw new IllegalArgumentException("Unexpected end of JSON");
            return s.charAt(pos);
        }

        void expect(char c) {
            if (peek() != c) throw new IllegalArgumentException("Expected '" + c + "' at " + pos);
            pos++;
        }

        Object value() {
            skipWs();
            char c = peek();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", pos))  { pos += 4; return Boolean.TRUE; }
            if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            if (s.startsWith("null", pos))  { pos += 4; return null; }
            return number();
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            expect('{'); skipWs();
            if (peek() == '}') { pos++; return m; }
            while (true) {
                skipWs();
                String k = string();
                skipWs(); expect(':');
                m.put(k, value());
                skipWs();
                if (peek() == ',') { pos++; continue; }
                expect('}');
                return m;
            }
        }

        List<Object> array() {
            List<Object> l = new ArrayList<>();
            expect('['); skipWs();
            if (peek() == ']') { pos++; return l; }
            while (true) {
                l.add(value());
                skipWs();
                if (peek() == ',') { pos++; continue; }
                expect(']');
                return l;
            }
        }

        String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = peek();
                pos++;
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char e = peek();
                pos++;
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: sb.append(e);
                }
            }
        }

        Double number() {
            int start = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
            if (start == pos) throw new IllegalArgumentException("Unexpected character at " + pos);
            return Double.valueOf(s.substring(start, pos));
        }
    }
}

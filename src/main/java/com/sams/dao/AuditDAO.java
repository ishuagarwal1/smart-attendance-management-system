package com.sams.dao;

import com.sams.model.AuditEntry;
import com.sams.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Writes and reads the audit trail. Logging never breaks the main request. */
public class AuditDAO {

    public void log(Long userId, String action, String details, String ip) {
        String sql = "INSERT INTO audit_log (user_id, action, details, ip_address) VALUES (?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, userId);
            ps.setString(2, action);
            ps.setString(3, details != null && details.length() > 400 ? details.substring(0, 400) : details);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SAMS] audit log failed: " + e.getMessage());
        }
    }

    public List<AuditEntry> recent(int limit) throws SQLException {
        String sql = "SELECT l.id, u.username, l.action, l.details, l.ip_address, l.created_at "
                   + "FROM audit_log l LEFT JOIN users u ON u.id = l.user_id ORDER BY l.id DESC LIMIT ?";
        List<AuditEntry> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditEntry a = new AuditEntry();
                    a.setId(rs.getLong("id"));
                    a.setUserName(rs.getString("username"));
                    a.setAction(rs.getString("action"));
                    a.setDetails(rs.getString("details"));
                    a.setIp(rs.getString("ip_address"));
                    a.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(a);
                }
            }
        }
        return list;
    }
}

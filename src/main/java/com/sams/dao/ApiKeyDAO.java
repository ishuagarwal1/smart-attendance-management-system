package com.sams.dao;

import com.sams.model.ApiKey;
import com.sams.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Data access for API keys (only SHA-256 hashes are stored). */
public class ApiKeyDAO {

    private ApiKey map(ResultSet rs) throws SQLException {
        ApiKey k = new ApiKey();
        k.setId(rs.getLong("id"));
        k.setLabel(rs.getString("label"));
        k.setKeyPrefix(rs.getString("key_prefix"));
        k.setActive(rs.getBoolean("active"));
        k.setCreatedAt(rs.getTimestamp("created_at"));
        k.setLastUsedAt(rs.getTimestamp("last_used_at"));
        return k;
    }

    public void create(String label, String prefix, String hash, long createdBy) throws SQLException {
        String sql = "INSERT INTO api_keys (label, key_prefix, key_hash, created_by) VALUES (?,?,?,?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, label);
            ps.setString(2, prefix);
            ps.setString(3, hash);
            ps.setLong(4, createdBy);
            ps.executeUpdate();
        }
    }

    public ApiKey findActiveByHash(String hash) throws SQLException {
        String sql = "SELECT id, label, key_prefix, active, created_at, last_used_at FROM api_keys WHERE key_hash = ? AND active = 1";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<ApiKey> findAll() throws SQLException {
        List<ApiKey> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, label, key_prefix, active, created_at, last_used_at FROM api_keys ORDER BY created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void touch(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE api_keys SET last_used_at = NOW() WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void setActive(long id, boolean active) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE api_keys SET active = ? WHERE id = ?")) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM api_keys WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}

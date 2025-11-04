package com.example.repository;

import com.example.connection.DatabaseConnection; 
import com.example.model.ChatMessage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {
    private static final String INSERT_MESSAGE = "INSERT INTO messages (sender_id, receiver_id, content, sent_time) VALUES (?, ?, ?, ?)";
    private static final String SELECT_HISTORY = 
        "SELECT id, sender_id, receiver_id, content, sent_time FROM messages " +
        "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
        "ORDER BY sent_time ASC";
    private static final String SELECT_PENDING = 
        "SELECT id, sender_id, receiver_id, content, sent_time FROM messages " +
        "WHERE receiver_id = ? AND is_sent = 0 ORDER BY sent_time ASC";
    private static final String MARK_AS_SENT = "UPDATE messages SET is_sent = 1 WHERE id = ?";

    public void saveMessage(ChatMessage message) throws SQLException {
        Connection con = DatabaseConnection.getInstance().getConnection(); 
        try (PreparedStatement ps = con.prepareStatement(INSERT_MESSAGE)) {
            ps.setInt(1, message.getSenderId());
            ps.setInt(2, message.getReceiverId());
            ps.setString(3, message.getContent());
            ps.setTimestamp(4, message.getSentTime());
            ps.executeUpdate();
        } 
    }
    
    public List<ChatMessage> getHistory(int userAId, int userBId) throws SQLException {
        List<ChatMessage> history = new ArrayList<>();
        Connection con = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = con.prepareStatement(SELECT_HISTORY)) {
            // Đảm bảo lấy cả 2 chiều A->B và B->A
            ps.setInt(1, userAId);
            ps.setInt(2, userBId);
            ps.setInt(3, userBId);
            ps.setInt(4, userAId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new ChatMessage(
                        rs.getLong("id"), rs.getInt("sender_id"), rs.getInt("receiver_id"), 
                        rs.getString("content"), rs.getTimestamp("sent_time")));
                }
            }
        }
        return history;
    }

    // Lấy tin nhắn chưa gửi/chờ
    public List<ChatMessage> getPendingMessages(int receiverId) throws SQLException {
        List<ChatMessage> pending = new ArrayList<>();
        Connection con = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement ps = con.prepareStatement(SELECT_PENDING)) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pending.add(new ChatMessage(
                        rs.getLong("id"), rs.getInt("sender_id"), rs.getInt("receiver_id"), 
                        rs.getString("content"), rs.getTimestamp("sent_time")));
                }
            }
        }
        return pending;
    }

    // Đánh dấu tin nhắn đã gửi
    public void markAsSent(long messageId) throws SQLException {
        Connection con = DatabaseConnection.getInstance().getConnection(); 
        try (PreparedStatement ps = con.prepareStatement(MARK_AS_SENT)) {
            ps.setLong(1, messageId);
            ps.executeUpdate();
        }
    }
}
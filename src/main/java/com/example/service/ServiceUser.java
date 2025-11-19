package com.example.service;

import com.example.connection.DatabaseConnection;
import com.example.model.*;
import com.example.app.MessageType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser {

    private final Connection connection;

    public ServiceUser() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public ModelMessage register(ModelRegister data) {
        ModelMessage message = new ModelMessage();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(CHECK_USER);
            preparedStatement.setString(1, data.getUserName());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                message.setAction(false);
                message.setMessage("User Already Exist");
            } else {
                message.setAction(true);
            }
            resultSet.close();
            preparedStatement.close();

            if (message.isAction()) {
                connection.setAutoCommit(false);

                preparedStatement = connection.prepareStatement(INSERT_USER, PreparedStatement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, data.getUserName());
                preparedStatement.setString(2, data.getPassword());
                int rows = preparedStatement.executeUpdate();
                if (rows <= 0) {
                    message.setAction(false);
                    message.setMessage("Insert failed, no rows affected");
                }

                ResultSet keys = preparedStatement.getGeneratedKeys();
                keys.next();
                int userID = keys.getInt(1);
                keys.close();
                preparedStatement.close();

                preparedStatement = connection.prepareStatement(INSERT_USER_ACCOUNT);
                preparedStatement.setInt(1, userID);
                preparedStatement.setString(2, data.getUserName());
                int rows2 = preparedStatement.executeUpdate();
                preparedStatement.close();
                if (rows2 <= 0) {
                    message.setAction(false);
                    message.setMessage("Insert failed, no rows affected");
                }

                connection.commit();
                connection.setAutoCommit(true);
                message.setAction(true);
                message.setMessage("Ok");
                message.setData(new ModelUserAccount(userID, data.getUserName(), "", "", true));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            message.setAction(false);
            message.setMessage("Server Error: " + e.getMessage());
            try {
                if (!connection.getAutoCommit()) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return message;
    }

    public ModelUserAccount login(ModelLogin login) throws SQLException {
        ModelUserAccount data = null;
        PreparedStatement preparedStatement = connection.prepareStatement(LOGIN);
        preparedStatement.setString(1, login.getUserName());
        preparedStatement.setString(2, login.getPassword());
        ResultSet rs = preparedStatement.executeQuery();
        if (rs.next()) {
            int userID = rs.getInt(1);
            String userName = rs.getString(2);
            String gender = rs.getString(3);
            String image = rs.getString(4);
            data = new ModelUserAccount(userID, userName, gender, image, checkUserStatus(userID));
        }
        rs.close();
        preparedStatement.close();
        return data;
    }

    public List<ModelUserAccount> getUser(int existUser) throws SQLException {
        List<ModelUserAccount> list = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_USER_ACCOUNT);
        preparedStatement.setInt(1, existUser);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            int userID = rs.getInt(1);
            String userName = rs.getString(2);
            String gender = rs.getString(3);
            String image = rs.getString(4);
            list.add(new ModelUserAccount(userID, userName, gender, image, checkUserStatus(userID)));
        }
        rs.close();
        preparedStatement.close();
        return list;
    }

    public List<ModelReceiveMessage> getMessageHistory(int fromUserID, int toUserID) throws SQLException {
        List<ModelReceiveMessage> history = new ArrayList<>();
        String sql = "SELECT MessageType, FromUserID, ToUserID, TextContent, FileID " +
                     "FROM messages WHERE (FromUserID=? AND ToUserID=?) OR (FromUserID=? AND ToUserID=?) " +
                     "ORDER BY MessageID ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, fromUserID);
            ps.setInt(2, toUserID);
            ps.setInt(3, toUserID);
            ps.setInt(4, fromUserID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int type = rs.getInt("MessageType");
                int from = rs.getInt("FromUserID");
                String text = rs.getString("TextContent");
                Integer fileID = rs.getInt("FileID");
                ModelReceiveImage imageData = null;
                if (!rs.wasNull() && (type == MessageType.FILE.getValue() || type == MessageType.IMAGE.getValue())) {
                    imageData = new ModelReceiveImage();
                    imageData.setFileID(fileID);
                }
                history.add(new ModelReceiveMessage(type, from, text, imageData));
            }
        }
        return history;
    }

    private boolean checkUserStatus(int userID) {
        List<ModelClient> clients = Service.getInstance(null).getListClient();
        for (ModelClient client : clients) {
            if (client.getUser().getUserID() == userID) return true;
        }
        return false;
    }

    private final String LOGIN = "SELECT UserID, user_account.UserName, Gender, ImageString " +
                                 "FROM `user` JOIN `user_account` USING(UserID) " +
                                 "WHERE `user`.`UserName`=BINARY(?) AND `user`.`Password`=BINARY(?) AND `user_account`.`Status`='1'";
    private final String SELECT_USER_ACCOUNT = "SELECT UserID, UserName, Gender, ImageString FROM user_account WHERE user_account.`Status`='1' AND UserID<>?";
    private final String INSERT_USER = "INSERT INTO `user` (UserName, `Password`) VALUES (?,?)";
    private final String INSERT_USER_ACCOUNT = "INSERT INTO `user_account` (UserID, UserName) VALUES(?,?)";
    private final String CHECK_USER = "SELECT UserID FROM user WHERE UserName = ? LIMIT 1";

}

package com.example.service;

import com.example.connection.DatabaseConnection;
import com.example.model.ModelClient;
import com.example.model.ModelLogin;
import com.example.model.ModelMessage;
import com.example.model.ModelRegister;
import com.example.model.ModelUserAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser {

    public ServiceUser() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public ModelMessage register(ModelRegister data) {
        // Check if user exist
        ModelMessage message = new ModelMessage();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(CHECK_USER);
            preparedStatement.setString(1, data.getUserName());
            ResultSet resultSet = preparedStatement.executeQuery();
            // Use next() which works with forward-only result sets
            if (resultSet.next()) {
                message.setAction(false);
                message.setMessage("User Already Exist");
            } else {
                message.setAction(true);
            }
            resultSet.close();
            preparedStatement.close();
            if (message.isAction()) {
                // Insert User Register
                connection.setAutoCommit(false);
                preparedStatement = connection.prepareStatement(INSERT_USER, preparedStatement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, data.getUserName());
                preparedStatement.setString(2, data.getPassword());
                int rows = preparedStatement.executeUpdate();
                if (rows <= 0) {
                    message.setAction(false);
                    message.setMessage("Insert failed, no rows affected");
                }
                resultSet = preparedStatement.getGeneratedKeys();
                resultSet.first();
                int userID = resultSet.getInt(1);
                resultSet.close();
                preparedStatement.close();

                // Create user account (insert into user_account)
                preparedStatement = connection.prepareStatement(INSERT_USER_ACCOUNT);
                preparedStatement.setInt(1, userID);
                preparedStatement.setString(2, data.getUserName());
                int rows_ = preparedStatement.executeUpdate();
                preparedStatement.close();
                if (rows_ <= 0) {
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
            // Return the DB error message to the caller for easier troubleshooting
            message.setMessage("Server Error: " + e.getMessage());
            try {
                if (connection.getAutoCommit() == false) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e1) {

            }
        }

        return message;
    }

    public ModelUserAccount login(ModelLogin login) throws SQLException {
        ModelUserAccount data = null;
        PreparedStatement preparedStatement = connection.prepareStatement(LOGIN);
        preparedStatement.setString(1, login.getUserName());
        preparedStatement.setString(2, login.getPassword());
        ResultSet resultSet = preparedStatement.executeQuery();
        // Use next() instead of first() because the default result set is forward-only.
        // Calling first() may throw an exception or be unsupported; next() works reliably.
        if (resultSet.next()) {
            int userID = resultSet.getInt(1);
            String userName = resultSet.getString(2);
            String gender = resultSet.getString(3);
            String image = resultSet.getString(4);
            data = new ModelUserAccount(userID, userName, gender, image, checkUserStatus(userID));
        }
        resultSet.close();
        preparedStatement.close();
        return data;
    }

    public List<ModelUserAccount> getUser(int existUser) throws SQLException {
        List<ModelUserAccount> list = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_USER_ACCOUNT);
        preparedStatement.setInt(1, existUser);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            int userID = resultSet.getInt(1);
            String userName = resultSet.getString(2);
            String gender = resultSet.getString(3);
            String image = resultSet.getString(4);
            // Determine status based on whether the user has an active client connection
            list.add(new ModelUserAccount(userID, userName, gender, image, checkUserStatus(userID)));
        }
        resultSet.close();
        preparedStatement.close();
        return list;
    }

    private boolean checkUserStatus(int userID) {
        List<ModelClient> clients = Service.getInstance(null).getListClient();
        for (ModelClient client : clients) {
            if (client.getUser().getUserID() == userID) {
                return true;
            }
        }
        return false;
    }

    // SQL
    // Escape the table name to avoid conflicts with reserved words and be explicit
    private final String LOGIN = "SELECT UserID, user_account.UserName, Gender, ImageString FROM `user` JOIN `user_account` USING (UserID) WHERE `user`.`UserName`=BINARY(?) AND `user`.`Password`=BINARY(?) AND `user_account`.`Status`='1'";
    private final String SELECT_USER_ACCOUNT = "SELECT UserID, UserName, Gender, ImageString FROM user_account WHERE user_account.`Status`='1' AND UserID<>?";
    private final String INSERT_USER = "INSERT INTO `user` (UserName, `Password`) VALUES (?,?)";
    private final String INSERT_USER_ACCOUNT = "INSERT INTO `user_account` (UserID, UserName) VALUES(?,?)";
    private final String CHECK_USER = "select UserID from user where UserName = ? limit 1";

    // Instance
    private final Connection connection;
}

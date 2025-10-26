package com.example.service;

import com.example.connection.DatabaseConnection;
import com.example.model.ModelMessage;
import com.example.model.ModelRegister;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

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
                preparedStatement = connection.prepareStatement(INSERT_USER);
                preparedStatement.setString(1, data.getUserName());
                preparedStatement.setString(2, data.getPassword());
                int rows = preparedStatement.executeUpdate();
                preparedStatement.close();
                if (rows <= 0) {
                    message.setAction(false);
                    message.setMessage("Insert failed, no rows affected");
                }
                message.setAction(true);
                message.setMessage("Ok");
                
            }
        } catch (SQLException e) {
            // Log the exception so it's visible during debugging
            e.printStackTrace();
            message.setAction(false);
            // Return the DB error message to the caller for easier troubleshooting
            message.setMessage("Server Error: " + e.getMessage());
        }

        return message;
    }

    // SQL
    // Escape the table name to avoid conflicts with reserved words and be explicit
    private final String INSERT_USER = "INSERT INTO `user` (UserName, `Password`) VALUES (?,?)";
    private final String CHECK_USER = "select UserID from user where UserName = ? limit 1";

    // Instance
    private final Connection connection;
}

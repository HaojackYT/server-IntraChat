package com.example.service;

import com.corundumstudio.socketio.*;
import com.example.app.MessageType;
import com.example.connection.DatabaseConnection;
import com.example.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Service {

    private static Service instance;
    private SocketIOServer server;
    private ServiceUser serviceUser;
    private ServiceFile serviceFile;
    private List<ModelClient> listClient;
    private JTextArea textArea;
    private final int PORT_NUMBER = 9999;
    private Connection connection;

    public static Service getInstance(JTextArea textArea) {
        if (instance == null) {
            instance = new Service(textArea);
        }
        return instance;
    }

    private Service(JTextArea textArea) {
        this.textArea = textArea;
        serviceUser = new ServiceUser();
        serviceFile = new ServiceFile();
        listClient = new ArrayList<>();
        connection = DatabaseConnection.getInstance().getConnection();
    }

    public void startServer() {
        Configuration config = new Configuration();
        config.setPort(PORT_NUMBER);
        server = new SocketIOServer(config);

        // Connect listener
        server.addConnectListener(client -> {
            SwingUtilities.invokeLater(() -> textArea.append("Client connected: " + client.getSessionId() + "\n"));
        });

        // Disconnect listener
        server.addDisconnectListener(client -> {
            int userID = removeClient(client);
            if (userID != 0) userDisconnect(userID);
            SwingUtilities.invokeLater(() -> textArea.append("Client disconnected: " + client.getSessionId() + "\n"));
        });

        // REGISTER
        server.addEventListener("register", ModelRegister.class, (client, data, ackSender) -> {
            ModelMessage message = serviceUser.register(data);
            ackSender.sendAckData(message.isAction(), message.getMessage(), message.getData());
            if (message.isAction()) {
                addClient(client, (ModelUserAccount) message.getData());
            }
        });

        // LOGIN
        server.addEventListener("login", ModelLogin.class, (client, data, ackSender) -> {
            try {
                ModelUserAccount login = serviceUser.login(data);
                if (login != null) {
                    addClient(client, login); // add client ngay sau login
                    ackSender.sendAckData(true, login);
                    userConnect(login.getUserID());
                } else {
                    ackSender.sendAckData(false);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                ackSender.sendAckData(false);
            }
        });

        // LIST_USER
        server.addEventListener("list_user", Integer.class, (client, userID, ackSender) -> {
            try {
                List<ModelUserAccount> list = serviceUser.getUser(userID);
                client.sendEvent("list_user", list.toArray());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        // SEND TO USER
        server.addEventListener("send_to_user", ModelSendMessage.class, (client, data, ackSender) -> {
            sendToClient(data, ackSender);
        });

        // REQUEST HISTORY
        server.addEventListener("requestHistory", Integer.class, (client, toUserID, ackSender) -> {
            try {
                int fromUserID = getUserIDByClient(client);
                JSONObject response = new JSONObject();
                if (fromUserID == 0) {
                    response.put("action", false);
                    response.put("message", "User not logged in");
                    response.put("messageList", new JSONArray());
                    ackSender.sendAckData(response);
                    return;
                }

                List<ModelReceiveMessage> history = serviceUser.getMessageHistory(fromUserID, toUserID);
                JSONArray arr = new JSONArray();
                for (ModelReceiveMessage msg : history) {
                    arr.put(msg.toJSONObject());
                }

                response.put("action", true);
                response.put("message", "History retrieved");
                response.put("messageList", arr);
                ackSender.sendAckData(response);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    JSONObject response = new JSONObject();
                    response.put("action", false);
                    response.put("message", "Error retrieving history");
                    response.put("messageList", new JSONArray());
                    ackSender.sendAckData(response);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        server.start();
        textArea.append("Server started on port " + PORT_NUMBER + "\n");
    }

    // ================= Helper methods =================

    private void sendToClient(ModelSendMessage data, AckRequest ackRequest) {
        Integer fileID = null;
        if (data.getMessageType() == MessageType.IMAGE.getValue() ||
            data.getMessageType() == MessageType.FILE.getValue()) {
            try {
                ModelFile file = serviceFile.addFileReceiver(data.getText());
                serviceFile.initFile(file, data);
                fileID = file.getFileID();
                ackRequest.sendAckData(file.getFileID());
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        } else {
            for (ModelClient client : listClient) {
                if (client.getUser().getUserID() == data.getToUserID()) {
                    client.getClient().sendEvent("receive_ms",
                            new ModelReceiveMessage(data.getMessageType(), data.getFromUserID(), data.getText(), null));
                    break;
                }
            }
        }
        saveMessageToDB(data, fileID);
    }

    private void saveMessageToDB(ModelSendMessage data, Integer fileID) {
        String sql = "INSERT INTO messages (MessageType, FromUserID, ToUserID, TextContent, FileID) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, data.getMessageType());
            ps.setInt(2, data.getFromUserID());
            ps.setInt(3, data.getToUserID());
            ps.setString(4, data.getText());
            if (fileID != null) ps.setInt(5, fileID);
            else ps.setNull(5, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getUserIDByClient(SocketIOClient client) {
        for (ModelClient c : listClient) {
            if (c.getClient() == client) return c.getUser().getUserID();
        }
        return 0;
    }

    private void addClient(SocketIOClient client, ModelUserAccount user) {
        for (ModelClient c : listClient) {
            if (c.getUser().getUserID() == user.getUserID()) return; // tránh trùng
        }
        listClient.add(new ModelClient(client, user));
    }

    private void userConnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, true);
    }

    private void userDisconnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, false);
    }

    public int removeClient(SocketIOClient client) {
        for (ModelClient c : listClient) {
            if (c.getClient() == client) {
                listClient.remove(c);
                return c.getUser().getUserID();
            }
        }
        return 0;
    }

    public List<ModelClient> getListClient() {
        return listClient;
    }
}

package com.example.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.example.app.MessageType;
import com.example.model.ChatMessage; 
import com.example.model.ModelClient;
import com.example.model.ModelFile; 
import com.example.model.ModelLogin;
import com.example.model.ModelMessage;
import com.example.model.ModelPackageSender; 
import com.example.model.ModelReceiveImage; 
import com.example.model.ModelReceiveMessage;
import com.example.model.ModelRegister;
import com.example.model.ModelRequestFile; 
import com.example.model.ModelSendMessage;
import com.example.model.ModelUserAccount;
import com.example.repository.MessageRepository;
import java.io.IOException; 
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Service {

    private MessageRepository messageRepository;
    private static Service instance;
    private SocketIOServer server;
    private ServiceUser serviceUser;
    private ServiceFile serviceFile;
    private List<ModelClient> listClient;
    private JTextArea textArea;
    private final int PORT_NUMBER = 9999;

    public static Service getInstance(JTextArea textArea) {
        if (instance == null) {
            instance = new Service(textArea);
        }
        return instance;
    }

    private Service(JTextArea textArea) {
        this.textArea = textArea;
        this.messageRepository = new MessageRepository(); 
        serviceUser = new ServiceUser();
        this.serviceFile = new ServiceFile();
        listClient = new ArrayList<>();
    }

    public void startServer() {
        Configuration config = new Configuration();
        config.setPort(PORT_NUMBER);
        server = new SocketIOServer(config);
        
        // Listener 1: Connect
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient sioc) {
                SwingUtilities.invokeLater(() -> textArea.append("One client connected\n"));
                System.out.println("[Service] onConnect: client connected: " + sioc.getSessionId());
            }
        });

        // Listener 2: Disconnect 
        server.addDisconnectListener((DisconnectListener) client -> {
            SwingUtilities.invokeLater(() -> textArea.append("One client disconnected\n"));
            System.out.println("[Service] onDisconnect: client disconnected: " + client.getSessionId());
            int userID = removeClient(client);
            if (userID != 0) {
                userDisconnect(userID);
            }
        });
        
        // Listener 3: "register"
        server.addEventListener("register", ModelRegister.class, new DataListener<ModelRegister>() {
            @Override
            public void onData(SocketIOClient sioc, ModelRegister t, AckRequest ar) throws Exception {
                ModelMessage message = serviceUser.register(t);
                ar.sendAckData(message.isAction(), message.getMessage(), message.getData());
                if (message.isAction()) {
                    textArea.append("User has register:" + t.getUserName() + " Pass:" + t.getPassword() + "\n");
                    server.getBroadcastOperations().sendEvent("list_user", (ModelUserAccount) message.getData());
                    addClient(sioc, (ModelUserAccount) message.getData());
                }
            }
        });
        
        // Listener 4: "login"
        server.addEventListener("login", ModelLogin.class, new DataListener<ModelLogin>() {
            @Override
            public void onData(SocketIOClient client, ModelLogin data, AckRequest ackSender) throws Exception {
                ModelUserAccount login = serviceUser.login(data);
                if (login != null) {
                    ackSender.sendAckData(true, login);
                    addClient(client, login);
                    userConnect(login.getUserID());
                    sendPendingMessages(login.getUserID()); // Giữ lại tính năng Pending Message
                } else {
                    ackSender.sendAckData(false);
                }
            }
        });
        
        // Listener 5: "list_user"
        server.addEventListener("list_user", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient client, Integer userID, AckRequest ackSender) throws Exception {
                try {
                    List<ModelUserAccount> list = serviceUser.getUser(userID);
                    client.sendEvent("list_user", list.toArray());
                } catch (SQLException e) {
                    System.err.println(e);
                }
            }
        });
        
        // Listener 6: "send_to_user" (Hợp nhất File/Image và Save Message)
        server.addEventListener("send_to_user", ModelSendMessage.class, new DataListener<ModelSendMessage>() {
            @Override
            public void onData(SocketIOClient client, ModelSendMessage data, AckRequest ackSender) throws Exception {
                sendToClient(data, ackSender);
            }
        });

        // Listener 7: "get_history" (Giữ lại tính năng Lịch sử)
        server.addEventListener("get_history", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient client, Integer toUserID, AckRequest ackSender) throws Exception {
                int fromUserID = getFromUserID(client); 
                
                if (fromUserID != 0) {
                    try {
                        List<ChatMessage> history = messageRepository.getHistory(fromUserID, toUserID);
                        ackSender.sendAckData(history); 
                    } catch (SQLException e) {
                        System.err.println("Error loading chat history: " + e.getMessage());
                        ackSender.sendAckData(new ArrayList<>()); 
                    }
                } else {
                    ackSender.sendAckData(new ArrayList<>()); 
                }
            }
        });
        
        // Listener 8: "send_file" (Giữ lại tính năng File/Image)
        server.addEventListener("send_file", ModelPackageSender.class, new DataListener<ModelPackageSender>() {
            @Override
            public void onData(SocketIOClient client, ModelPackageSender data, AckRequest ackSender) throws Exception {
                try {
                    serviceFile.receiveFile(data);
                    if (data.isFinish()) {
                        ackSender.sendAckData(true);
                        ModelReceiveImage dataImage = new ModelReceiveImage();
                        dataImage.setFileID(data.getFileID());
                        ModelSendMessage message = serviceFile.closeFile(dataImage);
                        sendTempFileToClient(message, dataImage);
                    } else {
                        ackSender.sendAckData(true);
                    }
                } catch (IOException | SQLException e) {
                    ackSender.sendAckData(false);
                    e.printStackTrace();
                }
            }
        });
        
        // Listener 9: "get_file" (Giữ lại tính năng File/Image)
        server.addEventListener("get_file", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient client, Integer data, AckRequest ackSender) throws Exception {
                try {
                    System.out.println("[Service] Client requesting file info for FileID: " + data);
                    ModelFile file = serviceFile.initFile(data);
                    long fileSize = serviceFile.getFileSize(data);
                    System.out.println("[Service] Sending file info - Extension: " + file.getFileExtension() + ", Size: " + fileSize);
                    ackSender.sendAckData(file.getFileExtension(), fileSize);
                } catch (Exception e) {
                    System.err.println("[Service] ERROR in get_file for FileID " + data + ": " + e.getMessage());
                    e.printStackTrace();
                    ackSender.sendAckData();
                }
            }
        });
        
        // Listener 10: "request_file" (Giữ lại tính năng File/Image)
        server.addEventListener("request_file", ModelRequestFile.class, new DataListener<ModelRequestFile>() {
            @Override
            public void onData(SocketIOClient client, ModelRequestFile data, AckRequest ackSender) throws Exception {
                try {
                    System.out.println("[Service] Client requesting file chunk - FileID: " + data.getFileID() + ", CurrentLength: " + data.getCurrentLength());
                    byte[] d = serviceFile.getFileData(data.getCurrentLength(), data.getFileID());
                    if (d != null) {
                        System.out.println("[Service] Sending file chunk - FileID: " + data.getFileID() + ", From: " + data.getCurrentLength() + ", Size: " + d.length + " bytes");
                        
                        if (d.length > 0) {
                            StringBuilder sb = new StringBuilder("[Service] First bytes being sent: ");
                            for (int i = 0; i < Math.min(10, d.length); i++) {
                                sb.append(String.format("%02X ", d[i] & 0xFF));
                            }
                            System.out.println(sb.toString());
                        }
                        
                        ackSender.sendAckData(d);
                    } else {
                        System.out.println("[Service] No more data for FileID: " + data.getFileID());
                        ackSender.sendAckData();
                    }
                } catch (Exception e) {
                    System.err.println("[Service] ERROR sending file chunk for FileID " + data.getFileID() + ": " + e.getMessage());
                    e.printStackTrace();
                    ackSender.sendAckData();
                }
            }
        });
        
        server.start();
        textArea.append("Server has started on port: " + PORT_NUMBER + "\n");
    }
    
    private void userConnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, true);
    }
    
    private void userDisconnect(int userID) {
        server.getBroadcastOperations().sendEvent("user_status", userID, false);
    }
    
    private void addClient(SocketIOClient client, ModelUserAccount user) {
        listClient.add(new ModelClient(client, user));
    }

    // Logic xử lý tin nhắn được hợp nhất để giữ lại cả hai tính năng
    private void sendToClient(ModelSendMessage data, AckRequest ackRequest) {
        // Xử lý File/Image nếu messageType là File hoặc Image
        if (data.getMessageType() == MessageType.IMAGE.getValue() || data.getMessageType() == MessageType.FILE.getValue()) {
            try {
                ModelFile file = serviceFile.addFileReceiver(data.getText());
                serviceFile.initFile(file, data);
                ackRequest.sendAckData(file.getFileID());
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Xử lý tin nhắn TEXT/thông thường: Lưu DB và Gửi qua Socket
            
            // 1. Lưu tin nhắn vào DB (Từ c8d935e...)
            ChatMessage chatMessage = new ChatMessage(data.getFromUserID(), data.getToUserID(), data.getText());
            try {
                messageRepository.saveMessage(chatMessage);
            } catch (SQLException e) {
                System.err.println("Error saving message: " + e.getMessage());
            }
            
            // 2. Gửi tin nhắn qua Socket (Từ c8d935e...)
            ModelReceiveMessage receiveData = new ModelReceiveMessage(data.getFromUserID(), data.getToUserID(), data.getText()); 

            for (ModelClient client : listClient) {
                if (client.getUser().getUserID() == data.getToUserID()) {
                    client.getClient().sendEvent("receive_ms", receiveData); 
                    break;
                }
            }
        }
    }
    
    // Hàm hỗ trợ cho tính năng File/Image
    private void sendTempFileToClient(ModelSendMessage data, ModelReceiveImage dataImage) {
        System.out.println("[Service] Sending file to client - ToUser: " + data.getToUserID() + ", FileID: " + dataImage.getFileID() + ", Width: " + dataImage.getWidth() + ", Height: " + dataImage.getHeight());
        for (ModelClient client : listClient) {
            if (client.getUser().getUserID() == data.getToUserID()) {
                client.getClient().sendEvent("receive_ms", new ModelReceiveMessage(data.getMessageType(), data.getFromUserID(), data.getToUserID(), data.getText(), dataImage));
                System.out.println("[Service] File message sent successfully to User " + data.getToUserID());
                break;
            }
        }
    }

    // Hàm hỗ trợ cho tính năng Pending Messages
    private void sendPendingMessages(int userID) {
        SocketIOClient clientSocket = findClientSocket(userID);
        if (clientSocket != null) {
            try {
                List<ChatMessage> pendingMessages = messageRepository.getPendingMessages(userID);
                
                for (ChatMessage msg : pendingMessages) {
                    ModelReceiveMessage receiveData = new ModelReceiveMessage(msg.getSenderId(), msg.getReceiverId(), msg.getContent());
                    
                    clientSocket.sendEvent("receive_ms", receiveData);
                    
                    messageRepository.markAsSent(msg.getId());
                }
            } catch (SQLException e) {
                System.err.println("Error sending pending messages: " + e.getMessage());
            }
        }
    }
    
    private int getFromUserID(SocketIOClient client) {
        for (ModelClient c : listClient) {
            if (c.getClient() == client) {
                return c.getUser().getUserID();
            }
        }
        return 0;
    }

    private SocketIOClient findClientSocket(int userID) {
        for (ModelClient client : listClient) {
            if (client.getUser().getUserID() == userID) {
                return client.getClient();
            }
        }
        return null;
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
package com.example.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.example.model.ChatMessage; 
import com.example.model.ModelClient;
import com.example.model.ModelLogin;
import com.example.model.ModelMessage;
import com.example.model.ModelReceiveMessage;
import com.example.model.ModelRegister;
import com.example.model.ModelSendMessage;
import com.example.model.ModelUserAccount;
import com.example.repository.MessageRepository;
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
            // Xử lý mất kết nối
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
                    sendPendingMessages(login.getUserID());
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
        
        // Listener 6: "send_to_user" 
        server.addEventListener("send_to_user", ModelSendMessage.class, new DataListener<ModelSendMessage>() {
            @Override
            public void onData(SocketIOClient client, ModelSendMessage data, AckRequest ackSender) throws Exception {
                sendToClient(data);
            }
        });
        
        // Listener 7: MỚI - "get_history" 
        server.addEventListener("get_history", Integer.class, new DataListener<Integer>() {
            @Override
            public void onData(SocketIOClient client, Integer toUserID, AckRequest ackSender) throws Exception {
                int fromUserID = getFromUserID(client); 
                
                if (fromUserID != 0) {
                    try {
                        List<ChatMessage> history = messageRepository.getHistory(fromUserID, toUserID);
                        // Gửi List<ChatMessage> (sẽ được chuyển thành JSONArray) về Client
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

    // Thêm logic lưu tin nhắn vào DB TRƯỚC khi gửi
    private void sendToClient(ModelSendMessage data) {
        // 1. Lưu tin nhắn vào DB
        ChatMessage chatMessage = new ChatMessage(data.getFromUserID(), data.getToUserID(), data.getText());
        try {
            messageRepository.saveMessage(chatMessage);
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
        
        // 2. Gửi tin nhắn qua Socket như cũ
        ModelReceiveMessage receiveData = new ModelReceiveMessage(data.getFromUserID(), data.getToUserID(), data.getText());

        for (ModelClient client : listClient) {
            if (client.getUser().getUserID() == data.getToUserID()) {
                // Gửi đối tượng hoàn chỉnh
                client.getClient().sendEvent("receive_ms", receiveData); 
                break;
            }
        }
    }

    private void sendPendingMessages(int userID) {
    SocketIOClient clientSocket = findClientSocket(userID);
    if (clientSocket != null) {
        try {
            // Lấy tin nhắn chờ từ DB
            List<ChatMessage> pendingMessages = messageRepository.getPendingMessages(userID);
            
            for (ChatMessage msg : pendingMessages) {
                // Chuyển ChatMessage sang ModelReceiveMessage để gửi qua Socket
                ModelReceiveMessage receiveData = new ModelReceiveMessage(msg.getSenderId(), msg.getReceiverId(), msg.getContent());
                
                // Gửi tin nhắn
                clientSocket.sendEvent("receive_ms", receiveData);
                
                // Đánh dấu tin nhắn là đã gửi thành công
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
    
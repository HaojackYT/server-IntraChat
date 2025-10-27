package com.example.service;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.example.model.ModelMessage;
import com.example.model.ModelRegister;
import com.example.model.ModelUserAccount;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Service {

    private static Service instance;
    private SocketIOServer server;
    private ServiceUser serviceUser;
    private JTextArea textArea;
    private final int PORT_NUMBER = 9999;

    public static Service getInstance(JTextArea textArea) {
        if (instance == null) {
            instance = new Service(textArea);
        }
        return instance;
    }

    private Service(JTextArea textArea) {
        serviceUser = new ServiceUser();
        this.textArea = textArea;
    }

    public void startServer() {
        Configuration config = new Configuration();
        config.setPort(PORT_NUMBER);
        server = new SocketIOServer(config);
        // Register connect listener correctly so onConnect is invoked
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient sioc) {
                // Always update Swing components on the EDT
                SwingUtilities.invokeLater(() -> textArea.append("One client connected\n"));
                // Also print to console for easier debugging when GUI isn't visible
                System.out.println("[Service] onConnect: client connected: " + sioc.getSessionId());
            }
        });

        // Add a disconnect listener as well to help debugging disconnects
        server.addDisconnectListener((DisconnectListener) client -> {
            SwingUtilities.invokeLater(() -> textArea.append("One client disconnected\n"));
            System.out.println("[Service] onDisconnect: client disconnected: " + client.getSessionId());
        });
        server.addEventListener("register", ModelRegister.class, new DataListener<ModelRegister>() {
            @Override
            public void onData(SocketIOClient sioc, ModelRegister t, AckRequest ar) throws Exception {
                ModelMessage message = serviceUser.register(t);
                ar.sendAckData(message.isAction(), message.getMessage(), message.getData());
                if (message.isAction()) {
                    textArea.append("User has register:" + t.getUserName() + " Pass:" + t.getPassword() + "\n");
                    server.getBroadcastOperations().sendEvent("list_user", (ModelUserAccount) message.getData());
                }
            }
        });
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
        server.start();
        textArea.append("Server has started on port: " + PORT_NUMBER + "\n");
    }
}

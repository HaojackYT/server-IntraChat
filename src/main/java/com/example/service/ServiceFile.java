package com.example.service;

import com.example.app.MessageType;
import com.example.connection.DatabaseConnection;
import com.example.model.ModelFile;
import com.example.model.ModelFileReceiver;
import com.example.model.ModelFileSender;
import com.example.model.ModelPackageSender;
import com.example.model.ModelReceiveImage;
import com.example.model.ModelSendMessage;
import com.example.swing.blurHash.BlurHash;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
// using binary transfer (byte[]) instead of Base64 strings
import javax.imageio.ImageIO;

public class ServiceFile {

    public ServiceFile() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.fileReceivers = new HashMap<>();
        this.fileSenders = new HashMap<>();
    }
    
    public ModelFile addFileReceiver(String fileExtension) throws SQLException {
        ModelFile data;
        PreparedStatement preparedStatement = connection.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, fileExtension);
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getGeneratedKeys();
        resultSet.first();
        int fileID = resultSet.getInt(1);
        data = new ModelFile(fileID, fileExtension);
        resultSet.close();
        preparedStatement.close();
        return data;
    }
    
    public void updateBlurHashDone(int fileID, String blurHash) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_BLUR_HASH_DONE);
        preparedStatement.setString(1, blurHash);
        preparedStatement.setInt(2, fileID);
        preparedStatement.execute();
        preparedStatement.close();
    }
    
    public void updateDone(int fileID) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_DONE);
        preparedStatement.setInt(1, fileID);
        preparedStatement.execute();
        preparedStatement.close();
    }
    
    public void initFile(ModelFile file, ModelSendMessage message) throws IOException {
        fileReceivers.put(file.getFileID(), new ModelFileReceiver(message, toFileObject(file)));
    }
    
    public ModelFile getFile(int fileID) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(GET_FILE_EXTENSION);
        preparedStatement.setInt(1, fileID);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            String fileExtension = resultSet.getString(1);
            ModelFile data = new ModelFile(fileID, fileExtension);
            resultSet.close();
            preparedStatement.close();
            return data;
        } else {
            resultSet.close();
            preparedStatement.close();
            throw new SQLException("File not found with ID: " + fileID);
        }
    }
    
    public synchronized ModelFile initFile(int fileID) throws IOException, SQLException {
        ModelFile file;
        if (!fileSenders.containsKey(fileID)) {
            file = getFile(fileID);
            File physicalFile = new File(PATH_FILE + fileID + file.getFileExtension());
            System.out.println("[ServiceFile] Initializing file sender for FileID: " + fileID + ", Path: " + physicalFile.getAbsolutePath() + ", Exists: " + physicalFile.exists() + ", Size: " + physicalFile.length());
            fileSenders.put(fileID, new ModelFileSender(file, physicalFile));
        } else {
            file = fileSenders.get(fileID).getData();
            System.out.println("[ServiceFile] File sender already initialized for FileID: " + fileID);
        }
        return file;
    }
    
    public byte[] getFileData(long currentLength, int fileID) throws IOException, SQLException {
        initFile(fileID);
        return fileSenders.get(fileID).read(currentLength);
    }
    
    public long getFileSize(int fileID) {
        return fileSenders.get(fileID).getFileSize();
    }
    
    public void receiveFile(ModelPackageSender dataPackage) throws IOException {
        if (!dataPackage.isFinish()) {
            // data is received as binary chunk (byte[])
            byte[] bytes = dataPackage.getData();
            if (bytes != null) {
                fileReceivers.get(dataPackage.getFileID()).writeFile(bytes);
            }
        } else {
            fileReceivers.get(dataPackage.getFileID()).close();
        }
    }
    
    public ModelSendMessage closeFile(ModelReceiveImage dataImage) throws IOException, SQLException {
        ModelFileReceiver file = fileReceivers.get(dataImage.getFileID());
        System.out.println("[ServiceFile] Closing file for FileID: " + dataImage.getFileID() + ", MessageType: " + file.getMessage().getMessageType());
        if (file.getMessage().getMessageType() == MessageType.IMAGE.getValue()) {
            // Create BlurHash image
            file.getMessage().setText("");
            String blurHash = convertFileToBlurHash(file.getFile(), dataImage);
            System.out.println("[ServiceFile] Created BlurHash for image: " + blurHash + ", Width: " + dataImage.getWidth() + ", Height: " + dataImage.getHeight());
            updateBlurHashDone(dataImage.getFileID(), blurHash);
        } else {
            updateDone(dataImage.getFileID());
        }
        fileReceivers.remove(dataImage.getFileID());
        // Get message to send to target client when finish receiving
        return file.getMessage();
    }
    
    private String convertFileToBlurHash(File file, ModelReceiveImage dataImage) throws IOException {
        BufferedImage image = ImageIO.read(file);
        Dimension size = getAutoSize(new Dimension(image.getWidth(), image.getHeight()), new Dimension(200, 200));
        // Convert image size smaller
        BufferedImage newImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, size.width, size.height, null);
        String blurHash = BlurHash.encode(newImage);
        dataImage.setWidth(size.width);
        dataImage.setHeight(size.height);
        dataImage.setImage(blurHash);
        return blurHash;
    }
    
    private Dimension getAutoSize(Dimension fromSize, Dimension toSize) {
        int w = toSize.width;
        int h = toSize.height;
        int iw = fromSize.width;
        int ih = fromSize.height;
        double xScale = (double) w / iw;
        double yScale = (double) h / ih;
        double scale = Math.min(xScale, yScale);
        int width = (int) (scale * iw);
        int height = (int) (scale * ih);
        return new Dimension(width, height);
    }
    
    private File toFileObject(ModelFile file) {
        return new File(PATH_FILE + file.getFileID() + file.getFileExtension());
    }
    
    // SQL
    private final String PATH_FILE = "db/data/";
    private final String INSERT = "INSERT INTO `files` (fileExtension) VALUES (?)";
    private final String UPDATE_BLUR_HASH_DONE = "UPDATE `files` SET BlurHash = ?, `Status` = '1' WHERE `FileID`=? LIMIT 1";
    private final String UPDATE_DONE = "UPDATE `files` SET `Status` = '1' WHERE `FileID` = ? LIMIT 1";
    private final String GET_FILE_EXTENSION = "SELECT `FileExtension` FROM `files` WHERE `FileID` = ? LIMIT 1";
    
    // Instance
    private final Connection connection;
    private final Map<Integer, ModelFileReceiver> fileReceivers;
    private final Map<Integer, ModelFileSender> fileSenders;
}

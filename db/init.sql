CREATE DATABASE chat_application_intrachat;
USE chat_application_intrachat;

CREATE TABLE `user` (
    UserID INT NOT NULL AUTO_INCREMENT,
    UserName VARCHAR(255) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    PRIMARY KEY (UserID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_account (
    UserID INT NOT NULL, 
    UserName VARCHAR(255) NOT NULL,
    Gender CHAR(1) NOT NULL DEFAULT '',
    Image LONGBLOB,
    ImageString VARCHAR(255) NOT NULL DEFAULT '',
    Status CHAR(1) NOT NULL DEFAULT '1',
    
    PRIMARY KEY (UserID), 
    
    CONSTRAINT fk_user_account_user 
        FOREIGN KEY (UserID) 
        REFERENCES `user`(UserID)
        ON DELETE CASCADE 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    content TEXT NOT NULL,
    sent_time DATETIME NOT NULL,
    
    CONSTRAINT fk_message_sender 
        FOREIGN KEY (sender_id) 
        REFERENCES user_account(UserID)
        ON DELETE CASCADE, 
    
    CONSTRAINT fk_message_receiver 
        FOREIGN KEY (receiver_id) 
        REFERENCES user_account(UserID)
        ON DELETE CASCADE,
        
    INDEX idx_chat_pair (sender_id, receiver_id, sent_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
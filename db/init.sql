CREATE DATABASE chat_application_intrachat;

CREATE TABLE `user` (
    UserID INT NOT NULL AUTO_INCREMENT,
    UserName VARCHAR(255),
    Password VARCHAR(255),
    PRIMARY KEY (UserID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_account (
    UserID INT(10) NOT NULL AUTO_INCREMENT,
    UserName VARCHAR(255) NOT NULL,
    Gender CHAR(1) NOT NULL DEFAULT '',
    Image LONGBLOB,
    ImageString VARCHAR(255) NOT NULL DEFAULT '',
    Status CHAR(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (UserID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
package com.is.auth.service;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class SftpService {

    @Value("${sftp.server}")
    private String server;

    @Value("${sftp.port}")
    private int port;

    @Value("${sftp.username}")
    private String username;

    @Value("${sftp.password}")
    private String password;

    public boolean uploadFile(String remoteFilePath, InputStream inputStream) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        
        try {
            log.info("Connecting to SFTP server: {}:{}", server, port);
            
            // Создаем сессию
            session = jsch.getSession(username, server, port);
            session.setPassword(password);
            
            // Настройки для избежания проблем с known_hosts
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            log.info("Logging in to SFTP server with username: {}", username);
            session.connect();
            
            // Открываем SFTP канал
            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            
            log.info("Creating directories for path: {}", remoteFilePath);
            // Создаем директории, если они не существуют
            createDirectories(channelSftp, remoteFilePath);
            
            log.info("Uploading file to: {}", remoteFilePath);
            // Загружаем файл
            channelSftp.put(inputStream, remoteFilePath);
            
            log.info("File uploaded successfully to SFTP: {}", remoteFilePath);
            return true;
            
        } catch (JSchException | SftpException e) {
            log.error("Error uploading file to SFTP server: {}", e.getMessage(), e);
            return false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (channelSftp != null && channelSftp.isConnected()) {
                    channelSftp.disconnect();
                }
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
                log.info("SFTP connection closed");
            } catch (IOException e) {
                log.error("Error closing SFTP connection: {}", e.getMessage());
            }
        }
    }

    private void createDirectories(ChannelSftp channelSftp, String remoteFilePath) throws SftpException {
        String[] directories = remoteFilePath.split("/");
        String currentPath = "";
        
        for (int i = 0; i < directories.length - 1; i++) {
            if (!directories[i].isEmpty()) {
                currentPath += "/" + directories[i];
                try {
                    channelSftp.cd(currentPath);
                    log.debug("Directory exists: {}", currentPath);
                } catch (SftpException e) {
                    // Директория не существует, создаем её
                    try {
                        channelSftp.mkdir(currentPath);
                        log.info("Created directory: {}", currentPath);
                    } catch (SftpException mkdirException) {
                        log.debug("Directory creation failed (might already exist): {}", currentPath);
                    }
                }
            }
        }
    }

    public boolean testConnection() {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            log.info("Testing SFTP connection to {}:{}", server, port);
            
            session = jsch.getSession(username, server, port);
            session.setPassword(password);
            
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            session.connect();
            
            log.info("SFTP connection test successful");
            return true;
            
        } catch (JSchException e) {
            log.error("SFTP connection test failed: {}", e.getMessage(), e);
            return false;
        } finally {
            try {
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            } catch (Exception e) {
                log.error("Error closing SFTP test connection: {}", e.getMessage());
            }
        }
    }

    public InputStream downloadFile(String remoteFilePath) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        
        try {
            log.info("Connecting to SFTP server for download: {}:{}", server, port);
            
            // Создаем сессию
            session = jsch.getSession(username, server, port);
            session.setPassword(password);
            
            // Настройки для избежания проблем с known_hosts
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            log.info("Logging in to SFTP server with username: {}", username);
            session.connect();
            
            // Открываем SFTP канал
            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            
            log.info("Downloading file from: {}", remoteFilePath);
            // Скачиваем файл
            InputStream inputStream = channelSftp.get(remoteFilePath);
            
            log.info("File downloaded successfully from SFTP: {}", remoteFilePath);
            return inputStream;
            
        } catch (JSchException | SftpException e) {
            log.error("Error downloading file from SFTP server: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during file download: {}", e.getMessage(), e);
            return null;
        }
        // Не закрываем соединение здесь, так как нужно вернуть InputStream
        // Соединение будет закрыто после использования InputStream
    }
}

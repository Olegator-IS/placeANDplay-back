package com.is.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class FtpService {

    @Value("${ftp.server:web2.webspace.uz}")
    private String server;

    @Value("${ftp.port:21}")
    private int port;

    @Value("${ftp.username:placeand}")
    private String username;

    @Value("${ftp.password:bwq6sjVvQwsYhhrqLs}")
    private String password;

    public boolean uploadFile(String remoteFilePath, InputStream inputStream) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Создаем директории, если они не существуют
            String[] directories = remoteFilePath.split("/");
            String currentPath = "";
            for (int i = 0; i < directories.length - 1; i++) {
                if (!directories[i].isEmpty()) {
                    currentPath += "/" + directories[i];
                    ftpClient.makeDirectory(currentPath);
                }
            }

            // Загружаем файл
            boolean success = ftpClient.storeFile(remoteFilePath, inputStream);
            log.info("File upload " + (success ? "successful" : "failed") + ": " + remoteFilePath);
            return success;
        } catch (IOException e) {
            log.error("Error uploading file to FTP server: {}", e.getMessage());
            return false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                ftpClient.disconnect();
            } catch (IOException e) {
                log.error("Error closing FTP connection: {}", e.getMessage());
            }
        }
    }
} 
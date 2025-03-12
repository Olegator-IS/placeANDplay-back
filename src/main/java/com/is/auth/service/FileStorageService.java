package com.is.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:/domains/placeandplay.uz/public_html/uploads}")
    private String uploadDir;

    @Value("${app.domain:https://placeandplay.uz}")
    private String domain;

    private final FtpService ftpService;

    public FileStorageService(FtpService ftpService) {
        this.ftpService = ftpService;
    }

    public String uploadFile(MultipartFile file, String directory) {
        try {
            // Генерируем уникальное имя файла
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            // Формируем путь для сохранения на FTP
            String remotePath = uploadDir + "/" + directory + "/" + fileName;
            
            log.info("Trying to upload file to FTP: {}", remotePath);
            
            // Загружаем файл на FTP сервер
            boolean success = ftpService.uploadFile(remotePath, file.getInputStream());
            
            if (!success) {
                throw new IOException("Failed to upload file to FTP server");
            }
            
            log.info("File uploaded successfully to FTP: {}", remotePath);

            // Возвращаем полный URL для доступа к файлу через веб
            return domain + "/uploads/" + directory + "/" + fileName;
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
} 
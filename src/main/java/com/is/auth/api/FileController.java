package com.is.auth.api;

import com.is.auth.service.SftpService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@Api(tags = "File Download APIs", description = "APIs for downloading files from SFTP server")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final SftpService sftpService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping("/download/{directory}/{filename}")
    @ApiOperation(value = "Download file", notes = "Download file from SFTP server")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable String directory,
            @PathVariable String filename) {
        
        try {
            String filePath = uploadDir + "/" + directory + "/" + filename;
            log.info("Attempting to download file: {}", filePath);
            
            InputStream inputStream = sftpService.downloadFile(filePath);
            if (inputStream == null) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Определяем MIME тип файла
            String contentType = determineContentType(filename);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            
            log.info("File downloaded successfully: {}", filePath);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
                    
        } catch (Exception e) {
            log.error("Error downloading file {}/{}: {}", directory, filename, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/view/{directory}/{filename}")
    @ApiOperation(value = "View file", notes = "View file in browser (for images, PDFs, etc.)")
    public ResponseEntity<InputStreamResource> viewFile(
            @PathVariable String directory,
            @PathVariable String filename) {
        
        try {
            String filePath = uploadDir + "/" + directory + "/" + filename;
            log.info("Attempting to view file: {}", filePath);
            
            InputStream inputStream = sftpService.downloadFile(filePath);
            if (inputStream == null) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Определяем MIME тип файла
            String contentType = determineContentType(filename);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            log.info("File viewed successfully: {}", filePath);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
                    
        } catch (Exception e) {
            log.error("Error viewing file {}/{}: {}", directory, filename, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }
}

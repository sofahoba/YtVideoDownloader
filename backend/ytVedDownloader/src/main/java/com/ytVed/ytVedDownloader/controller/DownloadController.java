package com.ytVed.ytVedDownloader.controller;

import com.ytVed.ytVedDownloader.model.ApiResponse;
import com.ytVed.ytVedDownloader.model.DownloadRequest;
import com.ytVed.ytVedDownloader.service.DownloadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class DownloadController {

    @Autowired
    private DownloadService downloadService;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getVideoInfo(
            @RequestParam String url) {
        try {
            Map<String, String> info = downloadService.getVideoInfo(url);
            return ResponseEntity.ok(ApiResponse.success(info));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/download")
    public ResponseEntity<?> downloadVideo(
            @Valid @RequestBody DownloadRequest request) {
        try {
            File file = downloadService.downloadVideo(
                    request.getUrl(),
                    request.getFormat(),
                    request.getQuality()
            );

            FileSystemResource resource = new FileSystemResource(file);

            String actualFileName = file.getName();
            String actualExtension = getFileExtension(actualFileName);

            String contentType = getContentType(actualExtension);

            System.out.println("Sending file    : " + actualFileName);
            System.out.println("Actual extension: " + actualExtension);
            System.out.println("Content type    : " + contentType);

            String safeFileName = sanitizeFileName(actualFileName);

            String encodedFileName = URLEncoder.encode(actualFileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; " +
                                    "filename=\"" + safeFileName + "\"; " +
                                    "filename*=UTF-8''" + encodedFileName)
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(
                ApiResponse.success(Map.of(
                        "status", "UP",
                        "service", "YT Downloader API"
                ))
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + errors));
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "mp4";
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    private String getContentType(String extension) {
        return switch (extension) {
            case "mp4"  -> "video/mp4";
            case "mp3"  -> "audio/mpeg";
            case "webm" -> "video/webm";
            case "m4a"  -> "audio/mp4";
            case "ogg"  -> "audio/ogg";
            default     -> "application/octet-stream";
        };
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^\\x20-\\x7E]", "_")
                // Remove emojis and other special chars
                .replaceAll("[^a-zA-Z0-9._\\-() ]", "_")
                // Collapse multiple underscores
                .replaceAll("_+", "_");
    }
}
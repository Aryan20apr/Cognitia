package com.intellidesk.cognitia.ingestion.service.uploadStrategy;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import java.util.List;



@Service
@Slf4j
public class FileUploadStrategyFactory {

    private final List<FileUploadStrategy> strategies;

    public FileUploadStrategyFactory(List<FileUploadStrategy> strategies) {
        this.strategies = strategies;
    }

    public FileUploadStrategy getStrategy(MultipartFile file) {
        String contentType = file.getContentType();
        String extension = extractExtension(file.getOriginalFilename());
        for (FileUploadStrategy strat : strategies) {
            if (strat.supports(contentType, extension)) {
                return strat;
            }
        }
        throw new IllegalArgumentException("No upload strategy found for file type: " + contentType + ", extension: " + extension);
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx >= 0 && idx < filename.length() - 1) ? filename.substring(idx + 1) : "";
    }
}

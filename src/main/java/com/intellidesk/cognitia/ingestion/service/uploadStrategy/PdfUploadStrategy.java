package com.intellidesk.cognitia.ingestion.service.uploadStrategy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfUploadStrategy implements FileUploadStrategy {

    private final Cloudinary cloudinary;

    @Override
    public CloudinaryUploadResult upload(MultipartFile file) throws IOException {
        Map<String, Object> uploadParams = new HashMap<>();
        uploadParams.put("resource_type", "raw");
        if (file.getOriginalFilename() != null) {
            uploadParams.put("public_id", file.getOriginalFilename().replaceAll("\\..+$", ""));
        }
        var result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
        log.info("[PdfUploadStrategy] [upload] Cloudinary upload result: {}", result);
        return CloudinaryMapper.fromMap(result);
    }

    @Override
    public boolean supports(String contentType, String extension) {
          return extension.equalsIgnoreCase("pdf") 
            || "application/pdf".equalsIgnoreCase(contentType);
    }
    
}

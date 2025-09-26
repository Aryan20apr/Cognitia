package com.intellidesk.cognitia.ingestion.storage.service.uploadStrategy;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.intellidesk.cognitia.ingestion.storage.models.dtos.CloudinaryUploadResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfUploadStrategy implements FileUploadStrategy {

    private final Cloudinary cloudinary;

    @Override
    public CloudinaryUploadResult upload(MultipartFile file) throws IOException {
        var result = cloudinary.uploader().upload(
            file.getBytes(),
            Map.of(
                "resource_type", "raw",
                "public_id", /* perhaps derive or pass in */
                    file.getOriginalFilename() != null
                        ? file.getOriginalFilename().replaceAll("\\..+$","") 
                        : null
            )
        );
        log.info("[PdfUploadStrategy] [upload] Cloudinary upload result: {}", result);
        return CloudinaryMapper.fromMap(result);
    }

    @Override
    public boolean supports(String contentType, String extension) {
          return extension.equalsIgnoreCase("pdf") 
            || "application/pdf".equalsIgnoreCase(contentType);
    }
    
}

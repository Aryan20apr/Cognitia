package com.intellidesk.cognitia.ingestion.service.uploadStrategy;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ImageUploadStrategy implements FileUploadStrategy {

    private final Cloudinary cloudinary;
    private final Set<String> supportedExtensions;

    public ImageUploadStrategy(
            Cloudinary cloudinary,
            @Value("${ingestion.supported-extensions.image:}") String imageExts) {

        this.cloudinary = cloudinary;
        this.supportedExtensions = Stream.of(imageExts.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public CloudinaryUploadResult upload(MultipartFile file) throws IOException {
        var result = cloudinary.uploader().upload(
                file.getBytes(),
                Map.of(
                        "resource_type", "image",
                        "public_id", file.getOriginalFilename() != null
                                ? file.getOriginalFilename().replaceAll("\\..+$", "")
                                : null
                )
        );
        log.info("[ImageUploadStrategy] [upload] Cloudinary upload result: {}", result);
        return CloudinaryMapper.fromMap(result);
    }

    @Override
    public boolean supports(String contentType, String extension) {
        if (extension == null) return false;
        return supportedExtensions.contains(extension.toLowerCase());
    }
}

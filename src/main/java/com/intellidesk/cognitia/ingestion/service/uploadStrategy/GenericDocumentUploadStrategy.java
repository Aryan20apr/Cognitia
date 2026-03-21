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
public class GenericDocumentUploadStrategy implements FileUploadStrategy {

    private final Cloudinary cloudinary;
    private final Set<String> supportedExtensions;

    public GenericDocumentUploadStrategy(
            Cloudinary cloudinary,
            @Value("${ingestion.supported-extensions.document:}") String documentExts,
            @Value("${ingestion.supported-extensions.tabular:}") String tabularExts) {

        this.cloudinary = cloudinary;
        this.supportedExtensions = Stream.of(documentExts, tabularExts)
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> Stream.of(s.split(",")))
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
                        "resource_type", "raw",
                        "public_id", file.getOriginalFilename() != null
                                ? file.getOriginalFilename().replaceAll("\\..+$", "")
                                : null
                )
        );
        log.info("[GenericDocumentUploadStrategy] [upload] Cloudinary upload result: {}", result);
        return CloudinaryMapper.fromMap(result);
    }

    @Override
    public boolean supports(String contentType, String extension) {
        if (extension == null) return false;
        return supportedExtensions.contains(extension.toLowerCase());
    }
}

package com.intellidesk.cognitia.ingestion.storage.service.uploadStrategy;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.storage.models.dtos.CloudinaryUploadResult;

public interface FileUploadStrategy {

    /**
     * Uploads the provided multipart file using this strategy.
     *
     * Implementations should validate the input and persist the file according to
     * the
     * strategy’s rules (e.g., target storage, path, naming).
     *
     * @param file the multipart file to upload; must not be empty
     * @return an UploadResponseDTO describing the outcome and any generated
     *         identifiers
     * @throws IOException 
     */
    public CloudinaryUploadResult upload(MultipartFile file) throws IOException;

    /**
     * Determines whether this strategy supports a file described by the given
     * content type and filename extension.
     *
     * Matching should be case-insensitive and may rely on either argument.
     *
     * @param contentType the MIME type from the request or file metadata, may be
     *                    null
     * @param extension   the file extension without a dot, may be null
     * @return true if the file is supported by this strategy; false otherwise
     */
    boolean supports(String contentType, String extension);

}
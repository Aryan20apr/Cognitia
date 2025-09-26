package com.intellidesk.cognitia.ingestion.storage.service;

import java.io.File;

import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.storage.models.dtos.CloudinaryUploadResult;

public interface StorageService {
    
    public CloudinaryUploadResult uploadRawResource(MultipartFile file);
}

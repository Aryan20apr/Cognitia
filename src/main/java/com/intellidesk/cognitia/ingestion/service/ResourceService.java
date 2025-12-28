package com.intellidesk.cognitia.ingestion.service;

import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceMetadata;

public interface ResourceService {
    
    public CloudinaryUploadResult uploadRawResource(MultipartFile file, ResourceMetadata resourceMetadata);

    public List<ResourceDetailsDTO> getResourceUploadHistory();
}

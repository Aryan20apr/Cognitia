package com.intellidesk.cognitia.ingestion.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceDetails;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceMetadata;
import org.springframework.data.domain.Page;

public interface ResourceService {
    
    public CloudinaryUploadResult uploadRawResource(MultipartFile file, ResourceMetadata resourceMetadata);

    public Page<ResourceDetails> getResourceUploadHistory(int page, int size);
}

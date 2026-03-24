package com.intellidesk.cognitia.ingestion.service;

import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceDetails;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceMetadata;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceUpdateDTO;

import org.springframework.data.domain.Page;

public interface ResourceService {
    
    public CloudinaryUploadResult uploadRawResource(MultipartFile file, ResourceMetadata resourceMetadata);

    public Page<ResourceDetails> getResourceUploadHistory(int page, int size);

    public ResourceDetails updateResource(String assetId, ResourceUpdateDTO dto);

    public void deleteResource(String assetId);
}

package com.intellidesk.cognitia.ingestion.storage.service;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.storage.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.storage.models.dtos.ResourceMetadata;
import com.intellidesk.cognitia.ingestion.storage.models.entities.RawSouce;
import com.intellidesk.cognitia.ingestion.storage.models.enums.Status;
import com.intellidesk.cognitia.ingestion.storage.repository.ResourceRepository;
import com.intellidesk.cognitia.ingestion.storage.service.uploadStrategy.FileUploadStrategy;
import com.intellidesk.cognitia.ingestion.storage.service.uploadStrategy.FileUploadStrategyFactory;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ResourceUploadException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class StorageServiceImple implements StorageService {

    private final FileUploadStrategyFactory fileUploadStrategyFactory;
    private final ResourceRepository resourceRepository;

     @Override
    public CloudinaryUploadResult uploadRawResource(MultipartFile file, ResourceMetadata resourceMetadata) {
        

        FileUploadStrategy fileUploadStrategy = fileUploadStrategyFactory.getStrategy(file);
        try {
           CloudinaryUploadResult cloudinaryUploadResult = fileUploadStrategy.upload(file);
           
           RawSouce rawSouce = RawSouce.builder()
                .assetId(cloudinaryUploadResult.assetId())
                .url(cloudinaryUploadResult.url())
                .name(resourceMetadata.name())
                .description(resourceMetadata.description())
                .secureUrl(cloudinaryUploadResult.secureUrl())
                .signature(cloudinaryUploadResult.signature())
                .size((double)file.getSize())
                .format(file.getContentType())
                .status(Status.UPLOADED)
                .build();
           resourceRepository.save(rawSouce);
           return cloudinaryUploadResult;
        } catch (IOException e) {
            throw new ResourceUploadException(e.getMessage());
        }
    }
    
}

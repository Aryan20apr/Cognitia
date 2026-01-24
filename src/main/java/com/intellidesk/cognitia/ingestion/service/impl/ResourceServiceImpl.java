package com.intellidesk.cognitia.ingestion.service.impl;

import java.io.IOException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceDetails;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceMetadata;
import com.intellidesk.cognitia.ingestion.models.entities.IngestionJob;
import com.intellidesk.cognitia.ingestion.models.entities.Resource;
import com.intellidesk.cognitia.ingestion.models.enums.IngestionStatus;
import com.intellidesk.cognitia.ingestion.models.enums.Status;
import com.intellidesk.cognitia.ingestion.repository.IngestionJobRepository;
import com.intellidesk.cognitia.ingestion.repository.ResourceRepository;
import com.intellidesk.cognitia.ingestion.service.ResourceService;
import com.intellidesk.cognitia.ingestion.service.uploadStrategy.FileUploadStrategy;
import com.intellidesk.cognitia.ingestion.service.uploadStrategy.FileUploadStrategyFactory;
import com.intellidesk.cognitia.ingestion.utils.ResourceMapper;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.utils.exceptionHandling.QuotaExceededException;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ResourceUploadException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final FileUploadStrategyFactory fileUploadStrategyFactory;
    private final IngestionJobRepository resourceOutboxRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceMapper mapper;
    private final QuotaService quotaService;

    @Override
    @Transactional
    public CloudinaryUploadResult uploadRawResource(MultipartFile file, ResourceMetadata resourceMetadata) {
        
        // Check resource quota before uploading
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null && !quotaService.canUploadResource(tenantId)) {
            log.warn("Resource quota exceeded for tenant {}", tenantId);
            throw new QuotaExceededException("Resource upload quota exceeded. Please upgrade your plan or delete existing resources.");
        }

        FileUploadStrategy fileUploadStrategy = fileUploadStrategyFactory.getStrategy(file);
        try {
           CloudinaryUploadResult cloudinaryUploadResult = fileUploadStrategy.upload(file);
           
           Resource rawSouce = Resource.builder()
                .assetId(cloudinaryUploadResult.assetId())
                .url(cloudinaryUploadResult.url())
                .name(resourceMetadata.name())
                .description(resourceMetadata.description())
                .secureUrl(cloudinaryUploadResult.secureUrl())
                .signature(cloudinaryUploadResult.signature())
                .size((double)file.getSize())
                .format(getFileExtension(file))
                .status(Status.UPLOADED)
                .build();

            IngestionJob ingestionOutbox = IngestionJob.builder()
                .source(rawSouce)
                .status(IngestionStatus.PENDING_PROCESSING)
                .retries(0)
                .build();

           resourceOutboxRepository.save(ingestionOutbox);
           resourceRepository.save(rawSouce);
           
           // Increment resource count after successful save
           if (tenantId != null) {
               quotaService.incrementResourceCount(tenantId);
           }
           
           return cloudinaryUploadResult;
        } catch (IOException e) {
            throw new ResourceUploadException(e.getMessage());
        }
    }

    public String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return null; // or throw exception
        }

        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex == -1) {
            return ""; // no extension
        }
        return originalFilename.substring(dotIndex); // includes the dot (.pdf, .txt, etc.)
}

    @Override
    @Transactional
    public Page<ResourceDetails> getResourceUploadHistory(int page, int size) {
       
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt") // optional but recommended
    );

    Page<Resource> pageResult = resourceRepository.findAll(pageable);

    return pageResult.map(res -> mapper.toDto(res));
    }

    
}

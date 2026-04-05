package com.intellidesk.cognitia.ingestion.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceDetails;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceMetadata;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceUpdateDTO;
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
import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;
import com.intellidesk.cognitia.userandauth.models.entities.Department;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.repository.ClassificationLevelRepository;
import com.intellidesk.cognitia.userandauth.repository.DepartmentRepository;
import com.intellidesk.cognitia.utils.exceptionHandling.QuotaExceededException;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;
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
    private final Cloudinary cloudinary;
    private final VectorStore vectorStore;
    private final DepartmentRepository departmentRepository;
    private final ClassificationLevelRepository classificationLevelRepository;

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
           
           Department department = null;
           ClassificationLevel classificationLevel = null;

           if (resourceMetadata.departmentId() != null) {
               department = departmentRepository.findById(resourceMetadata.departmentId())
                   .orElseThrow(() -> new ApiException("Department not found"));
               if (!department.getTenantId().equals(tenantId)) {
                   throw new ApiException("Department does not belong to this tenant");
               }
           }
           if (resourceMetadata.classificationLevelId() != null) {
               classificationLevel = classificationLevelRepository.findById(resourceMetadata.classificationLevelId())
                   .orElseThrow(() -> new ApiException("Classification level not found"));
               if (!classificationLevel.getTenantId().equals(tenantId)) {
                   throw new ApiException("Classification level does not belong to this tenant");
               }
           }

           Resource rawSouce = Resource.builder()
                .assetId(cloudinaryUploadResult.assetId())
                .publicId(cloudinaryUploadResult.publicId())
                .url(cloudinaryUploadResult.url())
                .name(resourceMetadata.name())
                .description(resourceMetadata.description())
                .secureUrl(cloudinaryUploadResult.secureUrl())
                .signature(cloudinaryUploadResult.signature())
                .size((double)file.getSize())
                .format(getFileExtension(file))
                .status(Status.UPLOADED)
                .department(department)
                .classificationLevel(classificationLevel)
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

    @Override
    @Transactional
    public ResourceDetails updateResource(String assetId, ResourceUpdateDTO dto) {
        Resource resource = resourceRepository.findByAssetId(assetId)
                .orElseThrow(() -> new ApiException("Resource not found"));

        if (dto.name() != null) {
            resource.setName(dto.name());
        }
        if (dto.description() != null) {
            resource.setDescription(dto.description());
        }

        Resource updated = resourceRepository.save(resource);
        return mapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteResource(String assetId) {
        Resource resource = resourceRepository.findByAssetId(assetId)
                .orElseThrow(() -> new ApiException("Resource not found"));

        deleteFromCloudinary(resource);
        deleteEmbeddings(resource);

        resourceRepository.delete(resource);
        log.info("Resource deleted: assetId={}, name={}", assetId, resource.getName());
    }

    private void deleteFromCloudinary(Resource resource) {
        try {
            String resourceType = resolveCloudinaryResourceType(resource.getFormat());
            String publicId = resource.getPublicId();
            if (publicId == null || publicId.isBlank()) {
                log.warn("No publicId stored for resource (assetId={}), skipping Cloudinary deletion", resource.getAssetId());
                return;
            }
            cloudinary.uploader().destroy(
                    publicId,
                    Map.of("resource_type", resourceType, "invalidate", true)
            );
        } catch (Exception e) {
            log.error("Failed to delete resource from Cloudinary (assetId={}): {}", resource.getAssetId(), e.getMessage());
            throw new ApiException("Failed to delete resource from storage", e.getMessage());
        }
    }

    private void deleteEmbeddings(Resource resource) {
        try {
            String filterExpression = "sourceId == '" + resource.getResId().toString() + "'";
            vectorStore.delete(filterExpression);
            log.info("Embeddings deleted for resource: resId={}", resource.getResId());
        } catch (Exception e) {
            log.error("Failed to delete embeddings for resource (resId={}): {}", resource.getResId(), e.getMessage());
        }
    }

    private String resolveCloudinaryResourceType(String format) {
        if (format == null) return "raw";
        String ext = format.trim().toLowerCase();
        if (ext.startsWith(".")) ext = ext.substring(1);
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> "image";
            case "mp4", "webm", "mov", "avi" -> "video";
            default -> "raw";
        };
    }

    
}

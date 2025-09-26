package com.intellidesk.cognitia.ingestion.storage.service;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.storage.models.dtos.CloudinaryUploadResult;

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

     @Override
    public CloudinaryUploadResult uploadRawResource(MultipartFile file) {
        

        FileUploadStrategy fileUploadStrategy = fileUploadStrategyFactory.getStrategy(file);
        try {
           CloudinaryUploadResult cloudinaryUploadResult = fileUploadStrategy.upload(file);
           
           return cloudinaryUploadResult;
        } catch (IOException e) {
            throw new ResourceUploadException(e.getMessage());
        }
    }
    
}

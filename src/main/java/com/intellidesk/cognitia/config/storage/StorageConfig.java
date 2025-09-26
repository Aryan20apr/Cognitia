package com.intellidesk.cognitia.config.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {
    
    private StorageProperties cloudinaryProperties;

    @Bean
    Cloudinary cloudinary() {
        return new Cloudinary(cloudinaryProperties.cloudinary_url());
    }


}

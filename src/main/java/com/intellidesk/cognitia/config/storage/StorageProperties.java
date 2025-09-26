package com.intellidesk.cognitia.config.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String cloudinary_url) {}

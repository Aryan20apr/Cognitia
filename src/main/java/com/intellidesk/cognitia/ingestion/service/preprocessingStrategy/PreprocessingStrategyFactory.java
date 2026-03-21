package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PreprocessingStrategyFactory {

    private final DocumentPreprocessingStrategy documentStrategy;
    private final TabularPreprocessingStrategy tabularStrategy;
    private final ImagePreprocessingStrategy imageStrategy;

    private final Set<String> tabularExtensions;
    private final Set<String> imageExtensions;

    public PreprocessingStrategyFactory(
            DocumentPreprocessingStrategy documentStrategy,
            TabularPreprocessingStrategy tabularStrategy,
            ImagePreprocessingStrategy imageStrategy,
            @Value("${ingestion.supported-extensions.tabular:csv,xlsx,xls,ods}") String tabularExts,
            @Value("${ingestion.supported-extensions.image:png,jpg,jpeg,webp,gif}") String imageExts) {

        this.documentStrategy = documentStrategy;
        this.tabularStrategy = tabularStrategy;
        this.imageStrategy = imageStrategy;

        this.tabularExtensions = parseExtensions(tabularExts);
        this.imageExtensions = parseExtensions(imageExts);
    }

    public PreprocessingStrategy getStrategy(String format) {
        String ext = normalizeFormat(format);
        log.info("Selecting preprocessing strategy for format: '{}' (normalized: '{}')", format, ext);

        if (imageExtensions.contains(ext)) {
            return imageStrategy;
        }
        if (tabularExtensions.contains(ext)) {
            return tabularStrategy;
        }
        return documentStrategy;
    }

    private String normalizeFormat(String format) {
        if (format == null) return "";
        String trimmed = format.trim().toLowerCase();
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private Set<String> parseExtensions(String exts) {
        return Stream.of(exts.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}

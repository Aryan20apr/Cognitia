package com.intellidesk.cognitia.ingestion.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.ingestion.models.entities.IngestionOutbox;
import com.intellidesk.cognitia.ingestion.models.entities.RawSouce;
import com.intellidesk.cognitia.ingestion.service.PreprocessingService;
import com.intellidesk.cognitia.ingestion.service.preprocessingStrategy.DocumentPreprocessingStrategy;
import com.intellidesk.cognitia.ingestion.service.preprocessingStrategy.PreprocessingStrategy;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

public class PreprocessingServiceImpl implements PreprocessingService {

    private final PreprocessingStrategy preprocessingStrategy;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)) // connection timeout
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    PreprocessingServiceImpl(PreprocessingStrategy preprocessingStrategy){
        this.preprocessingStrategy = preprocessingStrategy;
    }
   
    @Async
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void preprocessFile(IngestionOutbox ingestoionOutbox){

        RawSouce rawSource = ingestoionOutbox.getSource();
        
        // Use ingestion outbox instead
        String url = rawSource.getUrl();
        log.info("Starting preprocessing for URL: {}", url);

        Path filePth = getLocalFilePath(url, rawSource.getFormat());

        preprocessingStrategy.preprocess(new PathResource(filePth), rawSource);

        

    }


    public Path getLocalFilePath(String url, String suffix) {
        try {
            return downloadFile(url, "resource_", suffix);
        } catch (IOException | InterruptedException e) {
            log.error("Error downloading file from URL {}: {}", url, e.getMessage());
            return null;
        }


    }


    /**
     * Downloads a file from the given URL and saves it to a temporary file.
     *
     * @param fileUrl the URL of the file to download
     * @param prefix  prefix for temp file
     * @param suffix  suffix/extension for temp file (e.g., ".pdf")
     * @return Path to the downloaded temp file
     * @throws IOException
     * @throws InterruptedException
     */
    public static Path downloadFile(String fileUrl, String prefix, String suffix) throws IOException, InterruptedException {
        log.info("Starting download from {}", fileUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .timeout(Duration.ofSeconds(60)) // request timeout
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to download file, HTTP status: " + response.statusCode());
        }

        Path tempFile = Files.createTempFile(prefix, suffix);

        try (InputStream is = response.body()) {
            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Downloaded file saved at {}", tempFile);

        return tempFile;
    }

}

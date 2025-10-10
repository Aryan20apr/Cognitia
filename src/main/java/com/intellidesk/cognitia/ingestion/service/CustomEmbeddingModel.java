package com.intellidesk.cognitia.ingestion.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.intellidesk.cognitia.ingestion.models.dtos.HuggingFaceResponse;

import jakarta.persistence.Convert;
import lombok.AllArgsConstructor;

// @Primary
@AllArgsConstructor
public class CustomEmbeddingModel implements EmbeddingModel {

     private final WebClient webClient;

    public CustomEmbeddingModel(WebClient.Builder webClientBuilder,
                                @Value("${huggingface.api-key}") String apiToken,
                                @Value("${huggingface.embedding-model}") String model) {
        this.webClient = webClientBuilder
                .baseUrl("https://api-inference.huggingface.co/models/" + model)
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {

        // Send request to Hugging Face API
        Map<String, Object> body = Map.of("inputs", request.getInstructions());

        HuggingFaceResponse resp = webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(HuggingFaceResponse.class)
                .block();

        List<Embedding> embeddings = IntStream.range(0, resp.getEmbeddings().size())
        .mapToObj(i -> new Embedding(
                toFloatArray(resp.getEmbeddings().get(i)), // convert List<Double> -> float[]
                i,                                        // index required by constructor
                null                                      // or EmbeddingResultMetadata.EMPTY
        ))
        .collect(Collectors.toList());

        // Create EmbeddingResponse (metadata can be empty)
        return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata());
    }

    // Utility method to convert List<Double> to float[]
private float[] toFloatArray(List<Double> list) {
    float[] arr = new float[list.size()];
    for (int i = 0; i < list.size(); i++) {
        arr[i] = list.get(i).floatValue();
    }
    return arr;
}


    @Override
    public float[] embed(Document document) {
        return embedDocuments(List.of(document)).get(0);
    }

    
    public List<float[]> embedDocuments(List<Document> documents) {
        List<String> texts = documents.stream().map(Document::getText).collect(Collectors.toList());
        EmbeddingResponse response = call(new EmbeddingRequest(texts, null));
        return response.getResults().stream()
                .map(Embedding::getOutput)
                .collect(Collectors.toList());
    }
}
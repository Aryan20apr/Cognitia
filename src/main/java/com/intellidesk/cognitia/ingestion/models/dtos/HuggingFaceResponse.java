package com.intellidesk.cognitia.ingestion.models.dtos;

import java.util.List;

public class HuggingFaceResponse {
    private List<List<Double>> embeddings;

    public List<List<Double>> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<List<Double>> embeddings) {
        this.embeddings = embeddings;
    }
}
package com.tamakara.bakabooru.module.model.service;

import com.tamakara.bakabooru.module.model.client.ModelManagerClient;
import com.tamakara.bakabooru.module.model.dto.ImageEmbeddingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ModelManagerClient modelManagerClient;

    public double[] generateImageEmbedding(MultipartFile file, String modelId) {
        try {
            return toArray(modelManagerClient.imageEmbedding(file, modelId));
        } catch (Exception e) {
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    private double[] toArray(ImageEmbeddingResponseDto response) {
        if (response == null) {
            throw new RuntimeException("Embedding generation returned an empty response");
        }
        List<Double> embedding = response.getEmbedding();
        if (embedding == null || embedding.size() != 512) {
            throw new RuntimeException("Embedding response must contain 512 values");
        }
        return embedding.stream().mapToDouble(Double::doubleValue).toArray();
    }
}

package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AiServiceClient aiServiceClient;

    public double[] generateImageEmbedding(MultipartFile file) {
        try {
            return toArray(aiServiceClient.imageEmbedding(file));
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

package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingRequestDto;
import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final AiServiceClient aiServiceClient;

    /**
     * 为图像生成 CLIP embedding
     * @param objectName MinIO 中图片的对象名称
     * @return 512 维 CLIP 向量
     */
    public double[] generateImageEmbedding(String objectName) {
        ImageEmbeddingRequestDto requestDto = new ImageEmbeddingRequestDto();
        requestDto.setObjectName(objectName);

        try {
            ImageEmbeddingResponseDto response = aiServiceClient.imageEmbedding(requestDto);
            if (!response.isSuccess()) {
                throw new RuntimeException("生成embedding失败: " + response.getError());
            }
            List<Double> embeddingList = response.getEmbedding();
            if (embeddingList == null || embeddingList.isEmpty()) {
                throw new RuntimeException("生成embedding失败: embedding为空");
            }
            // 转换为 double[]
            return embeddingList.stream().mapToDouble(Double::doubleValue).toArray();
        } catch (Exception e) {
            throw new RuntimeException("Embedding生成失败: " + e.getMessage(), e);
        }
    }
}

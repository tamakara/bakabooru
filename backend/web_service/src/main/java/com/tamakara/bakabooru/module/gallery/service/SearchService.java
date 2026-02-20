package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.TagsResponseDto;
import com.tamakara.bakabooru.module.ai.service.EmbeddingService;
import com.tamakara.bakabooru.module.ai.service.ParseQueryService;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.image.dto.ImageThumbnailDto;
import com.tamakara.bakabooru.module.image.dto.SearchDto;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.service.ImageSearchService;
import com.tamakara.bakabooru.module.image.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.DoubleStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ImageSearchService imageSearchService;
    private final ImageMapper imageMapper;
    private final ParseQueryService parseQueryService;
    private final StorageService storageService;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public Page<ImageThumbnailDto> search(SearchRequestDto request) {
        long startTime = System.currentTimeMillis();

        // 1. 基础参数校验
        validateRequest(request);

        SearchDto searchDto = new SearchDto();
        searchDto.setKeyword(StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : "");

        // 2. 构建排序规则
        Sort sort = buildSort(request, searchDto);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        searchDto.setPageable(pageable);

        // 3. 设置过滤参数
        searchDto.setWidthMin(request.getWidthMin());
        searchDto.setWidthMax(request.getWidthMax());
        searchDto.setHeightMin(request.getHeightMin());
        searchDto.setHeightMax(request.getHeightMax());
        searchDto.setSizeMin(request.getSizeMin());
        searchDto.setSizeMax(request.getSizeMax());

        // 4. 解析标签
        Set<String> positiveTags = new HashSet<>();
        Set<String> negativeTags = new HashSet<>();
        parseTags(request.getTags(), positiveTags, negativeTags);

        // 5. 处理语义搜索
        if (StringUtils.hasText(request.getSemanticQuery())) {
            processSemanticQuery(request.getSemanticQuery(), positiveTags, negativeTags, searchDto, isSimilaritySort(request));
        }

        searchDto.setPositiveTags(positiveTags);
        searchDto.setNegativeTags(negativeTags);

        // 6. 执行搜索
        Page<ImageThumbnailDto> result = imageSearchService
                .searchImages(searchDto)
                .map(imageMapper::toThumbnailDto);

        if (log.isDebugEnabled()) {
             log.debug("搜索完成 - 耗时: {}ms, 结果数: {}, 总数: {}",
                    System.currentTimeMillis() - startTime, result.getNumberOfElements(), result.getTotalElements());
        }

        return result;
    }

    public Page<ImageThumbnailDto> searchByImage(MultipartFile file, Double threshold, Integer page, Integer size) {
        String taskId = UUID.randomUUID().toString();
        String objectName = "temp/search/" + taskId;
        File tempFile = null;

        try {
            tempFile = File.createTempFile("search-" + taskId, null);
            file.transferTo(tempFile);

            // 1. 上传图片到MinIO
            storageService.uploadFile(objectName, tempFile);

            // 2. 生成Embedding
            double[] embedding = embeddingService.generateImageEmbedding(objectName);

            // 3. 构建查询
            SearchDto searchDto = new SearchDto();
            searchDto.setEmbedding(DoubleStream.of(embedding).mapToObj(d -> (float) d).toList());

            // 转换相似度阈值为距离阈值 (Cosine Distance = 1 - Similarity)
            if (threshold != null && threshold > 0) {
                // 如果用户输入 0.8 (要求至少80%相似)，则距离必须小于等于 0.2
                searchDto.setDistanceThreshold(Math.max(0, 1.0 - threshold));
            }

            // 默认按相似度排序 (Distance ASC)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "similarity"));
            searchDto.setPageable(pageable);

            // 4. 执行搜索
            return imageSearchService.searchImages(searchDto)
                    .map(imageMapper::toThumbnailDto);

        } catch (Exception e) {
            throw new RuntimeException("以图搜图失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            try {
                storageService.deleteFile(objectName);
            } catch (Exception e) {
                log.warn("清理临时搜索图片失败: {}", objectName);
            }
        }
    }

    private void validateRequest(SearchRequestDto request) {
        if (request.getPage() == null || request.getPage() < 0) {
            throw new IllegalArgumentException("Page index must be non-negative");
        }
        if (request.getSize() == null || request.getSize() <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (!StringUtils.hasText(request.getSort())) {
            throw new IllegalArgumentException("Sort must be non-empty");
        }
    }

    private boolean isSimilaritySort(SearchRequestDto request) {
        String[] parts = request.getSort().split(",");
        return parts.length > 0 && "similarity".equalsIgnoreCase(parts[0].trim());
    }

    private Sort buildSort(SearchRequestDto request, SearchDto searchDto) {
        String[] parts = request.getSort().split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Sort format must be 'property,direction'");
        }

        String property = parts[0].trim();
        String directionStr = parts[1].trim();
        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(directionStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Sort direction must be 'ASC' or 'DESC'");
        }

        if ("random".equalsIgnoreCase(property)) {
            if (!StringUtils.hasText(request.getRandomSeed())) {
                throw new IllegalArgumentException("Random seed must be provided for random sorting");
            }
            searchDto.setRandomSeed(request.getRandomSeed());
            return Sort.unsorted();
        }

        if ("similarity".equalsIgnoreCase(property)) {
            // 相似度排序但无语义描述，回退到随机排序
            if (!StringUtils.hasText(request.getSemanticQuery())) {
                String seed = StringUtils.hasText(request.getRandomSeed())
                        ? request.getRandomSeed()
                        : java.util.UUID.randomUUID().toString();
                searchDto.setRandomSeed(seed);
                return Sort.unsorted();
            }
            return Sort.by(direction, "similarity");
        }

        if (!StringUtils.hasText(property)) {
            throw new IllegalArgumentException("Sort property cannot be empty");
        }
        return Sort.by(direction, property);
    }

    private void processSemanticQuery(String query, Set<String> positive, Set<String> negative, SearchDto searchDto, boolean isSimilarity) {
        try {
            // 提取语义标签
            TagsResponseDto tagResult = parseQueryService.extractTags(query);
            if (tagResult.getPositive() != null) positive.addAll(tagResult.getPositive());
            if (tagResult.getNegative() != null) negative.addAll(tagResult.getNegative());

            // 仅当排序为 similarity 时，调用 CLIP 向量生成
            if (isSimilarity) {
                EmbeddingResponseDto embeddingResult = parseQueryService.generateEmbedding(query);
                if (embeddingResult.getEmbedding() != null) {
                    searchDto.setEmbedding(embeddingResult.getEmbedding().stream()
                            .map(Double::floatValue)
                            .toList());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("语义搜索解析失败: " + e.getMessage(), e);
        }
    }

    private void parseTags(String search, Set<String> positive, Set<String> negative) {
        if (!StringUtils.hasText(search)) return;
        for (String tag : search.trim().split("\\s+")) {
            if (tag.startsWith("-") && tag.length() > 1) {
                negative.add(tag.substring(1));
            } else if (!tag.isEmpty()) {
                positive.add(tag);
            }
        }
    }
}

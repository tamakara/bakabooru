package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.dto.SemanticSearchResponseDto;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.dto.SearchDto;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.service.ImageSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ImageSearchService imageSearchService;
    private final ImageMapper imageMapper;
    private final ParseQueryService parseQueryService;

    @Transactional(readOnly = true)
    public Page<ImageDto> search(SearchRequestDto request) {
        long startTime = System.currentTimeMillis();
        log.info("开始搜索 - 标签: {}, 关键字: {}, 语义查询: {}, 页码: {}, 页大小: {}",
                request.getTags(), request.getKeyword(), request.getSemanticQuery(),
                request.getPage(), request.getSize());

        SearchDto searchDto = new SearchDto();
        if (request.getKeyword() == null) {
            request.setKeyword("");
        } else {
            request.setKeyword(request.getKeyword().trim());
        }

        if (request.getPage() == null || request.getPage() < 0) {
            throw new RuntimeException("Page index must be non-negative");
        }
        int page = request.getPage();

        if (request.getSize() == null || request.getSize() <= 0) {
            throw new RuntimeException("Page size must be greater than zero");
        }
        int size = request.getSize();

        if (!StringUtils.hasText(request.getSort())) {
            throw new RuntimeException("Sort must be non-empty");
        }

        String[] parts = request.getSort().split(",");
        if (parts.length != 2) {
            throw new RuntimeException("Sort format must be 'property,direction'");
        }
        String sortProperty = parts[0].trim();
        String sortDirection = parts[1].trim();

        Sort.Direction direction;
        if ("ASC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        } else if ("DESC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.DESC;
        } else {
            throw new RuntimeException("Sort direction must be 'ASC' or 'DESC'");
        }

        Sort sort;
        if ("RANDOM".equalsIgnoreCase(sortProperty)) {
            if (!StringUtils.hasText(request.getRandomSeed())) {
                throw new RuntimeException("Random seed must be provided for random sorting");
            }
            sort = Sort.unsorted();
            searchDto.setRandomSeed(request.getRandomSeed());
        } else if (StringUtils.hasText(sortProperty)) {
            sort = Sort.by(direction, sortProperty);
        } else {
            throw new RuntimeException("Sort property cannot be empty");
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        searchDto.setPageable(pageable);

        searchDto.setWidthMin(request.getWidthMin());
        searchDto.setWidthMax(request.getWidthMax());
        searchDto.setHeightMin(request.getHeightMin());
        searchDto.setHeightMax(request.getHeightMax());
        searchDto.setSizeMin(request.getSizeMin());
        searchDto.setSizeMax(request.getSizeMax());

        // 解析用户表单中的标签
        Set<String> positiveTags = new HashSet<>();
        Set<String> negativeTags = new HashSet<>();
        parseTags(request.getTags(), positiveTags, negativeTags);

        // 处理语义描述搜索
        if (StringUtils.hasText(request.getSemanticQuery())) {
            try {
                SemanticSearchResponseDto semanticResult = parseQueryService.parseSemanticQuery(request.getSemanticQuery());

                // 合并 AI 生成的标签与用户表单标签
                if (semanticResult.getTags() != null) {
                    if (semanticResult.getTags().getPositive() != null) {
                        positiveTags.addAll(semanticResult.getTags().getPositive());
                    }
                    if (semanticResult.getTags().getNegative() != null) {
                        negativeTags.addAll(semanticResult.getTags().getNegative());
                    }
                }

                // 设置 CLIP 搜索向量
                if (semanticResult.getClipSearch() != null && semanticResult.getClipSearch().getEmbedding() != null) {
                    searchDto.setEmbedding(semanticResult.getClipSearch().getEmbedding().stream()
                            .map(Double::floatValue)
                            .toList());
                    log.info("语义搜索 CLIP 文本: {}", semanticResult.getClipSearch().getText());
                }
            } catch (Exception e) {
                log.warn("语义搜索解析失败，将忽略语义条件: {}", e.getMessage());
            }
        }

        searchDto.setPositiveTags(positiveTags);
        searchDto.setNegativeTags(negativeTags);

        log.info("解析完成 - 正向标签: {}, 负向标签: {}", positiveTags, negativeTags);

        Page<ImageDto> result = imageSearchService
                .searchImages(searchDto)
                .map(imageMapper::toDto);

        log.info("搜索完成 - 总耗时: {}ms, 结果数: {}, 总数: {}",
                System.currentTimeMillis() - startTime, result.getNumberOfElements(), result.getTotalElements());

        return result;
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

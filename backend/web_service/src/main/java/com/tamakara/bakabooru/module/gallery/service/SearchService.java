package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.service.EmbeddingService;
import com.tamakara.bakabooru.module.ai.service.ParseQueryService;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.gallery.dto.SearchResultDto;
import com.tamakara.bakabooru.module.image.dto.ImageThumbnailDto;
import com.tamakara.bakabooru.module.image.dto.SearchDto;
import com.tamakara.bakabooru.module.image.service.ImageSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.DoubleStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ImageSearchService imageSearchService;
    private final ParseQueryService parseQueryService;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public SearchResultDto<ImageThumbnailDto> search(SearchRequestDto request) {
        validateRequest(request);

        SearchDto searchDto = new SearchDto();
        searchDto.setPage(request.getPage());
        searchDto.setSize(request.getSize());
        searchDto.setKeyword(StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : "");
        searchDto.setAiStatus(request.getAiStatus());
        searchDto.setRandomSeed(request.getRandomSeed());
        searchDto.setWidthMin(request.getWidthMin());
        searchDto.setWidthMax(request.getWidthMax());
        searchDto.setHeightMin(request.getHeightMin());
        searchDto.setHeightMax(request.getHeightMax());
        searchDto.setSizeMin(request.getSizeMin());
        searchDto.setSizeMax(request.getSizeMax());
        applySort(request, searchDto);

        Set<String> positiveTags = new HashSet<>();
        Set<String> negativeTags = new HashSet<>();
        parseTags(request.getTags(), positiveTags, negativeTags);
        searchDto.setPositiveTags(positiveTags);
        searchDto.setNegativeTags(negativeTags);

        if (StringUtils.hasText(request.getSemanticQuery())) {
            EmbeddingResponseDto embeddingResult = parseQueryService.generateEmbedding(request.getSemanticQuery());
            if (embeddingResult.getEmbedding() != null) {
                searchDto.setEmbedding(embeddingResult.getEmbedding().stream()
                        .map(Double::floatValue)
                        .toList());
            }
        }

        return imageSearchService.searchImages(searchDto);
    }

    public SearchResultDto<ImageThumbnailDto> searchByImage(MultipartFile file, Double threshold, Integer page, Integer size) {
        double[] embedding = embeddingService.generateImageEmbedding(file);

        SearchDto searchDto = new SearchDto();
        searchDto.setPage(page == null ? 0 : page);
        searchDto.setSize(size == null ? 20 : size);
        searchDto.setEmbedding(DoubleStream.of(embedding).mapToObj(d -> (float) d).toList());
        searchDto.setSortProperty("similarity");
        searchDto.setSortDirection("DESC");

        if (threshold != null && threshold > 0) {
            searchDto.setDistanceThreshold(Math.max(0, 1.0 - threshold));
        }

        return imageSearchService.searchImages(searchDto);
    }

    private void validateRequest(SearchRequestDto request) {
        if (request.getPage() == null || request.getPage() < 0) {
            request.setPage(0);
        }
        if (request.getSize() == null || request.getSize() <= 0) {
            request.setSize(20);
        }
    }

    private void applySort(SearchRequestDto request, SearchDto searchDto) {
        String sort = StringUtils.hasText(request.getSort()) ? request.getSort() : "createdAt,DESC";
        String[] parts = sort.split(",");
        String property = parts.length > 0 && StringUtils.hasText(parts[0]) ? parts[0].trim() : "createdAt";
        String direction = parts.length > 1 && "ASC".equalsIgnoreCase(parts[1].trim()) ? "ASC" : "DESC";

        if ("similarity".equalsIgnoreCase(property) && !StringUtils.hasText(request.getSemanticQuery())) {
            property = "createdAt";
            direction = "DESC";
        }

        searchDto.setSortProperty(property);
        searchDto.setSortDirection(direction);
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

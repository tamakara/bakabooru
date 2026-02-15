package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.dto.ImageSearchDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.service.ImageSearchService;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ImageSearchService imageSearchService;
    private final ImageMapper imageMapper;

    @Transactional(readOnly = true)
    public Page<ImageDto> search(SearchRequestDto request) {
        ImageSearchDto imageSearchDto = new ImageSearchDto();
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
            imageSearchDto.setRandomSeed(request.getRandomSeed());
        } else if (!StringUtils.hasText(sortProperty)) {
            sort = Sort.by(direction, sortProperty);
        } else {
            throw new RuntimeException("Sort property cannot be empty");
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        imageSearchDto.setPageable(pageable);

        imageSearchDto.setWidthMin(request.getWidthMin());
        imageSearchDto.setWidthMax(request.getWidthMax());
        imageSearchDto.setHeightMin(request.getHeightMin());
        imageSearchDto.setHeightMax(request.getHeightMax());
        imageSearchDto.setSizeMin(request.getSizeMin());
        imageSearchDto.setSizeMax(request.getSizeMax());

        Set<String> positiveTags = new HashSet<>();
        Set<String> negativeTags = new HashSet<>();
        parseTags(request.getTags(), positiveTags, negativeTags);

        imageSearchDto.setPositiveTags(positiveTags);
        imageSearchDto.setNegativeTags(negativeTags);

        return imageSearchService.searchImages(imageSearchDto).map(imageMapper::toDto);
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

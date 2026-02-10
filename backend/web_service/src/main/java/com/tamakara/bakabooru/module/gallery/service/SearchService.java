package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.file.service.SignatureService;
import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.gallery.mapper.ImageMapper;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final SignatureService signatureService;

    @Transactional(readOnly = true)
    public Page<ImageDto> search(SearchRequestDto request) {
        int page = (request.getPage() != null && request.getPage() >= 0) ? request.getPage() : 0;
        int size = (request.getSize() != null && request.getSize() > 0) ? request.getSize() : 20;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (hasText(request.getSort())) {
            String[] parts = request.getSort().split(",");
            if (parts.length > 0) {
                String property = parts[0].trim();
                Sort.Direction direction = Sort.Direction.DESC;
                if (parts.length > 1 && "ASC".equalsIgnoreCase(parts[1].trim())) {
                    direction = Sort.Direction.ASC;
                }
                if (!property.isEmpty()) {
                    sort = Sort.by(direction, property);
                }
            }
        }

        boolean isRandomSort = sort.stream().anyMatch(o -> "RANDOM".equals(o.getProperty()));
        Pageable effectivePageable = isRandomSort
                ? PageRequest.of(page, size)
                : PageRequest.of(page, size, sort);

        Specification<Image> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Set<String> includedTags = new HashSet<>();
            Set<String> excludedTags = new HashSet<>();
            parseTagSearch(request.getTagSearch(), includedTags, excludedTags);

            // 包含的标签 (AND)
            for (String tag : includedTags) {
                predicates.add(createTagExistsPredicate(cb, query, root, tag));
            }

            // 排除的标签 (NOT)
            if (!excludedTags.isEmpty()) {
                predicates.add(cb.not(createTagInPredicate(cb, query, root, excludedTags)));
            }

            // 关键字搜索 (Title or FileName)
            if (hasText(request.getKeyword())) {
                String likePattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("fileName")), likePattern)
                ));
            }

            // 尺寸限制
            if (request.getWidthMin() != null) {
                predicates.add(cb.ge(root.get("width"), request.getWidthMin()));
            }
            if (request.getWidthMax() != null) {
                predicates.add(cb.le(root.get("width"), request.getWidthMax()));
            }

            if (request.getHeightMin() != null) {
                predicates.add(cb.ge(root.get("height"), request.getHeightMin()));
            }
            if (request.getHeightMax() != null) {
                predicates.add(cb.le(root.get("height"), request.getHeightMax()));
            }

            if (request.getSizeMin() != null) {
                predicates.add(cb.ge(root.get("size"), request.getSizeMin()));
            }
            if (request.getSizeMax() != null) {
                predicates.add(cb.le(root.get("size"), request.getSizeMax()));
            }

            // 随机排序
            if (isRandomSort && query != null && Long.class != query.getResultType()) {
                if (hasText(request.getRandomSeed())) {
                    // 使用确定性算法实现一致性随机: (id * seed) % MAX_INT
                    long seedVal = request.getRandomSeed().hashCode();
                    // 保证 seed 不为 0 且为正数，提升混淆效果
                    long effectiveSeed = Math.abs(seedVal == 0 ? 0xADEAD1D5L : seedVal);
                    // 简单的乘法哈希排序
                    query.orderBy(cb.asc(
                            cb.function("MOD", Integer.class,
                                    cb.prod(root.get("id"), effectiveSeed),
                                    cb.literal(Integer.MAX_VALUE)
                            )
                    ));
                } else {
                    query.orderBy(cb.asc(cb.function("RANDOM", Double.class)));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return imageRepository.findAll(spec, effectivePageable).map(image -> imageMapper.toDto(image, signatureService));
    }

    private void parseTagSearch(String search, Set<String> included, Set<String> excluded) {
        if (!hasText(search)) return;
        for (String token : search.trim().split("\\s+")) {
            if (token.startsWith("-") && token.length() > 1) {
                excluded.add(token.substring(1));
            } else if (!token.isEmpty()) {
                included.add(token);
            }
        }
    }

    private Predicate createTagExistsPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Image> root, String tag) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Image> subRoot = subquery.from(Image.class);
        Join<Image, Tag> subTags = subRoot.join("tags");
        subquery.select(subRoot.get("id"));
        subquery.where(
                cb.equal(subRoot.get("id"), root.get("id")),
                cb.equal(subTags.get("name"), tag)
        );
        return cb.exists(subquery);
    }

    private Predicate createTagInPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Image> root, Set<String> tags) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Image> subRoot = subquery.from(Image.class);
        Join<Image, Tag> subTags = subRoot.join("tags");
        subquery.select(subRoot.get("id"));
        subquery.where(
                cb.equal(subRoot.get("id"), root.get("id")),
                subTags.get("name").in(tags)
        );
        return cb.exists(subquery);
    }

    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}

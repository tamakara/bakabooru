package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.image.dto.SearchDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.tag.entity.ImageTagRelation;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearchService {

    private final ImageRepository imageRepository;

    @Transactional(readOnly = true)
    public Page<Image> searchImages(SearchDto searchDto) {
        log.info("开始搜索图片 - 正向标签: {}, 负向标签: {}, 关键字: {}",
                searchDto.getPositiveTags(), searchDto.getNegativeTags(), searchDto.getKeyword());

        long startTime = System.currentTimeMillis();

        Specification<Image> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 包含标签 (AND 逻辑: 必须同时拥有所有 positiveTags)
            if (searchDto.getPositiveTags() != null && !searchDto.getPositiveTags().isEmpty()) {
                for (String tag : searchDto.getPositiveTags()) {
                    predicates.add(createTagExistsPredicate(cb, query, root, tag, true));
                }
            }

            // 2. 排除标签 (NOT 逻辑: 不能包含任何一个 negativeTags)
            if (searchDto.getNegativeTags() != null && !searchDto.getNegativeTags().isEmpty()) {
                // 多个排除标签可以用一个 NOT EXISTS + IN 解决，效率更高
                predicates.add(createTagExistsPredicate(cb, query, root, searchDto.getNegativeTags(), false));
            }

            // 3. 关键字模糊搜索 (带 null/empty 检查)
            if (StringUtils.hasText(searchDto.getKeyword())) {
                String pattern = "%" + searchDto.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("fileName")), pattern)
                ));
            }

            // 4. 数值区间搜索重构 (提取方法减少重复代码)
            addRangePredicate(predicates, cb, root.get("width"), searchDto.getWidthMin(), searchDto.getWidthMax());
            addRangePredicate(predicates, cb, root.get("height"), searchDto.getHeightMin(), searchDto.getHeightMax());
            addRangePredicate(predicates, cb, root.get("size"), searchDto.getSizeMin(), searchDto.getSizeMax());

            // 5. 向量相似度搜索
            if (searchDto.getEmbedding() != null && !searchDto.getEmbedding().isEmpty()) {
                String embeddingStr = searchDto.getEmbedding().toString();

                Expression<Double> distance = cb.function("cosine_distance", Double.class,
                        root.get("embedding"),
                        cb.literal(embeddingStr)
                );

                if (!isCountQuery(query)) {
                    applyEmbeddingSort(query, cb, root, searchDto.getPageable(), distance);
                }
            }
            // 6. 随机排序 (互斥逻辑：有向量搜索时不走随机，除非显式支持混合通过 Pageable)
            else if (!isCountQuery(query) && StringUtils.hasText(searchDto.getRandomSeed())) {
                int seedInt = searchDto.getRandomSeed().hashCode();
                applyRandomOrder(root, query, cb, seedInt);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Image> result = imageRepository.findAll(spec, searchDto.getPageable());

        log.info("搜索完成 - 耗时: {}ms, 结果数: {}, 总数: {}",
                System.currentTimeMillis() - startTime, result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    private boolean isCountQuery(CriteriaQuery<?> query) {
        return query != null && (query.getResultType() == Long.class || query.getResultType() == long.class);
    }

    /**
     * 通用的子查询构建工具
     * 使用 ImageTagRelation 表进行关联查询
     *
     * @param isPositive true 表示 EXISTS (包含), false 表示 NOT EXISTS (排除)
     */
    private Predicate createTagExistsPredicate(CriteriaBuilder cb, CommonAbstractCriteria query, Root<Image> root, Object tagValue, boolean isPositive) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<ImageTagRelation> subRoot = subquery.from(ImageTagRelation.class);
        Join<ImageTagRelation, Tag> tagJoin = subRoot.join("tag");

        subquery.select(cb.literal(1)); // 只需 select 1 提高效率

        Predicate tagPredicate;
        if (tagValue instanceof Set) {
            tagPredicate = tagJoin.get("name").in((Set<?>) tagValue);
        } else {
            tagPredicate = cb.equal(tagJoin.get("name"), tagValue);
        }

        subquery.where(
                cb.equal(subRoot.get("image").get("id"), root.get("id")),
                tagPredicate
        );

        return isPositive ? cb.exists(subquery) : cb.not(cb.exists(subquery));
    }

    private void addRangePredicate(List<Predicate> predicates, CriteriaBuilder cb, Path<Number> path, Number min, Number max) {
        if (min != null) predicates.add(cb.ge(path, min));
        if (max != null) predicates.add(cb.le(path, max));
    }

    private void applyEmbeddingSort(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Image> root,
                                    org.springframework.data.domain.Pageable pageable, Expression<Double> distance) {
        if (query == null) return;

        var sort = pageable.getSort();
        if (sort.isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            sort.forEach(order -> {
                if ("similarity".equalsIgnoreCase(order.getProperty()) || "distance".equalsIgnoreCase(order.getProperty())) {
                    orders.add(order.isAscending() ? cb.asc(distance) : cb.desc(distance));
                } else {
                    orders.add(order.isAscending() ? cb.asc(root.get(order.getProperty())) : cb.desc(root.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        } else {
            // 默认按距离升序（最相似在前）
            query.orderBy(cb.asc(distance));
        }
    }

    private void applyRandomOrder(Root<Image> root, CriteriaQuery<?> query, CriteriaBuilder cb, Integer seedInt) {
        // 1. 保证 seed 不为 0 避免乘法失效
        int effectiveSeed = (seedInt == 0) ? 1 : seedInt;

        // 2. 公式: ABS(MOD(id * seed, MAX_INT))
        Expression<Integer> modExpression = cb.function("MOD", Integer.class,
                cb.prod(root.get("id"), effectiveSeed),
                cb.literal(Integer.MAX_VALUE)
        ).as(Integer.class);

        Expression<Integer> hash = cb.abs(modExpression);

        query.orderBy(cb.asc(hash));
    }
}

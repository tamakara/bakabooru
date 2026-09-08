package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.gallery.dto.SearchResultDto;
import com.tamakara.bakabooru.module.image.dto.ImageThumbnailDto;
import com.tamakara.bakabooru.module.image.dto.SearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> IMAGE_STATUSES = Set.of("NORMAL", "ANALYZING", "ERROR", "MISSING");
    private static final Map<String, String> SORT_COLUMNS = new HashMap<>();

    static {
        SORT_COLUMNS.put("title", "i.title");
        SORT_COLUMNS.put("viewCount", "i.view_count");
        SORT_COLUMNS.put("createdAt", "i.created_at");
        SORT_COLUMNS.put("updatedAt", "i.updated_at");
        SORT_COLUMNS.put("size", "i.size");
        SORT_COLUMNS.put("width", "i.width");
        SORT_COLUMNS.put("height", "i.height");
    }

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ImageUrlService imageUrlService;

    @Transactional(readOnly = true)
    public SearchResultDto<ImageThumbnailDto> searchImages(SearchDto searchDto) {
        long startTime = System.currentTimeMillis();

        int page = Math.max(0, searchDto.getPage());
        int size = Math.min(Math.max(1, searchDto.getSize()), MAX_PAGE_SIZE);
        int offset = page * size;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size + 1)
                .addValue("offset", offset);

        List<String> predicates = new ArrayList<>();
        predicates.add("1 = 1");

        applyKeyword(searchDto, predicates, params);
        applyStatus(searchDto, predicates, params);
        applyRanges(searchDto, predicates, params);
        if (!applyTags(searchDto.getPositiveTags(), searchDto.getNegativeTags(), predicates, params)) {
            return new SearchResultDto<>(List.of(), page, size, false);
        }
        applyVector(searchDto, predicates, params);
        applyModelFilters(searchDto, predicates, params);

        String sql = """
                SELECT i.id, i.title, i.hash, i.extension, i.image_status,
                       COALESCE((SELECT array_agg(DISTINCT e.model_id)
                                 FROM image_embeddings e
                                 WHERE e.image_id = i.id AND e.status = 'READY'), ARRAY[]::text[]) AS index_vector_model_ids
                FROM images i
                WHERE %s
                %s
                LIMIT :limit OFFSET :offset
                """.formatted(String.join(" AND ", predicates), buildOrderBy(searchDto));

        List<ImageThumbnailDto> rows = jdbcTemplate.query(sql, params, thumbnailMapper());
        boolean hasNext = rows.size() > size;
        if (hasNext) {
            rows = rows.subList(0, size);
        }

        log.info("图片搜索完成 - 耗时 {}ms, 页码 {}, 返回 {}, hasNext: {}",
                System.currentTimeMillis() - startTime, page, rows.size(), hasNext);
        return new SearchResultDto<>(rows, page, size, hasNext);
    }

    private RowMapper<ImageThumbnailDto> thumbnailMapper() {
        return (rs, rowNum) -> {
            ImageThumbnailDto dto = new ImageThumbnailDto();
            dto.setId(rs.getLong("id"));
            dto.setTitle(rs.getString("title"));
            String hash = rs.getString("hash");
            dto.setThumbnailUrl(imageUrlService.getThumbnailUrl(hash));
            dto.setImageUrl(imageUrlService.getImageUrl(hash, dto.getId(), dto.getTitle(), rs.getString("extension")));
            dto.setStatus(rs.getString("image_status"));
            try {
                java.sql.Array array = rs.getArray("index_vector_model_ids");
                if (array != null) dto.setIndexVectorModelIds(Arrays.asList((String[]) array.getArray()));
            } catch (java.sql.SQLException ignored) {
                dto.setIndexVectorModelIds(List.of());
            }
            return dto;
        };
    }


    private void applyKeyword(SearchDto searchDto, List<String> predicates, MapSqlParameterSource params) {
        if (!StringUtils.hasText(searchDto.getKeyword())) return;
        predicates.add("(LOWER(i.title) LIKE :keyword OR LOWER(i.file_name) LIKE :keyword)");
        params.addValue("keyword", "%" + searchDto.getKeyword().trim().toLowerCase() + "%");
    }

    private void applyRanges(SearchDto searchDto, List<String> predicates, MapSqlParameterSource params) {
        addRange("i.width", "widthMin", "widthMax", searchDto.getWidthMin(), searchDto.getWidthMax(), predicates, params);
        addRange("i.height", "heightMin", "heightMax", searchDto.getHeightMin(), searchDto.getHeightMax(), predicates, params);
        addRange("i.size", "sizeMin", "sizeMax", searchDto.getSizeMin(), searchDto.getSizeMax(), predicates, params);
    }

    private void addRange(String column, String minName, String maxName, Number min, Number max,
                          List<String> predicates, MapSqlParameterSource params) {
        if (min != null) {
            predicates.add(column + " >= :" + minName);
            params.addValue(minName, min);
        }
        if (max != null) {
            predicates.add(column + " <= :" + maxName);
            params.addValue(maxName, max);
        }
    }

    private boolean applyTags(Set<String> positiveTags, Set<String> negativeTags,
                              List<String> predicates, MapSqlParameterSource params) {
        List<Long> positiveIds = resolveTagIds(positiveTags);
        if (positiveTags != null && positiveIds.size() != positiveTags.size()) {
            return false;
        }
        if (!positiveIds.isEmpty()) {
            predicates.add("""
                    i.id IN (
                        SELECT itr.image_id
                        FROM image_tag_relation itr
                        WHERE itr.tag_id IN (:positiveTagIds)
                        GROUP BY itr.image_id
                        HAVING COUNT(DISTINCT itr.tag_id) = :positiveTagCount
                    )
                    """);
            params.addValue("positiveTagIds", positiveIds);
            params.addValue("positiveTagCount", positiveIds.size());
        }

        List<Long> negativeIds = resolveTagIds(negativeTags);
        if (!negativeIds.isEmpty()) {
            predicates.add("""
                    NOT EXISTS (
                        SELECT 1
                        FROM image_tag_relation itr_neg
                        WHERE itr_neg.image_id = i.id
                          AND itr_neg.tag_id IN (:negativeTagIds)
                    )
                    """);
            params.addValue("negativeTagIds", negativeIds);
        }
        return true;
    }

    private List<Long> resolveTagIds(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return List.of();
        List<String> names = tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
        if (names.isEmpty()) return List.of();
        return jdbcTemplate.queryForList("SELECT id FROM tags WHERE name IN (:names)",
                new MapSqlParameterSource("names", names), Long.class);
    }

    private void applyVector(SearchDto searchDto, List<String> predicates, MapSqlParameterSource params) {
        if (searchDto.getEmbedding() == null || searchDto.getEmbedding().isEmpty()) return;
        if (!StringUtils.hasText(searchDto.getVectorModelId())) {
            throw new IllegalArgumentException("vectorModelId is required for vector search");
        }
        String modelId = searchDto.getVectorModelId().trim();
        predicates.add("EXISTS (SELECT 1 FROM image_embeddings ev WHERE ev.image_id = i.id AND ev.model_id = :embeddingModelId AND ev.status = 'READY')");
        params.addValue("embeddingModelId", modelId);
        params.addValue("embedding", toVectorLiteral(searchDto.getEmbedding()));
        if (searchDto.getDistanceThreshold() != null) {
            predicates.add("(SELECT ev.embedding <=> CAST(:embedding AS vector) FROM image_embeddings ev WHERE ev.image_id = i.id AND ev.model_id = :embeddingModelId AND ev.status = 'READY' LIMIT 1) <= :distanceThreshold");
            params.addValue("distanceThreshold", searchDto.getDistanceThreshold());
        }
    }

    private void applyStatus(SearchDto searchDto, List<String> predicates, MapSqlParameterSource params) {
        String value = searchDto.getStatus();
        if (!StringUtils.hasText(value)) return;
        String status = value.trim().toUpperCase();
        if (!IMAGE_STATUSES.contains(status)) return;
        predicates.add("i.image_status = :imageStatus");
        params.addValue("imageStatus", status);
    }

    private void applyModelFilters(SearchDto searchDto, List<String> predicates, MapSqlParameterSource params) {
        if (searchDto.getVectorModelIds() != null && !searchDto.getVectorModelIds().isEmpty()) {
            predicates.add("EXISTS (SELECT 1 FROM image_embeddings ef WHERE ef.image_id = i.id "
                    + "AND ef.status = 'READY' AND ef.model_id IN (:vectorModelIds) "
                    + "GROUP BY ef.image_id HAVING COUNT(DISTINCT ef.model_id) = :vectorModelCount)");
            params.addValue("vectorModelIds", searchDto.getVectorModelIds());
            params.addValue("vectorModelCount", searchDto.getVectorModelIds().stream().distinct().count());
        }
    }

    private String buildOrderBy(SearchDto searchDto) {
        if (searchDto.getEmbedding() != null && !searchDto.getEmbedding().isEmpty()) {
            if (!StringUtils.hasText(searchDto.getVectorModelId())) {
                throw new IllegalArgumentException("vectorModelId is required for vector search");
            }
            String modelId = searchDto.getVectorModelId().trim();
            return "ORDER BY (SELECT ev.embedding <=> CAST(:embedding AS vector) FROM image_embeddings ev WHERE ev.image_id = i.id AND ev.model_id = '" + modelId.replace("'", "''") + "' AND ev.status = 'READY' LIMIT 1), i.id ASC";
        }
        if ("random".equalsIgnoreCase(searchDto.getSortProperty()) && StringUtils.hasText(searchDto.getRandomSeed())) {
            int seed = searchDto.getRandomSeed().hashCode();
            if (seed == 0) seed = 1;
            return "ORDER BY ABS(MOD((i.id * " + seed + ")::bigint, 2147483647::bigint)) ASC";
        }
        String property = StringUtils.hasText(searchDto.getSortProperty()) ? searchDto.getSortProperty() : "createdAt";
        String column = SORT_COLUMNS.getOrDefault(property, "i.created_at");
        String direction = "ASC".equalsIgnoreCase(searchDto.getSortDirection()) ? "ASC" : "DESC";
        return "ORDER BY " + column + " " + direction + ", i.id DESC";
    }

    private String toVectorLiteral(List<Float> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}


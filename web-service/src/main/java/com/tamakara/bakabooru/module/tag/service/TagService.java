package com.tamakara.bakabooru.module.tag.service;

import com.tamakara.bakabooru.module.ai.dto.TagImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageResponseDto;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.tag.dto.TagDto;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.mapper.TagMapper;
import com.tamakara.bakabooru.module.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final AiServiceClient aiServiceClient;
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<TagDto> listTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<TagDto> searchTags(String query) {
        long startTime = System.currentTimeMillis();

        // 默认返回前 20 条匹配结果
        List<TagDto> results = tagRepository.searchTags(query, PageRequest.of(0, 20))
                .stream()
                .map(tagMapper::toDto)
                .toList();

        if (log.isDebugEnabled()) {
            log.debug("标签搜索 - 关键词: {}, 结果: {}, 耗时: {}ms", query, results.size(), System.currentTimeMillis() - startTime);
        }
        return results;
    }

    public Tag getTagById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在: " + id));
    }

    public Tag getTagByName(String name) {
        return tagRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("标签不存在: " + name));
    }

    public Map<String, Double> tagImage(String objectName, double threshold) {

        TagImageRequestDto requestBody = new TagImageRequestDto();
        requestBody.setObjectName(objectName);
        requestBody.setThreshold(threshold);

        try {
            TagImageResponseDto response = aiServiceClient.tagImage(requestBody);
            return response.getData();
        } catch (Exception e) {
            throw new RuntimeException("标签生成失败: " + e.getMessage());
        }
    }
}

package com.tamakara.bakabooru.module.tag.service;

import com.tamakara.bakabooru.module.ai.dto.TagImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageResponseDto;
import com.tamakara.bakabooru.module.ai.service.AiService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.dto.TagDto;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.mapper.TagMapper;
import com.tamakara.bakabooru.module.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final SystemSettingService systemSettingService;
    private final AiService aiService;
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<TagDto> listTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<TagDto> searchTags(String query) {
        String lowerQuery = query.toLowerCase();
        return tagRepository.findByNameContainingIgnoreCase(query).stream()
                .sorted((t1, t2) -> {
                    String n1 = t1.getName().toLowerCase();
                    String n2 = t2.getName().toLowerCase();
                    boolean s1 = n1.startsWith(lowerQuery);
                    boolean s2 = n2.startsWith(lowerQuery);

                    if (s1 && !s2) {
                        return -1;
                    } else if (!s1 && s2) {
                        return 1;
                    } else {
                        return Integer.compare(n1.length(), n2.length());
                    }
                })
                .map(tagMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Tag findOrCreateTag(String name, String type) {
        return tagRepository.findByName(name)
                .orElseGet(() -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    tag.setType(type);
                    return tagRepository.save(tag);
                });
    }

    public Tag findTag(String name) {
        return tagRepository.findByName(name).orElse(null);
    }

    public Map<String, List<String>> tagImage(String imagePath) {
        double threshold = systemSettingService.getDoubleSetting("tag.threshold");
        TagImageRequestDto requestBody = new TagImageRequestDto();
        requestBody.setImagePath(imagePath);
        requestBody.setThreshold(threshold);

        try {
            TagImageResponseDto response = aiService.tagImage(requestBody);
            return response.getData();
        } catch (Exception e) {
            throw new RuntimeException("标签生成失败: " + e.getMessage());
        }
    }
}

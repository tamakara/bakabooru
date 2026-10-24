package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.image.dto.TagDto;
import com.tamakara.bakabooru.module.image.entity.Tag;
import com.tamakara.bakabooru.module.image.mapper.TagMapper;
import com.tamakara.bakabooru.module.image.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<TagDto> listTags() {
        return tagRepository.findAll().stream().map(tagMapper::toDto).toList();
    }

    public List<TagDto> searchTags(String query) {
        return tagRepository.searchTags(query, PageRequest.of(0, 20)).stream()
                .map(tagMapper::toDto)
                .toList();
    }

    public Tag getTagById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + id));
    }

    public Tag getTagByName(String name) {
        return tagRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + name));
    }
}

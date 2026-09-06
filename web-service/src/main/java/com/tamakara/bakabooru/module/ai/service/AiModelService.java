package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.dto.AiModelDto;
import com.tamakara.bakabooru.module.ai.entity.AiModel;
import com.tamakara.bakabooru.module.ai.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModelService {
    private final AiModelRepository repository;

    @Transactional(readOnly = true)
    public List<AiModelDto> list() {
        return repository.findAllByOrderByNameAsc().stream().map(AiModelDto::from).toList();
    }

    @Transactional
    public AiModelDto setEnabled(String id, boolean enabled) {
        AiModel model = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("濡€崇€锋稉宥呯摠閸? " + id));
        model.setStatus(enabled ? "READY" : "DISABLED");
        model.setUpdatedAt(Instant.now());
        return AiModelDto.from(repository.save(model));
    }

    @Transactional
    public AiModelDto markDownloadStarted(String id) {
        AiModel model = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("濡€崇€锋稉宥呯摠閸? " + id));
        // The AI service owns the actual runtime cache; the registry records availability.
        model.setStatus("READY");
        model.setUpdatedAt(Instant.now());
        return AiModelDto.from(repository.save(model));
    }

    @Transactional
    public AiModelDto uninstall(String id) {
        AiModel model = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("濡€崇€锋稉宥呯摠閸? " + id));
        model.setStatus("DISABLED");
        model.setArtifactObject(null);
        model.setUpdatedAt(Instant.now());
        return AiModelDto.from(repository.save(model));
    }
}

package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.dto.AiModelDto;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.ai.entity.AiModel;
import com.tamakara.bakabooru.module.ai.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AiModelService {
    private final AiModelRepository repository;
    private final AiServiceClient aiServiceClient;

    @Transactional
    public List<AiModelDto> list() {
        List<AiModel> models = repository.findAllByOrderByNameAsc();
        try {
            List<?> remote = aiServiceClient.modelCatalog();
            final List<?> remoteCatalog = remote == null ? List.of() : remote;
            var states = remoteCatalog.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<?, ?>) item)
                    .filter(item -> item.get("id") != null && item.get("artifactState") != null)
                    .collect(java.util.stream.Collectors.toMap(item -> item.get("id").toString(), item -> item.get("artifactState").toString(), (a, b) -> b));
            var errors = remoteCatalog.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<?, ?>) item)
                    .filter(item -> item.get("id") != null && item.get("errorMessage") != null)
                    .collect(java.util.stream.Collectors.toMap(item -> item.get("id").toString(), item -> item.get("errorMessage").toString(), (a, b) -> b));
            models.forEach(model -> {
                String state = states.get(model.getId());
                if (state != null && !state.equals(model.getStatus())) {
                    model.setStatus(state);
                    model.setUpdatedAt(Instant.now());
                }
            });
            repository.saveAll(models);
            List<AiModelDto> result = new ArrayList<>(models.stream().map(model -> {
                AiModelDto dto = AiModelDto.from(model);
                dto.setErrorMessage(errors.get(model.getId()));
                return dto;
            }).toList());
            java.util.Set<String> knownIds = models.stream().map(AiModel::getId).collect(java.util.stream.Collectors.toSet());
            remoteCatalog.stream().filter(Map.class::isInstance).map(item -> (Map<?, ?>) item)
                    .filter(item -> item.get("id") != null && !knownIds.contains(item.get("id").toString()))
                    .forEach(item -> result.add(AiModelDto.fromRemote(item)));
            result.sort(java.util.Comparator.comparing(AiModelDto::getName, java.util.Comparator.nullsLast(String::compareToIgnoreCase)));
            return result;
        } catch (RuntimeException ignored) {
            // The web service remains usable while the AI service is starting.
        }
        return models.stream().map(AiModelDto::from).toList();
    }

    @Transactional
    public AiModelDto markDownloadStarted(String id) {
        AiModel model = repository.findById(id).orElse(null);
        if (model == null) {
            try {
                aiServiceClient.downloadModel(id);
                return list().stream().filter(item -> id.equals(item.getId())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("Model not found: " + id, error);
            }
        }
        model.setStatus("DOWNLOADING");
        model.setUpdatedAt(Instant.now());
        AiModelDto result = AiModelDto.from(repository.save(model));
        try {
            String state = aiServiceClient.downloadModel(id);
            model.setStatus(state);
            model.setUpdatedAt(Instant.now());
            result = AiModelDto.from(repository.save(model));
        } catch (RuntimeException error) {
            model.setStatus("FAILED");
            repository.save(model);
            throw error;
        }
        return result;
    }

    @Transactional
    public AiModelDto uninstall(String id) {
        AiModel model = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
        model.setStatus("NOT_INSTALLED");
        model.setUpdatedAt(Instant.now());
        return AiModelDto.from(repository.save(model));
    }
}

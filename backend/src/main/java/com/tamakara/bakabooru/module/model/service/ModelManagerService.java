package com.tamakara.bakabooru.module.model.service;

import com.tamakara.bakabooru.module.model.dto.ModelDto;
import com.tamakara.bakabooru.module.model.client.ModelManagerClient;
import com.tamakara.bakabooru.module.model.entity.Model;
import com.tamakara.bakabooru.module.model.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ModelManagerService {
    private final ModelRepository repository;
    private final ModelManagerClient modelManagerClient;

    @Transactional
    public List<ModelDto> list() {
        List<Model> models = repository.findAllByOrderByNameAsc();
        try {
            List<?> remote = modelManagerClient.modelCatalog();
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
            List<ModelDto> result = new ArrayList<>(models.stream().map(model -> {
                ModelDto dto = ModelDto.from(model);
                dto.setErrorMessage(errors.get(model.getId()));
                return dto;
            }).toList());
            java.util.Set<String> knownIds = models.stream().map(Model::getId).collect(java.util.stream.Collectors.toSet());
            remoteCatalog.stream().filter(Map.class::isInstance).map(item -> (Map<?, ?>) item)
                    .filter(item -> item.get("id") != null && !knownIds.contains(item.get("id").toString()))
                    .forEach(item -> result.add(ModelDto.fromRemote(item)));
            result.sort(java.util.Comparator.comparing(ModelDto::getName, java.util.Comparator.nullsLast(String::compareToIgnoreCase)));
            return result;
        } catch (RuntimeException ignored) {
            // The web service remains usable while the AI service is starting.
        }
        return models.stream().map(ModelDto::from).toList();
    }

    @Transactional
    public ModelDto markDownloadStarted(String id) {
        Model model = repository.findById(id).orElse(null);
        if (model == null) {
            try {
                modelManagerClient.downloadModel(id);
                return list().stream().filter(item -> id.equals(item.getId())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("Model not found: " + id, error);
            }
        }
        model.setStatus("DOWNLOADING");
        model.setUpdatedAt(Instant.now());
        ModelDto result = ModelDto.from(repository.save(model));
        try {
            String state = modelManagerClient.downloadModel(id);
            model.setStatus(state);
            model.setUpdatedAt(Instant.now());
            result = ModelDto.from(repository.save(model));
        } catch (RuntimeException error) {
            model.setStatus("FAILED");
            repository.save(model);
            throw error;
        }
        return result;
    }

    @Transactional
    public ModelDto uninstall(String id) {
        Model model = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
        model.setStatus("NOT_INSTALLED");
        model.setUpdatedAt(Instant.now());
        return ModelDto.from(repository.save(model));
    }
}

package com.tamakara.bakabooru.module.model.service;

import com.tamakara.bakabooru.module.model.dto.ModelDto;
import com.tamakara.bakabooru.module.model.client.ModelManagerClient;
import com.tamakara.bakabooru.module.model.entity.Model;
import com.tamakara.bakabooru.module.model.repository.ModelRepository;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ModelManagerService {
    private final ModelRepository repository;
    private final ModelManagerClient modelManagerClient;
    private final SystemSettingService settingService;

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
                boolean enabled = model.getId().equals("TAGGER".equals(model.getModelType()) ? settingService.getActiveTaggerId() : settingService.getActiveClipId());
                ModelDto dto = ModelDto.from(model, enabled, errors.get(model.getId()));
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
        return models.stream().map(model -> ModelDto.from(model,
                model.getId().equals("TAGGER".equals(model.getModelType()) ? settingService.getActiveTaggerId() : settingService.getActiveClipId()), null)).toList();
    }

    @Scheduled(fixedDelayString = "${app.model-manager.status-sync-interval-ms:15000}", initialDelayString = "${app.model-manager.status-sync-initial-delay-ms:5000}")
    public void synchronizeArtifactStates() {
        list();
    }

    @Transactional
    public ModelDto markDownloadStarted(String id) {
        list();
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
        // Downloads are asynchronous. A complete artifact must be returned as-is
        // so polling clients never restart an already finished download.
        if ("READY".equals(model.getStatus()) || "RESTART_REQUIRED".equals(model.getStatus())) {
            return ModelDto.from(model,
                    model.getId().equals("TAGGER".equals(model.getModelType())
                            ? settingService.getActiveTaggerId() : settingService.getActiveClipId()), null);
        }
        model.setStatus("DOWNLOADING");
        model.setUpdatedAt(Instant.now());
        String state = modelManagerClient.downloadModel(id);
        if (List.of("DOWNLOADING", "RESTART_REQUIRED", "READY", "FAILED").contains(state)) {
            model.setStatus(state);
        }
        return ModelDto.from(repository.save(model), false, null);
    }

    @Transactional
    public ModelDto enable(String id) {
        // Refresh the persisted artifact state before enforcing the READY rule;
        // model-manager may have completed a download since the last listing.
        list();
        Model model = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
        if (!"READY".equals(model.getStatus())) throw new IllegalStateException("模型尚未就绪，无法启用");
        String type = model.getModelType() == null ? "CLIP" : model.getModelType();
        settingService.updateSettings(Map.of("TAGGER".equals(type) ? SystemSettingService.ACTIVE_TAGGER : SystemSettingService.ACTIVE_CLIP, id));
        return ModelDto.from(model, true, null);
    }
}

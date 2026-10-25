package com.tamakara.bakabooru.module.model.client;

import com.tamakara.bakabooru.module.model.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.model.dto.ImageAnalysisRequestDto;
import com.tamakara.bakabooru.module.model.dto.ImageAnalysisResponseDto;
import com.tamakara.bakabooru.module.model.dto.ImageEmbeddingResponseDto;
import com.tamakara.bakabooru.module.model.dto.SemanticSearchRequestDto;
import com.tamakara.bakabooru.config.ModelManagerProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

@Component
public class ModelManagerClient {

    private static final Duration MODEL_MANAGER_TIMEOUT = Duration.ofSeconds(120);

    private final WebClient webClient;
    private final String baseUrl;

    public ModelManagerClient(
            ModelManagerProperties modelManagerProperties,
            WebClient.Builder webClientBuilder
    ) {
        this.webClient = webClientBuilder.build();
        this.baseUrl = modelManagerProperties.getUrl().replaceAll("/+$", "");
    }

    private String url(String path) {
        return baseUrl + path;
    }

    public ImageAnalysisResponseDto analyzeImage(ImageAnalysisRequestDto requestBody) {
        return webClient
                .post()
                .uri(url("/v1/images/analyze"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ImageAnalysisResponseDto.class)
                .block(MODEL_MANAGER_TIMEOUT);
    }

    public ImageAnalysisResponseDto analyzeTags(ImageAnalysisRequestDto requestBody) {
        // The capability endpoint has a deliberately small contract: model_id,
        // threshold and object_name. Do not forward the combined analysis DTO,
        // whose field is named tag_model_id.
        Map<String, Object> body = Map.of(
                "object_name", requestBody.getObjectName(),
                "model_id", requestBody.getTagModelId(),
                "threshold", requestBody.getThreshold());
        return webClient.post().uri(url("/v1/images/tags")).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body).retrieve().bodyToMono(ImageAnalysisResponseDto.class).block(MODEL_MANAGER_TIMEOUT);
    }

    public ImageAnalysisResponseDto analyzeVectors(ImageAnalysisRequestDto requestBody) {
        return webClient.post().uri(url("/v1/images/vectors")).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody).retrieve().bodyToMono(ImageAnalysisResponseDto.class).block(MODEL_MANAGER_TIMEOUT);
    }

    public ImageEmbeddingResponseDto imageEmbedding(MultipartFile file, String modelId) {
        return webClient
                .post().uri(url("/v1/embeddings/image-file") + "?model_id={modelId}", modelId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", file.getResource()))
                .retrieve()
                .bodyToMono(ImageEmbeddingResponseDto.class)
                .block(MODEL_MANAGER_TIMEOUT);
    }

    public EmbeddingResponseDto generateEmbedding(SemanticSearchRequestDto requestBody) {
        return webClient
                .post()
                .uri(url("/v1/embeddings/text"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(EmbeddingResponseDto.class)
                .block(MODEL_MANAGER_TIMEOUT);
    }

    public String downloadModel(String modelId) {
        Map<?, ?> result = webClient.post().uri(url("/v1/models/" + modelId + "/download"))
                .retrieve().bodyToMono(Map.class).block(MODEL_MANAGER_TIMEOUT);
        Object state = result == null ? null : result.get("artifactState");
        return state == null ? "DOWNLOADING" : state.toString();
    }

    public Map<String, String> modelStates() {
        List<?> models = modelCatalog();
        Map<String, String> states = new HashMap<>();
        if (models != null) {
            for (Object item : models) {
                if (item instanceof Map<?, ?> model && model.get("id") != null && model.get("artifactState") != null) {
                    states.put(model.get("id").toString(), model.get("artifactState").toString());
                }
            }
        }
        return states;
    }

    public Map<String, String> modelErrors() {
        List<?> models = modelCatalog();
        Map<String, String> errors = new HashMap<>();
        if (models != null) {
            for (Object item : models) {
                if (item instanceof Map<?, ?> model && model.get("id") != null && model.get("errorMessage") != null) {
                    errors.put(model.get("id").toString(), model.get("errorMessage").toString());
                }
            }
        }
        return errors;
    }

    public List<?> modelCatalog() {
        return webClient.get().uri(url("/v1/models")).retrieve().bodyToMono(List.class).block(MODEL_MANAGER_TIMEOUT);
    }

    public void updateRuntimeSettings(Map<String, Object> settings) {
        webClient.post().uri(url("/v1/runtime-settings"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(settings)
                .retrieve().toBodilessEntity().block(Duration.ofSeconds(10));
    }
}

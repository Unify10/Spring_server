package com.unify.onyak.domain.drugs.service;

import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

/**
 * 3장 의약품 검색/OCR. Python_server(FastAPI) 응답을 그대로 돌려준다.
 * 응답 스키마는 Python_server 가 가지므로 필드가 바뀌면 앱 응답도 그대로 바뀐다.
 */
public interface DrugProxyService {

    JsonNode search(String q, int limit, boolean otcOnly);

    JsonNode barcode(String code);

    JsonNode detail(String drugId);

    JsonNode dur(String drugId, Integer age, Boolean pregnant);

    JsonNode ocr(MultipartFile file);

    JsonNode analyze(MultipartFile file, Integer age, Boolean pregnant);
}

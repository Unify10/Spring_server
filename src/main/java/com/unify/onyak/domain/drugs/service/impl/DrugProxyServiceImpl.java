package com.unify.onyak.domain.drugs.service.impl;

import com.unify.onyak.domain.drugs.client.DrugProxyClient;
import com.unify.onyak.domain.drugs.service.DrugProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

/**
 * 지금은 그대로 넘기기만 한다. 검색 결과 캐시나 사용자별 호출 제한처럼 Spring 쪽 규칙이 생기면 여기에 둔다.
 * DB 를 쓰지 않으므로 트랜잭션도 없다.
 */
@Service
@RequiredArgsConstructor
public class DrugProxyServiceImpl implements DrugProxyService {

    private final DrugProxyClient drugProxyClient;

    @Override
    public JsonNode search(String q, int limit, boolean otcOnly) {
        return drugProxyClient.search(q, limit, otcOnly);
    }

    @Override
    public JsonNode barcode(String code) {
        return drugProxyClient.barcode(code);
    }

    @Override
    public JsonNode detail(String drugId) {
        return drugProxyClient.detail(drugId);
    }

    @Override
    public JsonNode dur(String drugId, Integer age, Boolean pregnant) {
        return drugProxyClient.dur(drugId, age, pregnant);
    }

    @Override
    public JsonNode ocr(MultipartFile file) {
        return drugProxyClient.ocr(file);
    }

    @Override
    public JsonNode analyze(MultipartFile file, Integer age, Boolean pregnant) {
        return drugProxyClient.analyze(file, age, pregnant);
    }
}

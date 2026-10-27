package com.unify.onyak.domain.drugs.controller;

import com.unify.onyak.domain.drugs.service.DrugProxyService;
import com.unify.onyak.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

/**
 * 3장 의약품 검색/OCR. Python_server(FastAPI)의 같은 경로를 그대로 넘기고, 응답 본문(data)도
 * FastAPI 응답 그대로다. 앱은 Python_server 를 직접 부르지 않는다.
 * 권한: 로그인한 보호자·피보호자 모두. 약장에 담을 때 쓰는 item_seq 는 여기 응답의 drug_id 다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/drugs")
public class DrugController {

    private static final String DRUG_ID = "^\\d{1,20}$";

    private final DrugProxyService drugProxyService;

    // 3.4 의약품 검색
    @GetMapping("/search")
    public ApiResponse<JsonNode> search(
            @RequestParam @Size(min = 2, max = 100) String q,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(name = "otc_only", defaultValue = "false") boolean otcOnly
    ) {
        return ApiResponse.ok(drugProxyService.search(q, limit, otcOnly));
    }

    // 3.3 바코드 조회
    @GetMapping("/barcode/{code}")
    public ApiResponse<JsonNode> barcode(@PathVariable @Pattern(regexp = "^\\d{8,14}$") String code) {
        return ApiResponse.ok(drugProxyService.barcode(code));
    }

    // 3.5 약품 상세
    @GetMapping("/{drugId}")
    public ApiResponse<JsonNode> detail(@PathVariable @Pattern(regexp = DRUG_ID) String drugId) {
        return ApiResponse.ok(drugProxyService.detail(drugId));
    }

    // DUR 금기·주의 점검
    @GetMapping("/{drugId}/dur")
    public ApiResponse<JsonNode> dur(
            @PathVariable @Pattern(regexp = DRUG_ID) String drugId,
            @RequestParam(required = false) @Min(0) @Max(120) Integer age,
            @RequestParam(required = false) Boolean pregnant
    ) {
        return ApiResponse.ok(drugProxyService.dur(drugId, age, pregnant));
    }

    // 3.1 카메라촬영 / 3.2 갤러리업로드 — 약품명 후보까지
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JsonNode> ocr(@RequestPart MultipartFile file) {
        return ApiResponse.ok(drugProxyService.ocr(file));
    }

    // 사진 한 장 → 판정까지
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JsonNode> analyze(
            @RequestPart MultipartFile file,
            @RequestParam(required = false) @Min(0) @Max(120) Integer age,
            @RequestParam(required = false) Boolean pregnant
    ) {
        return ApiResponse.ok(drugProxyService.analyze(file, age, pregnant));
    }
}

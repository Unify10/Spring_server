package com.unify.onyak.domain.drugs.service;

import com.unify.onyak.domain.drugs.entity.Drug;

public interface DrugService {

    /** drugs 에 있으면 그대로, 없으면 FastAPI 에서 받아 저장한 뒤 반환한다. */
    Drug getOrFetch(String itemSeq);
}

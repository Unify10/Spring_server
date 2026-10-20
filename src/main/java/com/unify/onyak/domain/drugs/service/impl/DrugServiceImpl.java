package com.unify.onyak.domain.drugs.service.impl;

import com.unify.onyak.domain.drugs.client.DrugApiClient;
import com.unify.onyak.domain.drugs.client.dto.DrugDetail;
import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.drugs.repository.DrugRepository;
import com.unify.onyak.domain.drugs.service.DrugService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DrugServiceImpl implements DrugService {

    // "[M040702]아세트아미노펜|카페인무수물" 같은 원장 표기에서 코드를 걷어내고 성분을 나눈다
    private static final Pattern INGREDIENT_CODE = Pattern.compile("\\[[^\\]]*]");
    private static final Pattern INGREDIENT_SEPARATOR = Pattern.compile("[|,+]");

    private final DrugRepository drugRepository;
    private final DrugApiClient drugApiClient;
    private final TransactionTemplate newTransaction;

    public DrugServiceImpl(DrugRepository drugRepository, DrugApiClient drugApiClient,
                           PlatformTransactionManager transactionManager) {
        this.drugRepository = drugRepository;
        this.drugApiClient = drugApiClient;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Drug getOrFetch(String itemSeq) {
        return drugRepository.findByItemSeq(itemSeq)
                .orElseGet(() -> fetchAndSave(itemSeq));
    }

    /**
     * FastAPI 호출이 들어 있으므로 트랜잭션 밖에서 불러야 한다. 바깥 트랜잭션 안에서 부르면
     * 응답을 기다리는 동안 커넥션을 쥐고, 저장(REQUIRES_NEW)이 커넥션을 하나 더 요구한다.
     */
    private Drug fetchAndSave(String itemSeq) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            log.warn("DrugService.getOrFetch 가 트랜잭션 안에서 호출됐습니다 (itemSeq={}). "
                    + "FastAPI 응답을 기다리는 동안 DB 커넥션을 점유합니다.", itemSeq);
        }
        DrugDetail detail = drugApiClient.getDrug(itemSeq);
        Drug drug = Drug.create(
                itemSeq,
                detail.itemName() == null ? itemSeq : detail.itemName(),
                splitIngredients(detail.mainIngredient()),
                detail.entpName(),
                detail.efficacy(),
                detail.usage(),
                detail.caution()
        );
        try {
            // 저장만 짧은 트랜잭션으로 한다. 같은 약을 동시에 담아 item_seq UNIQUE 위반이 나도
            // 호출한 쪽 트랜잭션이 rollback-only 로 깨지지 않게 REQUIRES_NEW 로 둔다.
            return newTransaction.execute(status -> drugRepository.saveAndFlush(drug));
        } catch (DataIntegrityViolationException e) {
            // 다른 요청이 먼저 저장했다 → 그걸 쓴다
            return drugRepository.findByItemSeq(itemSeq)
                    .orElseThrow(() -> new DrugApiUnavailableException(e));
        }
    }

    static List<String> splitIngredients(String mainIngredient) {
        if (mainIngredient == null || mainIngredient.isBlank()) {
            return List.of();
        }
        String withoutCodes = INGREDIENT_CODE.matcher(mainIngredient).replaceAll("");
        return Arrays.stream(INGREDIENT_SEPARATOR.split(withoutCodes))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}

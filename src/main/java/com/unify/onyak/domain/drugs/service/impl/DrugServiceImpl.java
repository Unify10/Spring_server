package com.unify.onyak.domain.drugs.service.impl;

import com.unify.onyak.domain.drugs.client.DrugApiClient;
import com.unify.onyak.domain.drugs.client.dto.DrugDetail;
import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.drugs.repository.DrugRepository;
import com.unify.onyak.domain.drugs.service.DrugService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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

    private Drug fetchAndSave(String itemSeq) {
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
            // 별도 트랜잭션으로 저장한다. 같은 약을 동시에 담아 item_seq UNIQUE 위반이 나도
            // 바깥(약장 저장) 트랜잭션이 rollback-only 로 깨지지 않게 하기 위해서다.
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

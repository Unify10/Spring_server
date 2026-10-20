package com.unify.onyak.domain.drugs.service.impl;

import com.unify.onyak.domain.drugs.client.DrugApiClient;
import com.unify.onyak.domain.drugs.client.dto.DrugDetail;
import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.repository.DrugRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DrugServiceImplTest {

    DrugRepository drugRepository;
    DrugApiClient drugApiClient;
    DrugServiceImpl drugService;

    @BeforeEach
    void setUp() {
        drugRepository = mock(DrugRepository.class);
        drugApiClient = mock(DrugApiClient.class);
        drugService = new DrugServiceImpl(drugRepository, drugApiClient, mock(PlatformTransactionManager.class));
    }

    @Test
    void DB에_있으면_FastAPI를_호출하지_않는다() {
        Drug cached = Drug.create("195700020", "타이레놀정500mg", List.of("아세트아미노펜"), null, null, null, null);
        when(drugRepository.findByItemSeq("195700020")).thenReturn(Optional.of(cached));

        assertThat(drugService.getOrFetch("195700020")).isSameAs(cached);
        verifyNoInteractions(drugApiClient);
    }

    @Test
    void 없으면_FastAPI에서_받아_저장한다() {
        when(drugRepository.findByItemSeq("195700020")).thenReturn(Optional.empty());
        when(drugApiClient.getDrug("195700020")).thenReturn(new DrugDetail(
                "195700020", "타이레놀정500mg", "한국얀센", "[M040702]아세트아미노펜|카페인무수물",
                "해열", "1회 1정", "과량 금지"));
        when(drugRepository.saveAndFlush(any(Drug.class))).thenAnswer(inv -> inv.getArgument(0));

        Drug drug = drugService.getOrFetch("195700020");

        ArgumentCaptor<Drug> saved = ArgumentCaptor.forClass(Drug.class);
        verify(drugRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue()).isSameAs(drug);
        assertThat(drug.getName()).isEqualTo("타이레놀정500mg");
        assertThat(drug.getManufacturer()).isEqualTo("한국얀센");
        assertThat(drug.getIngredients()).containsExactly("아세트아미노펜", "카페인무수물");
        assertThat(drug.getUsageInfo()).isEqualTo("1회 1정");
        assertThat(drug.getPrecautions()).isEqualTo("과량 금지");
    }

    @Test
    void 동시에_저장돼_UNIQUE_위반이면_다시_조회한다() {
        Drug other = Drug.create("195700020", "타이레놀정500mg", List.of(), null, null, null, null);
        when(drugRepository.findByItemSeq("195700020"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(other));
        when(drugApiClient.getDrug("195700020")).thenReturn(new DrugDetail(
                "195700020", "타이레놀정500mg", null, null, null, null, null));
        when(drugRepository.saveAndFlush(any(Drug.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThat(drugService.getOrFetch("195700020")).isSameAs(other);
    }

    @Test
    void 성분_문자열을_나눈다() {
        assertThat(DrugServiceImpl.splitIngredients("[M040702]아세트아미노펜|카페인무수물"))
                .containsExactly("아세트아미노펜", "카페인무수물");
        assertThat(DrugServiceImpl.splitIngredients("암로디핀, 로사르탄 + 암로디핀"))
                .containsExactly("암로디핀", "로사르탄");
        assertThat(DrugServiceImpl.splitIngredients(null)).isEmpty();
        assertThat(DrugServiceImpl.splitIngredients("  ")).isEmpty();
    }
}

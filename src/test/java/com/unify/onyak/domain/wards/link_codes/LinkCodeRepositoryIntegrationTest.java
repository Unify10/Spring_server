package com.unify.onyak.domain.wards.link_codes;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import com.unify.onyak.domain.wards.link_codes.repository.LinkCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LinkCodeRepositoryIntegrationTest {

    @Autowired
    private LinkCodeRepository linkCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WardRepository wardRepository;

    private Ward ward;

    @BeforeEach
    void setUp() {
        User guardian = userRepository.saveAndFlush(User.createLocal(
                "guardian-" + UUID.randomUUID() + "@test.com",
                "password-hash",
                "보호자",
                "010-0000-0000",
                LocalDate.of(1990, 1, 1),
                70.0f,
                175.0f,
                Gender.M,
                true,
                true,
                true
        ));

        ward = wardRepository.saveAndFlush(Ward.createLocal(
                guardian,
                "피보호자",
                LocalDate.of(2010, 1, 1),
                50.0f,
                160.0f,
                Gender.F,
                false,
                true,
                true
        ));
    }

    @Test
    void 같은_PENDING_연동코드는_두번_저장되지_않는다() {
        String code = "123456";
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);

        int firstInserted = linkCodeRepository.insertIfCodeAvailable(
                ward.getId(), code, UUID.randomUUID().toString(), expiresAt
        );
        int secondInserted = linkCodeRepository.insertIfCodeAvailable(
                ward.getId(), code, UUID.randomUUID().toString(), expiresAt
        );

        assertEquals(1, firstInserted);
        assertEquals(0, secondInserted);
    }

    @Test
    void USED_상태의_같은_연동코드는_다시_저장할_수_있다() {
        String code = "123456";
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);

        int firstInserted = linkCodeRepository.insertIfCodeAvailable(
                ward.getId(), code, UUID.randomUUID().toString(), expiresAt
        );
        linkCodeRepository.findByCodeAndStatusAndExpiresAtAfter(
                code, LinkCodeStatus.PENDING, OffsetDateTime.now()
        ).orElseThrow().markUsed(OffsetDateTime.now());
        linkCodeRepository.flush();

        int secondInserted = linkCodeRepository.insertIfCodeAvailable(
                ward.getId(), code, UUID.randomUUID().toString(), expiresAt
        );

        assertEquals(1, firstInserted);
        assertEquals(1, secondInserted);
    }
}

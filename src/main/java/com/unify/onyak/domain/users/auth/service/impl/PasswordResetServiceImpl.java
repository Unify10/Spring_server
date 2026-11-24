package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.request.PasswordResetRequest;
import com.unify.onyak.domain.users.auth.entity.PasswordReset;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.EmailCodeAttemptsExceededException;
import com.unify.onyak.domain.users.auth.error.exception.EmailCodeMismatchException;
import com.unify.onyak.domain.users.auth.error.exception.EmailNotVerifiedException;
import com.unify.onyak.domain.users.auth.error.exception.EmailSendFailedException;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.PasswordResetRepository;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.auth.service.PasswordResetService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String CODE_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    static final int MAX_FAILED_ATTEMPTS = 5;

    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender emailSender;
    private final UserValidationPolicy userValidationPolicy;

    @Override
    public void sendCode(String email) {
        Optional<User> user = userRepository.findByEmail(email);

        // 가입 여부를 응답으로 드러내지 않기 위해, 대상이 아니어도 예외 없이 끝낸다.
        // 구글로만 가입한 계정은 비밀번호가 없어 재설정 대상이 아니다.
        if (user.isEmpty() || user.get().getPasswordHash() == null) {
            log.info("비밀번호 재설정 대상 아님 email={}", email);
            return;
        }

        String code = generateRandomCode(CODE_LENGTH);

        sendMail(email, "[온약] 비밀번호 재설정 인증코드는 " + code + "입니다", buildResetEmailHtml(code));

        // 재발송하면 이전 코드와 실패 횟수는 덮어써진다
        passwordResetRepository.save(PasswordReset.issue(email, code));
    }

    @Override
    public void verifyCode(String email, String code) {
        PasswordReset stored = passwordResetRepository.findById(email)
                .orElseThrow(EmailCodeMismatchException::new);

        if (!stored.getCode().equals(code)) {
            PasswordReset failed = stored.withFailedAttempt();

            if (failed.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                passwordResetRepository.deleteById(email);
                throw new EmailCodeAttemptsExceededException();
            }

            passwordResetRepository.save(failed);
            throw new EmailCodeMismatchException();
        }

        passwordResetRepository.save(stored.markVerified());
    }

    @Transactional
    @Override
    public void resetPassword(PasswordResetRequest request) {
        userValidationPolicy.validatePasswordMatch(request.password(), request.passwordConfirm());

        boolean verified = passwordResetRepository.findById(request.email())
                .map(PasswordReset::isVerified)
                .orElse(false);

        if (!verified) {
            throw new EmailNotVerifiedException();
        }

        User user = userRepository.findByEmail(request.email())
                .filter(found -> found.getPasswordHash() != null)
                .orElseThrow(EmailNotVerifiedException::new);

        user.resetPassword(passwordEncoder.encode(request.password()));

        // 기존 기기의 로그인(refresh 토큰)은 끊고, 같은 인증으로 다시 재설정하지 못하게 인증 정보도 지운다
        refreshTokenRepository.deleteById(RefreshToken.keyOf(Role.GUARDIAN, user.getId()));
        passwordResetRepository.deleteById(request.email());
    }

    private void sendMail(String email, String title, String content) {
        MimeMessage message = emailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject(title);
            helper.setText(content, true);
            helper.setReplyTo("dowan1220@dgsw.hs.kr");
            emailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("비밀번호 재설정 메일 발송 실패 email={}", email, e);
            throw new EmailSendFailedException();
        }
    }

    private String buildResetEmailHtml(String code) {
        return """
                <div style="background-color:#f2f3f5; padding:40px 0; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <div style="max-width:480px; margin:0 auto; background-color:#ffffff; border-radius:8px; overflow:hidden;">
                    <div style="background-color:#f2f3f5; padding:32px 0; text-align:center;">
                      <span style="font-size:22px; font-weight:700; color:#111111;">온약</span>
                    </div>
                    <div style="padding:32px;">
                      <p style="font-size:15px; color:#333333; line-height:1.6; margin:0 0 24px;">
                        아래 인증코드를 비밀번호 재설정 화면에 입력해주세요.
                      </p>
                      <div style="background-color:#f2f3f5; border-radius:6px; padding:24px; text-align:center; margin-bottom:24px;">
                        <span style="font-size:32px; font-weight:700; letter-spacing:4px; color:#111111;">%s</span>
                      </div>
                      <p style="font-size:13px; color:#767676; line-height:1.6; margin:0;">
                        이 코드는 5분간 유효하며, 다른 사람과 절대 공유하지 마세요.<br/>
                        본인이 요청하지 않았다면 이 메일을 무시해도 됩니다. 비밀번호는 바뀌지 않습니다.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(code);
    }

    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARACTERS.charAt(random.nextInt(CODE_CHARACTERS.length())));
        }

        return sb.toString();
    }
}

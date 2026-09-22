package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.entity.EmailVerification;
import com.unify.onyak.domain.users.auth.error.exception.EmailCodeMismatchException;
import com.unify.onyak.domain.users.auth.error.exception.EmailSendFailedException;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.EmailVerificationRepository;
import com.unify.onyak.domain.users.auth.service.EmailVerificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final String CODE_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender emailSender;
    private final UserValidationPolicy userValidationPolicy;

    @Override
    public void sendCode(String email) {
        userValidationPolicy.validateDuplicateEmail(email);

        String code = generateRandomCode(CODE_LENGTH);

        sendMail(email, "[온약] 회원님의 인증코드는 " + code + "입니다", buildVerificationEmailHtml(code));

        emailVerificationRepository.save(new EmailVerification(email, code, false));
    }

    @Override
    public void verifyCode(String email, String code) {
        if (!emailVerificationRepository.existsByEmailAndCode(email, code)) {
            throw new EmailCodeMismatchException();
        }

        emailVerificationRepository.save(new EmailVerification(email, code, true));
    }

    private void sendMail(String email, String title, String content) {
        MimeMessage message = emailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject(title);
            helper.setText(content, true);// true를 설정해서 HTML을 사용 가능하게 함
            helper.setReplyTo("dowan1220@dgsw.hs.kr");
            emailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("이메일 인증코드 발송 실패 email={}", email, e);
            throw new EmailSendFailedException();
        }
    }

    private String buildVerificationEmailHtml(String code) {
        return """
                <div style="background-color:#f2f3f5; padding:40px 0; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <div style="max-width:480px; margin:0 auto; background-color:#ffffff; border-radius:8px; overflow:hidden;">
                    <div style="background-color:#f2f3f5; padding:32px 0; text-align:center;">
                      <span style="font-size:22px; font-weight:700; color:#111111;">온약</span>
                    </div>
                    <div style="padding:32px;">
                      <p style="font-size:15px; color:#333333; line-height:1.6; margin:0 0 24px;">
                        아래 인증코드를 회원가입 화면에 입력해주세요.
                      </p>
                      <div style="background-color:#f2f3f5; border-radius:6px; padding:24px; text-align:center; margin-bottom:24px;">
                        <span style="font-size:32px; font-weight:700; letter-spacing:4px; color:#111111;">%s</span>
                      </div>
                      <p style="font-size:13px; color:#767676; line-height:1.6; margin:0;">
                        이 코드는 5분간 유효하며, 다른 사람과 절대 공유하지 마세요.<br/>
                        본인이 요청하지 않았다면 이 메일을 무시해도 됩니다.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(code);
    }

    //
    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARACTERS.charAt(random.nextInt(CODE_CHARACTERS.length())));
        }

        return sb.toString();
    }
}

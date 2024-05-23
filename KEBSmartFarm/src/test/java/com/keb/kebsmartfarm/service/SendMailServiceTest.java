package com.keb.kebsmartfarm.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.keb.kebsmartfarm.constant.Message.Error;
import com.keb.kebsmartfarm.dto.MailDto;
import com.keb.kebsmartfarm.entity.VerificationCode;
import com.keb.kebsmartfarm.repository.UserRepository;
import com.keb.kebsmartfarm.repository.VerificationCodeRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SendMailServiceTest {
    @InjectMocks
    private SendMailService sendMailService;
    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    @Mock
    private UserRepository userRepository;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void 인증_코드_비정상_확인() {
        // given
        String userEmail = "test@test.com", code = "asdf", wrong = "bcdf";
        when(verificationCodeRepository.findById(userEmail)).thenReturn(
                Optional.of(new VerificationCode("test@test.com", wrong)), // 코드가 일치하지 않음
                Optional.empty() // 해당 이메일을 찾을 수 없음
        );

        //then
        assertAll(
                () -> assertThatThrownBy(() -> sendMailService.verifyEmail(code, userEmail))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining(Error.INVALID_CODE),
                () -> assertThatThrownBy(() -> sendMailService.verifyEmail(code, userEmail))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining(Error.INVALID_EMAIL)
        );

    }

    @Test
    void 인증_코드_정상_확인() {
        // given
        String userEmail = "test@test.com", code = "asdf";
        // when
        when(verificationCodeRepository.findById(userEmail)).thenReturn(
                Optional.of(new VerificationCode(userEmail, code)));
        // then
        assertDoesNotThrow(() -> sendMailService.verifyEmail(code, userEmail));
    }

    @Test
    void 메일_DTO_생성_확인() {
        UUID randomUUID = UUID.randomUUID(); // 최초 설정

        try (MockedStatic<UUID> uuid = mockStatic(UUID.class)) {
            // given
            String address = "test@test.com", code = randomUUID.toString().substring(0, 6);
            given(verificationCodeRepository.save(any(VerificationCode.class)))
                    .willReturn(new VerificationCode(address, code));
            given(UUID.randomUUID()).willReturn(randomUUID);

            // when
            MailDto verificationMail = sendMailService.createVerificationMail(address);

            // then
            assertThat(verificationMail.getAddress()).isEqualTo(address);
            assertThat(verificationMail.getMessage()).contains(code);
        }
    }


}
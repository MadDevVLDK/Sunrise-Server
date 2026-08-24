package com.sunrise.web.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sunrise.helpclass.exception.MyErrorCode;
import com.sunrise.helpclass.exception.MyException;
import com.sunrise.orchestrator.type.TokenType;

@RequiredArgsConstructor
@Service
public class EmailNotifier {

    @Value("${app.mail.mail-address}")
    private String mailAddress;
    
    @Value("${app.mail.base-url}")
    private String baseUrl;

    private final JavaMailSender mailSender;

    @Async // Пока что почта не гарантирована
    public void sendVerificationTokenMail(String to, TokenType tokenType, String token) {

        String subject;
        String body;
        String confirmUrl;

        switch (tokenType) {
            case REGISTRATION:
                subject = "Подтверждение регистрации на Sunrise Messenger";
                confirmUrl = baseUrl + "/forms/auth-confirmation/" + token + "/reg";
                body = String.format("""
                Здравствуйте!
                
                Для подтверждения регистрации перейдите по ссылке:
                %s
                
                Ссылка действительна 24 часа.
                """, confirmUrl);
                break;
            case EMAIL_UPDATE:
                subject = "Подтверждение смены email на Sunrise Messenger";
                confirmUrl = baseUrl + "/forms/auth-confirmation/" + token + "/email";
                body = String.format("""
                Здравствуйте!
                
                Для подтверждения смены email перейдите по ссылке и введите новый email:
                %s
                
                Ссылка действительна 24 часа.
                """, confirmUrl);
                break;
            case PASSWORD_UPDATE:
                subject = "Сброс пароля на Sunrise Messenger";
                confirmUrl = baseUrl + "/forms/auth-confirmation/" + token + "/password";
                body = String.format("""
                Здравствуйте!
                
                Для сброса пароля перейдите по ссылке и введите новый пароль:
                %s
                
                Ссылка действительна 1 час.
                """, confirmUrl);
                break;
            default:
                throw new MyException(MyErrorCode.VERIFICATION_TOKEN_NOT_FOUND);
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(mailAddress);
        email.setTo(to);
        email.setSubject(subject);
        email.setText(body);
        mailSender.send(email);
    }
}
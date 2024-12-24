package com.sovannara.spring_boot_auth.mail;

import java.util.HashMap;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void send(
            String to,
            String username,
            String templateName,
            String confirmationUrl
    ) throws MessagingException {
        try {
            if (!StringUtils.hasLength(templateName)) {
                templateName = "confirm-email";
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name()
            );

            Map<String, Object> emailProperties = new HashMap<>();
            emailProperties.put("username", username);
            emailProperties.put("confirmationUrl", confirmationUrl);

            Context context = new Context();
            context.setVariables(emailProperties);

            helper.setFrom("sovannara@gmail.com");
            helper.setTo(to);
            helper.setSubject("Welcome to our nice platform");

            String processTemplate = templateEngine.process(templateName, context);

            helper.setText(processTemplate, true);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new MessagingException("Error while sending email", e);
        }
    }

}

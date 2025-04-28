package com.is.auth.service;

import com.is.auth.model.email.EmailVerificationCode;
import com.is.auth.repository.EmailVerificationCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    private final EmailVerificationCodeRepository verificationCodeRepository;

    public EmailService(JavaMailSender mailSender,EmailVerificationCodeRepository verificationCodeRepository) {
        this.mailSender = mailSender;
        this.verificationCodeRepository = verificationCodeRepository;
    }

    public ResponseEntity<?> sendVerificationEmail(String email, String lang) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

            long requestCount = verificationCodeRepository.countRecentRequests(email, tenMinutesAgo);
            boolean isEmailVerifed = verificationCodeRepository.checkIsEmailVerified(email);
            Map<String, String> texts = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>("ru_title", "Вы превысили лимит попыток. Попробуйте снова через 10 минут."),
                    new AbstractMap.SimpleEntry<>("en_title", "Your limit exceeded. Try again 10 minutes later."),
                    new AbstractMap.SimpleEntry<>("uz_title", "Siz urinishlar limitidan oshdingiz. 10 daqiqadan so‘ng qayta urinib ko‘ring.")
            );

            Map<String, String> textEmailIsVerified = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>("ru_title", "Ваш email уже подтвержден, повторное подтверждение не требуется."),
                    new AbstractMap.SimpleEntry<>("en_title", "Your email is already verified, no further confirmation is needed."),
                    new AbstractMap.SimpleEntry<>("uz_title", "Sizning emailingiz allaqachon tasdiqlangan, qayta tasdiqlash talab qilinmaydi.")
            );

            Map<String, String> textSubject = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>("ru_title", "Place&Play - Код подтверждения email"),
                    new AbstractMap.SimpleEntry<>("en_title", "Place&Play - Email Verification Code"),
                    new AbstractMap.SimpleEntry<>("uz_title", "Place&Play - Email tasdiqlash kodi")
            );


            String countPrefix = texts.containsKey(lang + "_title") ? lang : "ru";
            String EmailsIsVerifiedPrefix = textEmailIsVerified.containsKey(lang + "_title") ? lang : "ru";
            String subjectPrefix = textSubject.containsKey(lang + "_title") ? lang : "ru";
            if (requestCount >= 3) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(texts.get(countPrefix + "_title"));
            }

            if(isEmailVerifed){
                return ResponseEntity.status(HttpStatus.OK)
                        .body(textEmailIsVerified.get(EmailsIsVerifiedPrefix + "_title"));
            }
            int code = generateCode();
            EmailVerificationCode verificationCode = new EmailVerificationCode(email, code, 10); // действует 10 минут
            verificationCodeRepository.save(verificationCode);
            helper.setFrom("verify@placeandplay.uz");
            helper.setTo(email);
            helper.setSubject(textSubject.get(subjectPrefix + "_title"));
            helper.setText(getContent(code,lang), true);
            mailSender.send(message);
            return ResponseEntity.ok().build();


        } catch (MessagingException e) {
            log.error("Ошибка при отправке письма на email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при отправке письма.");
        } catch (Exception e) {
            log.error("Неизвестная ошибка при отправке email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Произошла непредвиденная ошибка.");
        }


    }



    public ResponseEntity<?> verifyCode(String email, int code,String lang) {
        Optional<EmailVerificationCode> optionalCode = verificationCodeRepository.findByEmailAndCodeAndIsVerifiedFalse(email, code);

        if (optionalCode.isPresent()) {
            EmailVerificationCode verificationCode = optionalCode.get();

            if (verificationCode.getExpiresAt().isAfter(LocalDateTime.now())) {
                verificationCode.setVerified(true);
                verificationCodeRepository.save(verificationCode);
                verificationCodeRepository.updateUserIsEmailVerified(email); // если юзер подтвердил свой Email
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.status(401).build();
    }

        public int generateCode(){
            Random random = new SecureRandom();
            return 100000 + random.nextInt(900000);
        }
        public String getContent(int code, String lang) {
            Map<String, String> texts = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>("ru_title", "Добро пожаловать в Place&Play!"),
                    new AbstractMap.SimpleEntry<>("ru_instruction", "Для завершения регистрации введите следующий код в приложении:"),
                    new AbstractMap.SimpleEntry<>("ru_ignore", "Если вы не запрашивали регистрацию, просто проигнорируйте это письмо."),
                    new AbstractMap.SimpleEntry<>("ru_footer", "Это автоматическое сообщение, не отвечайте на него.<br>С уважением, команда Place&Play."),

                    new AbstractMap.SimpleEntry<>("en_title", "Welcome to Place&Play!"),
                    new AbstractMap.SimpleEntry<>("en_instruction", "To complete your registration, enter the following code in the app:"),
                    new AbstractMap.SimpleEntry<>("en_ignore", "If you did not request registration, please ignore this email."),
                    new AbstractMap.SimpleEntry<>("en_footer", "This is an automated message, do not reply.<br>Best regards, the Place&Play team."),

                    new AbstractMap.SimpleEntry<>("uz_title", "Place&Play'ga xush kelibsiz!"),
                    new AbstractMap.SimpleEntry<>("uz_instruction", "Ro‘yxatdan o‘tishni yakunlash uchun quyidagi kodni ilovaga kiriting:"),
                    new AbstractMap.SimpleEntry<>("uz_ignore", "Agar siz ro‘yxatdan o‘tishni so‘ramagan bo‘lsangiz, ushbu xatni e’tiborsiz qoldiring."),
                    new AbstractMap.SimpleEntry<>("uz_footer", "Bu avtomatik xabar, unga javob bermang.<br>Hurmat bilan, Place&Play jamoasi.")
            );

            // Проверяем, есть ли язык в списке, иначе выбираем русский
            String prefix = texts.containsKey(lang + "_title") ? lang : "ru";

            return String.format("""
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                .container { max-width: 500px; background: white; padding: 20px; border-radius: 10px; text-align: center; }
                h2 { color: #333; }
                p { font-size: 16px; color: #555; }
                .code { font-size: 24px; font-weight: bold; background: #eee; padding: 10px; border-radius: 5px; display: inline-block; margin: 10px 0; }
                .footer { font-size: 12px; color: #888; margin-top: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>%s</h2>
                <p>%s</p>
                <div class="code">%d</div>
                <p>%s</p>
                <div class="footer">%s</div>
            </div>
        </body>
        </html>
        """,
                    texts.get(prefix + "_title"),
                    texts.get(prefix + "_instruction"),
                    code,
                    texts.get(prefix + "_ignore"),
                    texts.get(prefix + "_footer")
            );
        }

    public ResponseEntity<?> sendWelcomeEmail(String email, String lang) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            Map<String, String> welcomeTexts = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>("ru_subject", "Place&Play - Добро пожаловать!"),
                    new AbstractMap.SimpleEntry<>("en_subject", "Place&Play - Welcome!"),
                    new AbstractMap.SimpleEntry<>("uz_subject", "Place&Play - Xush kelibsiz!"),

                    new AbstractMap.SimpleEntry<>("ru_body", """
                        Добро пожаловать в Place&Play! Мы очень рады, что вы присоединились к нам! 
                        Здесь вы сможете находить единомышленников, организовывать спортивные мероприятия и просто весело проводить время.
                        Откройте для себя новые возможности и наслаждайтесь игрой вместе с нами!
                        """),
                    new AbstractMap.SimpleEntry<>("en_body", """
                        Welcome to Place&Play! We're thrilled to have you with us! 
                        Here you can find like-minded people, organize sports events, and simply enjoy your time.
                        Discover new opportunities and enjoy playing with us!
                        """),
                    new AbstractMap.SimpleEntry<>("uz_body", """
                        Place&Play'ga xush kelibsiz! Biz sizning bizga qo'shilganingizdan juda xursandmiz!
                        Bu yerda siz hamfikrlar topishingiz, sport tadbirlarini tashkil qilishingiz va yaxshi vaqt o'tkazishingiz mumkin.
                        Yangi imkoniyatlarni kashf qiling va biz bilan birga zavqlaning!
                        """)
            );

            String langPrefix = welcomeTexts.containsKey(lang + "_subject") ? lang : "ru";

            helper.setFrom("welcome@placeandplay.uz");
            helper.setTo(email);
            helper.setSubject(welcomeTexts.get(langPrefix + "_subject"));
            helper.setText(getWelcomeContent(welcomeTexts.get(langPrefix + "_body")), true);

            mailSender.send(message);

            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            log.error("Ошибка при отправке приветственного письма на email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при отправке приветственного письма.");
        } catch (Exception e) {
            log.error("Неизвестная ошибка при отправке приветственного письма: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Произошла непредвиденная ошибка.");
        }
    }

    private String getWelcomeContent(String bodyText) {
        return String.format("""
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px; }
            .container { max-width: 600px; background: white; padding: 30px; border-radius: 10px; text-align: center; }
            h1 { color: #4CAF50; }
            p { font-size: 16px; color: #555; margin-top: 20px; }
            .logo { max-width: 100%%; margin-top: 20px; border-radius: 8px; }
            .footer { font-size: 12px; color: #888; margin-top: 30px; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>Добро пожаловать в Place&Play!</h1>
            <p>%s</p>
            <div class="footer">
                Это автоматическое сообщение. Пожалуйста, не отвечайте на него.<br>
                С уважением, команда Place&Play.
            </div>
        </div>
    </body>
    </html>
    """, bodyText);
    }



}
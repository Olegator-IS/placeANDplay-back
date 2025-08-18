package com.is.auth.service;

import com.is.auth.model.phoneNumber.PhoneNumberVerificationCode;
import com.is.auth.repository.PhoneNumberVerificationCodeRepository;
import com.is.auth.repository.UserRepository;
import com.is.events.model.Event;
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
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PhoneService {

    private final JavaMailSender mailSender;

    private final PhoneNumberVerificationCodeRepository phoneNumberVerificationCodeRepository;
    private final UserRepository userRepository;

    public PhoneService(JavaMailSender mailSender, PhoneNumberVerificationCodeRepository phoneNumberVerificationCodeRepository, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.phoneNumberVerificationCodeRepository = phoneNumberVerificationCodeRepository;
        this.userRepository = userRepository;
    }


    public ResponseEntity<?> sendVerificationPhoneNumber(String phoneNumber, String lang) {
        try {

            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
            boolean isPhoneExist = userRepository.existsByPhoneNumber(phoneNumber);
            long requestCount = phoneNumberVerificationCodeRepository.countRecentRequests(phoneNumber, tenMinutesAgo);
            boolean isEmailVerifed = phoneNumberVerificationCodeRepository.checkIsPhoneNumberVerified(phoneNumber);
            Map<String, String> texts = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>("ru_title", "Вы превысили лимит попыток. Попробуйте снова через 10 минут."),
                    new AbstractMap.SimpleEntry<>("en_title", "Your limit exceeded. Try again 10 minutes later."),
                    new AbstractMap.SimpleEntry<>("uz_title", "Siz urinishlar limitidan oshdingiz. 10 daqiqadan so'ng qayta urinib ko'ring.")
            );

            Map<String, String> isNotExist = Map.ofEntries(
                    new AbstractMap.SimpleEntry<>("ru_title", "Мы не нашли этот номер в Place&Play. Пожалуйста, введите номер, который использовался при регистрации."),
                    new AbstractMap.SimpleEntry<>("en_title", "We couldn’t find this number in Place&Play. Please enter the number you used during registration."),
                    new AbstractMap.SimpleEntry<>("uz_title", "Ushbu raqam Place&Play tizimida topilmadi. Iltimos, roʻyxatdan oʻtishda ishlatgan raqamingizni kiriting.")
            );


            String isNotExistPrefix = isNotExist.containsKey(lang + "_title") ? lang : "ru";


            if(!isPhoneExist){
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(isNotExist.get(isNotExistPrefix + "_title"));
            }

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
            PhoneNumberVerificationCode verificationCode = new PhoneNumberVerificationCode(phoneNumber, code, 10); // действует 10 минут
            phoneNumberVerificationCodeRepository.save(verificationCode);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(verificationCode);
        } catch (Exception e) {
            log.error("Неизвестная ошибка при отправке email: {}", phoneNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Произошла непредвиденная ошибка.");
        }


    }


    public ResponseEntity<?> sendMessageToMyMail(String email, String messageFromMvp) {
        try {
            // Создаем структурированное сообщение для бизнеса
            String structuredMessage = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%); color: white; padding: 20px; border-radius: 8px; text-align: center; margin-bottom: 30px; }
                        .user-info { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
                        .message-content { background: #f5f5f5; padding: 20px; border-left: 4px solid #4CAF50; border-radius: 4px; }
                        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                        .timestamp { color: #888; font-size: 12px; margin-top: 10px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>📧 Новое сообщение от пользователя</h2>
                        </div>
                        <div class="user-info">
                            <h3>👤 Информация об отправителе:</h3>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Время отправки:</strong> %s</p>
                        </div>
                        <div class="message-content">
                            <h3>💬 Сообщение:</h3>
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>Это автоматическое уведомление от Place&Play.</p>
                            <p>Для ответа используйте кнопку "Ответить" в вашем почтовом клиенте.</p>
                        </div>
                        
                        <div class="timestamp">
                            Сообщение получено: %s
                        </div>
                    </div>
                </body>
                </html>
                """, 
                email, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                messageFromMvp.replace("\n", "<br>"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
            );

            // Отправляем письмо на бизнес-почту
            MimeMessage businessMessage = mailSender.createMimeMessage();
            MimeMessageHelper businessHelper = new MimeMessageHelper(businessMessage, true, "UTF-8");
            businessHelper.setFrom("info@placeandplay.uz");
            businessHelper.setTo("business@placeandplay.uz");
            businessHelper.setReplyTo(email); // Для ответа на email пользователя
            businessHelper.setSubject("📧 Новое сообщение от пользователя Place&Play");
            businessHelper.setText(structuredMessage, true);
            mailSender.send(businessMessage);

            // Отправляем подтверждение пользователю
            String userConfirmation = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%); color: white; padding: 20px; border-radius: 8px; text-align: center; margin-bottom: 30px; }
                        .message { background: #f0f9f4; padding: 20px; border-radius: 8px; border-left: 4px solid #4CAF50; }
                        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 14px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>✅ Сообщение отправлено</h2>
                        </div>
                        <div class="message">
                            <h3>Спасибо за ваше сообщение!</h3>
                            <p>Мы получили ваше сообщение и ответим вам в ближайшее время.</p>
                            <p><strong>Время отправки:</strong> %s</p>
                        </div>
                        <div class="footer">
                            <p>Это автоматическое подтверждение от Place&Play.</p>
                            <p>С уважением, команда Place&Play</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
            );

            MimeMessage userMessage = mailSender.createMimeMessage();
            MimeMessageHelper userHelper = new MimeMessageHelper(userMessage, true, "UTF-8");
            userHelper.setFrom("business@placeandplay.uz");
            userHelper.setTo(email);
            userHelper.setSubject("✅ Ваше сообщение отправлено - Place&Play");
            userHelper.setText(userConfirmation, true);
            mailSender.send(userMessage);

            log.info("Message from user {} sent successfully to business email", email);
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



    public ResponseEntity<?> verifyCode(String phoneNumber, int code,String lang) {
        Optional<PhoneNumberVerificationCode> optionalCode = phoneNumberVerificationCodeRepository.findByPhoneNumberAndCodeAndIsVerifiedFalse(phoneNumber, code);

        if (optionalCode.isPresent()) {
            PhoneNumberVerificationCode verificationCode = optionalCode.get();

            if (verificationCode.getExpiresAt().isAfter(LocalDateTime.now())) {
                verificationCode.setVerified(true);
                phoneNumberVerificationCodeRepository.save(verificationCode);
                phoneNumberVerificationCodeRepository.updateUserIsPhoneVerified(phoneNumber); // если юзер подтвердил свой номер телефона
                return ResponseEntity.ok().build();
            }
        }

        Map<String, String> conflict = Map.ofEntries(
                new AbstractMap.SimpleEntry<>("ru_title", "Текущий код не является действительным, попробуйте еще раз!"),
                new AbstractMap.SimpleEntry<>("en_title", "The current code is not valid, please try again!"),
                new AbstractMap.SimpleEntry<>("uz_title", "Kiritilgan kod yaroqsiz. Iltimos, qayta urinib ko'ring!")
        );

        String conflictPrefix = conflict.containsKey(lang + "_title") ? lang : "ru";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(conflict.get(conflictPrefix + "_title"));
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
                    new AbstractMap.SimpleEntry<>("uz_instruction", "Ro'yxatdan o'tishni yakunlash uchun quyidagi kodni ilovaga kiriting:"),
                    new AbstractMap.SimpleEntry<>("uz_ignore", "Agar siz ro'yxatdan o'tishni so'ramagan bo'lsangiz, ushbu xatni e'tiborsiz qoldiring."),
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

    public ResponseEntity<?> sendEventCreated(Event event, String lang, String address, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Map<String, String> texts = Map.ofEntries(
                // Subjects with emojis
                new AbstractMap.SimpleEntry<>("ru_subject", "🎉 Place&Play - Новое событие создано!"),
                new AbstractMap.SimpleEntry<>("en_subject", "🎉 Place&Play - New Event Created!"),
                new AbstractMap.SimpleEntry<>("uz_subject", "🎉 Place&Play - Yangi tadbir yaratildi!"),

                // Greetings with emojis
                new AbstractMap.SimpleEntry<>("ru_greeting", "👋 Здравствуйте, %s!"),
                new AbstractMap.SimpleEntry<>("en_greeting", "👋 Hello, %s!"),
                new AbstractMap.SimpleEntry<>("uz_greeting", "👋 Salom, %s!"),

                // Success messages with emojis
                new AbstractMap.SimpleEntry<>("ru_success", """
                    ✨ Отлично! Вы успешно создали новое событие на платформе Place&Play.
                    Мы уже отправили уведомление организаторам места проведения."""),
                new AbstractMap.SimpleEntry<>("en_success", """
                    ✨ Great! You have successfully created a new event on Place&Play platform.
                    We have already notified the venue organizers."""),
                new AbstractMap.SimpleEntry<>("uz_success", """
                    ✨ Ajoyib! Siz Place&Play platformasida yangi tadbirni muvaffaqiyatli yaratdingiz.
                    Biz allaqachon joy tashkilotchilariga xabar yubordik."""),

                // Event details
                new AbstractMap.SimpleEntry<>("ru_details", "Детали события:"),
                new AbstractMap.SimpleEntry<>("en_details", "Event details:"),
                new AbstractMap.SimpleEntry<>("uz_details", "Tadbir tafsilotlari:"),

                // Event fields
                new AbstractMap.SimpleEntry<>("ru_name", "Название:"),
                new AbstractMap.SimpleEntry<>("en_name", "Name:"),
                new AbstractMap.SimpleEntry<>("uz_name", "Nomi:"),

                new AbstractMap.SimpleEntry<>("ru_datetime", "Дата и время:"),
                new AbstractMap.SimpleEntry<>("en_datetime", "Date and time:"),
                new AbstractMap.SimpleEntry<>("uz_datetime", "Sana va vaqt:"),

                new AbstractMap.SimpleEntry<>("ru_place", "Место проведения:"),
                new AbstractMap.SimpleEntry<>("en_place", "Location:"),
                new AbstractMap.SimpleEntry<>("uz_place", "O'tkaziladigan joy:"),

                new AbstractMap.SimpleEntry<>("ru_address", "Адрес:"),
                new AbstractMap.SimpleEntry<>("en_address", "Address:"),
                new AbstractMap.SimpleEntry<>("uz_address", "Manzil:"),

                new AbstractMap.SimpleEntry<>("ru_status", "Статус:"),
                new AbstractMap.SimpleEntry<>("en_status", "Status:"),
                new AbstractMap.SimpleEntry<>("uz_status", "Holati:"),

                // Updated notification messages with emojis
                new AbstractMap.SimpleEntry<>("ru_notify", """
                    ⏳ Мы уведомим вас, как только организация рассмотрит вашу заявку.
                    Обычно это занимает не более 24 часов."""),
                new AbstractMap.SimpleEntry<>("en_notify", """
                    ⏳ We will notify you as soon as the organization reviews your request.
                    This usually takes no more than 24 hours."""),
                new AbstractMap.SimpleEntry<>("uz_notify", """
                    ⏳ Tashkilot arizangizni ko'rib chiqqanida sizga xabar beramiz.
                    Bu odatda 24 soatdan oshmaydi."""),

                // Updated button text with emoji
                new AbstractMap.SimpleEntry<>("ru_button", "🔍 Посмотреть событие"),
                new AbstractMap.SimpleEntry<>("en_button", "🔍 View Event"),
                new AbstractMap.SimpleEntry<>("uz_button", "🔍 Tadbirni ko'rish"),

                // Updated footer with emoji
                new AbstractMap.SimpleEntry<>("ru_footer", "📧 Это автоматическое уведомление от Place&Play."),
                new AbstractMap.SimpleEntry<>("en_footer", "📧 This is an automated notification from Place&Play."),
                new AbstractMap.SimpleEntry<>("uz_footer", "📧 Bu Place&Play'dan avtomatik bildirishnoma.")
            );

            // Format date and time according to locale
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                switch (lang) {
                    case "en" -> "MMM dd, yyyy 'at' HH:mm";
                    case "uz" -> "dd.MM.yyyy HH:mm";
                    default -> "dd.MM.yyyy HH:mm";
                }
            );

            String formattedDateTime = event.getDateTime().format(formatter);

            String prefix = texts.containsKey(lang + "_subject") ? lang : "ru";

            // Prepare email content
            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        @import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap');
                        
                        body {
                            font-family: 'Roboto', Arial, sans-serif;
                            line-height: 1.6;
                            color: #2c3e50;
                            margin: 0;
                            padding: 0;
                            background-color: #f5f7fa;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background-color: #ffffff;
                            border-radius: 16px;
                            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%);
                            color: white;
                            padding: 40px 20px;
                            text-align: center;
                            position: relative;
                        }
                        .header::after {
                            content: '';
                            position: absolute;
                            bottom: 0;
                            left: 0;
                            right: 0;
                            height: 6px;
                            background: linear-gradient(90deg, rgba(255,255,255,0.2) 0%%, rgba(255,255,255,0.4) 50%%, rgba(255,255,255,0.2) 100%%);
                        }
                        .logo {
                            width: 140px;
                            height: auto;
                            margin-bottom: 20px;
                            filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
                        }
                        .header h2 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 700;
                            text-shadow: 0 2px 4px rgba(0,0,0,0.2);
                            letter-spacing: 0.5px;
                        }
                        .content {
                            padding: 40px 30px;
                            background-color: #ffffff;
                        }
                        .greeting {
                            font-size: 24px;
                            color: #2c3e50;
                            margin-bottom: 25px;
                            font-weight: 500;
                        }
                        .success-message {
                            color: #27ae60;
                            font-size: 17px;
                            margin-bottom: 30px;
                            padding: 20px;
                            background-color: #f0f9f4;
                            border-radius: 12px;
                            border-left: 5px solid #27ae60;
                            box-shadow: 0 2px 8px rgba(39, 174, 96, 0.1);
                        }
                        .event-details {
                            background-color: #f8fafc;
                            padding: 30px;
                            border-radius: 12px;
                            margin: 30px 0;
                            border: 1px solid #e2e8f0;
                            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
                        }
                        .event-details h3 {
                            color: #2c3e50;
                            margin: 0 0 25px 0;
                            font-size: 20px;
                            font-weight: 600;
                            padding-bottom: 12px;
                            border-bottom: 2px solid #e2e8f0;
                        }
                        .event-details p {
                            margin: 15px 0;
                            color: #4a5568;
                            display: flex;
                            align-items: center;
                            font-size: 16px;
                        }
                        .event-details strong {
                            color: #2c3e50;
                            min-width: 180px;
                            font-weight: 500;
                        }
                        .event-details span {
                            color: #4a5568;
                            flex: 1;
                        }
                        .status-badge {
                            display: inline-block;
                            padding: 8px 16px;
                            background-color: #fff3cd;
                            color: #856404;
                            border-radius: 20px;
                            font-size: 15px;
                            font-weight: 500;
                            box-shadow: 0 2px 4px rgba(133, 77, 4, 0.1);
                        }
                        .notification {
                            background-color: #e3f2fd;
                            padding: 20px 25px;
                            border-radius: 12px;
                            margin: 30px 0;
                            color: #1976d2;
                            font-size: 16px;
                            border-left: 5px solid #1976d2;
                            box-shadow: 0 2px 8px rgba(25, 118, 210, 0.1);
                        }
                        .button-container {
                            text-align: center;
                            margin: 35px 0;
                        }
                        .button {
                            display: inline-block;
                            padding: 16px 32px;
                            background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%);
                            color: white;
                            text-decoration: none;
                            border-radius: 30px;
                            font-weight: 500;
                            font-size: 17px;
                            transition: all 0.3s ease;
                            box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
                            letter-spacing: 0.5px;
                        }
                        .button:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 6px 16px rgba(76, 175, 80, 0.3);
                            background: linear-gradient(135deg, #45a049 0%%, #3d8b40 100%%);
                        }
                        .footer {
                            text-align: center;
                            padding: 30px;
                            background-color: #f8fafc;
                            color: #64748b;
                            font-size: 14px;
                            border-top: 1px solid #e2e8f0;
                        }
                        .footer p {
                            margin: 8px 0;
                        }
                        .social-links {
                            margin: 20px 0;
                        }
                        .social-links a {
                            display: inline-block;
                            margin: 0 12px;
                            color: #4CAF50;
                            text-decoration: none;
                            font-weight: 500;
                            transition: color 0.3s ease;
                        }
                        .social-links a:hover {
                            color: #45a049;
                        }
                        @media only screen and (max-width: 600px) {
                            .container {
                                margin: 10px;
                                border-radius: 12px;
                            }
                            .content {
                                padding: 25px;
                            }
                            .event-details {
                                padding: 20px;
                            }
                            .event-details p {
                                flex-direction: column;
                                align-items: flex-start;
                            }
                            .event-details strong {
                                min-width: auto;
                                margin-bottom: 8px;
                            }
                            .button {
                                display: block;
                                text-align: center;
                                margin: 0 auto;
                                padding: 14px 28px;
                            }
                            .header {
                                padding: 30px 15px;
                            }
                            .header h2 {
                                font-size: 24px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <img src="https://placeandplay.uz/images/logo-white.png" alt="Place&Play Logo" class="logo">
                            <h2>%s</h2>
                        </div>
                        <div class="content">
                            <div class="greeting">%s</div>
                            <div class="success-message">%s</div>
                            
                            <div class="event-details">
                                <h3>%s</h3>
                                <p>
                                    <strong>%s</strong>
                                    <span>%s</span>
                                </p>
                                <p>
                                    <strong>%s</strong>
                                    <span>%s</span>
                                </p>
                                <p>
                                    <strong>%s</strong>
                                    <span>%s</span>
                                </p>
                            </div>

                            <div class="notification">%s</div>
                            
                            <div class="button-container">
                                <a href="https://placeandplay.uz/events/%s" class="button">%s</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p>%s</p>
                            <div class="social-links">
                                <a href="https://t.me/placeandplay" target="_blank">Telegram</a>
                                <a href="https://instagram.com/placeandplay.uz" target="_blank">Instagram</a>
                                <a href="https://facebook.com/placeandplay" target="_blank">Facebook</a>
                            </div>
                            <p>%s</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                texts.get(prefix + "_subject"),
                String.format(texts.get(prefix + "_greeting"), event.getOrganizerEvent().getOrganizerName()),
                texts.get(prefix + "_success"),
                texts.get(prefix + "_details"),
                texts.get(prefix + "_name"), event.getSportEvent().getSportName(),
                texts.get(prefix + "_datetime"), formattedDateTime,
                texts.get(prefix + "_place"), name,
                texts.get(prefix + "_address"), address,
                texts.get(prefix + "_status"), texts.get(prefix + "_notify"),
                event.getEventId().toString(),
                texts.get(prefix + "_button"),
                texts.get(prefix + "_footer"),
                texts.get(prefix + "_copyright")
            );

            helper.setFrom("events@placeandplay.uz");
            helper.setTo(event.getOrganizerEvent().getEmail());
            helper.setSubject(texts.get(prefix + "_subject"));
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Event creation notification sent successfully to: {}", event.getOrganizerEvent().getEmail());

            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            log.error("Failed to send event creation notification to: {}. Error: {}", event.getOrganizerEvent().getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Failed to send event creation notification",
                        "details", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Unexpected error while sending event creation notification to: {}. Error: {}",
                    event.getOrganizerEvent().getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Unexpected error occurred",
                        "details", e.getMessage()
                    ));
        }
    }

    public ResponseEntity<?> sendEventStatusChangeNotification(Event event, String lang, String placeName, String placePhone) {
        try {
            // Add null check for placePhone
            String safePlacePhone = placePhone != null ? placePhone : "+998 (XX) XXX-XX-XX";
            String safePlaceName = placeName != null ? placeName : "Unknown Place";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Map<String, String> texts = Map.ofEntries(
                // Subjects with emojis
                new AbstractMap.SimpleEntry<>("ru_subject_rejected", "❌ Place&Play - Событие отклонено"),
                new AbstractMap.SimpleEntry<>("en_subject_rejected", "❌ Place&Play - Event Rejected"),
                new AbstractMap.SimpleEntry<>("uz_subject_rejected", "❌ Place&Play - Tadbir rad etildi"),

                new AbstractMap.SimpleEntry<>("ru_subject_confirmed", "✅ Place&Play - Событие подтверждено"),
                new AbstractMap.SimpleEntry<>("en_subject_confirmed", "✅ Place&Play - Event Confirmed"),
                new AbstractMap.SimpleEntry<>("uz_subject_confirmed", "✅ Place&Play - Tadbir tasdiqlandi"),

                new AbstractMap.SimpleEntry<>("ru_subject_changes", "📝 Place&Play - Требуются изменения"),
                new AbstractMap.SimpleEntry<>("en_subject_changes", "📝 Place&Play - Changes Required"),
                new AbstractMap.SimpleEntry<>("uz_subject_changes", "📝 Place&Play - O'zgarishlar talab qilinadi"),

                new AbstractMap.SimpleEntry<>("ru_subject_expired", "⏰ Place&Play - Событие просрочено"),
                new AbstractMap.SimpleEntry<>("en_subject_expired", "⏰ Place&Play - Event Expired"),
                new AbstractMap.SimpleEntry<>("uz_subject_expired", "⏰ Place&Play - Tadbir muddati tugadi"),

                new AbstractMap.SimpleEntry<>("ru_subject_in_progress", "🎮 Place&Play - Событие началось"),
                new AbstractMap.SimpleEntry<>("en_subject_in_progress", "🎮 Place&Play - Event Started"),
                new AbstractMap.SimpleEntry<>("uz_subject_in_progress", "🎮 Place&Play - Tadbir boshlandi"),

                new AbstractMap.SimpleEntry<>("ru_subject_completed", "🏆 Place&Play - Событие завершено"),
                new AbstractMap.SimpleEntry<>("en_subject_completed", "🏆 Place&Play - Event Completed"),
                new AbstractMap.SimpleEntry<>("uz_subject_completed", "🏆 Place&Play - Tadbir yakunlandi"),

                // Updated status messages with emojis
                new AbstractMap.SimpleEntry<>("ru_rejected", """
                    😔 К сожалению, организация "%s" отклонила вашу заявку на создание события.
                    
                    📞 Вы можете позвонить по номеру %s, чтобы узнать детали отказа.
                    🔄 Или попробовать создать событие на другую дату.
                    
                    💪 Мы всегда готовы помочь вам организовать успешное мероприятие!"""),
                new AbstractMap.SimpleEntry<>("en_rejected", """
                    😔 Unfortunately, the organization "%s" has rejected your event creation request.
                    
                    📞 You can call %s to learn more about the rejection details.
                    🔄 Or try to create an event for a different date.
                    
                    💪 We are always ready to help you organize a successful event!"""),
                new AbstractMap.SimpleEntry<>("uz_rejected", """
                    😔 Afsuski, "%s" tashkiloti sizning tadbir yaratish so'rovingizni rad etdi.
                    
                    📞 Rad etilish sabablari haqida batafsil ma'lumot olish uchun %s raqamiga qo'ng'iroq qilishingiz mumkin.
                    🔄 Yoki boshqa sana uchun tadbir yaratishni sinab ko'rishingiz mumkin.
                    
                    💪 Biz sizga muvaffaqiyatli tadbir tashkil qilishda yordam berishga har doim tayyormiz!"""),

                new AbstractMap.SimpleEntry<>("ru_confirmed", """
                    🎉 Отличные новости! Организация "%s" подтвердила регистрацию вашего события!
                    
                    📅 Событие состоится в запланированное время (%s).
                    
                    ⏰ Важная информация:
                    • Просим прибыть заранее, если это возможно
                    • Событие начнется автоматически в указанное время (±5 минут)
                    
                    ⚠️ Если вы считаете, что это сообщение ошибочно и вы не создавали данное событие,
                    пожалуйста, свяжитесь с нами по номеру %s."""),
                new AbstractMap.SimpleEntry<>("en_confirmed", """
                    🎉 Great news! The organization "%s" has confirmed your event registration!
                    
                    📅 The event will take place at the scheduled time (%s).
                    
                    ⏰ Important information:
                    • Please arrive early if possible
                    • The event will start automatically at the specified time (±5 minutes)
                    
                    ⚠️ If you believe this message is in error and you did not create this event,
                    please contact us at %s."""),
                new AbstractMap.SimpleEntry<>("uz_confirmed", """
                    🎉 Ajoyib yangilik! "%s" tashkiloti sizning tadbiringizni tasdiqladi!
                    
                    📅 Tadbir rejalashtirilgan vaqtda (%s) bo'lib o'tadi.
                    
                    ⏰ Muhim ma'lumot:
                    • Iltimos, imkoniyat bo'lsa, oldindan kelib turing
                    • Tadbir ko'rsatilgan vaqtda avtomatik ravishda (±5 daqiqa) boshlanadi
                    
                    ⚠️ Agar siz bu xabarni xato deb hisoblasangiz va bu tadbirni yaratmagan bo'lsangiz,
                    iltimos, %s raqamiga qo'ng'iroq qiling."""),

                new AbstractMap.SimpleEntry<>("ru_changes", """
                    📝 Организация "%s" запросила изменения в данных вашего события.
                    
                    📞 Пожалуйста, свяжитесь с нами по номеру %s для уточнения деталей.
                    
                    🤝 Мы поможем вам внести необходимые корректировки и сделаем всё возможное,
                    чтобы ваше событие прошло успешно!"""),
                new AbstractMap.SimpleEntry<>("en_changes", """
                    📝 The organization "%s" has requested changes to your event details.
                    
                    📞 Please contact us at %s for more information.
                    
                    🤝 We will help you make the necessary adjustments and do everything possible
                    to make your event successful!"""),
                new AbstractMap.SimpleEntry<>("uz_changes", """
                    📝 "%s" tashkiloti tadbiringiz ma'lumotlarida o'zgarishlar so'radi.
                    
                    📞 Tafsilotlarni aniqlash uchun %s raqamiga qo'ng'iroq qiling.
                    
                    🤝 Biz sizga kerakli o'zgarishlarni kiritishda yordam beramiz va tadbiringiz
                    muvaffaqiyatli o'tishi uchun hamma imkoniyatlardan foydalanamiz!"""),

                new AbstractMap.SimpleEntry<>("ru_expired", """
                    ⏰ К сожалению, ваше событие было автоматически отменено, так как организация
                    не отреагировала на запрос в течение установленного времени.
                    
                    ℹ️ Это может произойти, если:
                    • Организация временно недоступна
                    • Организация перегружена запросами
                    
                    💡 Рекомендации:
                    1. Попробовать создать событие на другую дату
                    2. Связаться с организацией напрямую по номеру %s
                    3. Выбрать другое место для проведения события
                    
                    💪 Мы всегда готовы помочь вам организовать успешное мероприятие!"""),
                new AbstractMap.SimpleEntry<>("en_expired", """
                    ⏰ Unfortunately, your event has been automatically cancelled as the organization
                    did not respond to the request within the specified time.
                    
                    ℹ️ This can happen if:
                    • The organization is temporarily unavailable
                    • The organization is overwhelmed with requests
                    
                    💡 Recommendations:
                    1. Try creating an event for a different date
                    2. Contact the organization directly at %s
                    3. Choose a different venue for your event
                    
                    💪 We are always ready to help you organize a successful event!"""),
                new AbstractMap.SimpleEntry<>("uz_expired", """
                    ⏰ Afsuski, tashkilot belgilangan vaqt ichida so'rovga javob bermagani uchun
                    sizning tadbiringiz avtomatik ravishda bekor qilindi.
                    
                    ℹ️ Bu quyidagi sabablarga ko'ra yuz berishi mumkin:
                    • Tashkilot vaqtincha mavjud bo'lmasa
                    • Tashkilot so'rovlar bilan band bo'lsa
                    
                    💡 Tavsiyalar:
                    1. Boshqa sana uchun tadbir yaratishni sinab ko'ring
                    2. %s raqami orqali tashkilot bilan to'g'ridan-to'g'ri bog'laning
                    3. Tadbiringiz uchun boshqa joyni tanlang
                    
                    💪 Biz sizga muvaffaqiyatli tadbir tashkil qilishda yordam berishga har doim tayyormiz!"""),

                new AbstractMap.SimpleEntry<>("ru_in_progress", """
                    🎮 Уважаемый(ая) %s!
                    
                    🎯 Ваше событие началось в указанном месте.
                    
                    ✨ Желаем вам и вашим участникам:
                    • Удачи в игре
                    • Отличного времяпрепровождения
                    • Приятного общения
                    
                    🎉 Наслаждайтесь игрой!"""),
                new AbstractMap.SimpleEntry<>("en_in_progress", """
                    🎮 Dear %s!
                    
                    🎯 Your event has started at the specified location.
                    
                    ✨ We wish you and your participants:
                    • Good luck in the game
                    • A great time
                    • Pleasant communication
                    
                    🎉 Enjoy the game!"""),
                new AbstractMap.SimpleEntry<>("uz_in_progress", """
                    🎮 Hurmatli %s!
                    
                    🎯 Sizning tadbiringiz ko'rsatilgan joyda boshlandi.
                    
                    ✨ Sizga va ishtirokchilaringizga:
                    • O'yinda omad
                    • Yaxshi vaqt o'tkazish
                    • Yoqimli muloqot
                    
                    🎉 O'yindan zavqlaning!"""),

                new AbstractMap.SimpleEntry<>("ru_completed", """
                    🏆 Поздравляем!
                    
                    ✨ Ваше событие успешно завершено. Это значит, что всё прошло отлично
                    и всем всё понравилось!
                    
                    🙏 Благодарим вас за организацию мероприятия и надеемся,
                    что все участники остались довольны.
                    
                    💫 Желаем вам и вашим участникам:
                    • Дальнейших успехов
                    • Новых интересных встреч
                    • Приятных воспоминаний
                    
                    🎮 До новых встреч на Place&Play!"""),
                new AbstractMap.SimpleEntry<>("en_completed", """
                    🏆 Congratulations!
                    
                    ✨ Your event has been successfully completed. This means everything went great
                    and everyone enjoyed it!
                    
                    🙏 Thank you for organizing the event and we hope
                    all participants were satisfied.
                    
                    💫 We wish you and your participants:
                    • Continued success
                    • New interesting meetings
                    • Pleasant memories
                    
                    🎮 See you next time at Place&Play!"""),
                new AbstractMap.SimpleEntry<>("uz_completed", """
                    🏆 Tabriklaymiz!
                    
                    ✨ Sizning tadbiringiz muvaffaqiyatli yakunlandi. Bu hammasi ajoyib o'tgani
                    va hamma mamnun bo'lgani degani!
                    
                    🙏 Tadbirni tashkil qilganingiz uchun tashakkur va barcha ishtirokchilar
                    mamnun bo'lganiga umid qilamiz.
                    
                    💫 Sizga va ishtirokchilaringizga:
                    • Keyingi muvaffaqiyatlarni
                    • Yangi qiziqarli uchrashuvlarni
                    • Yoqimli xotiralarni
                    
                    🎮 Place&Play'da keyingi uchrashuvgacha!"""),

                // Common elements
                new AbstractMap.SimpleEntry<>("ru_event_details", "Детали события:"),
                new AbstractMap.SimpleEntry<>("en_event_details", "Event details:"),
                new AbstractMap.SimpleEntry<>("uz_event_details", "Tadbir tafsilotlari:"),

                new AbstractMap.SimpleEntry<>("ru_name", "Название:"),
                new AbstractMap.SimpleEntry<>("en_name", "Name:"),
                new AbstractMap.SimpleEntry<>("uz_name", "Nomi:"),

                new AbstractMap.SimpleEntry<>("ru_datetime", "Дата и время:"),
                new AbstractMap.SimpleEntry<>("en_datetime", "Date and time:"),
                new AbstractMap.SimpleEntry<>("uz_datetime", "Sana va vaqt:"),

                new AbstractMap.SimpleEntry<>("ru_place", "Место проведения:"),
                new AbstractMap.SimpleEntry<>("en_place", "Location:"),
                new AbstractMap.SimpleEntry<>("uz_place", "O'tkaziladigan joy:"),

                new AbstractMap.SimpleEntry<>("ru_button", "Перейти к событию"),
                new AbstractMap.SimpleEntry<>("en_button", "Go to event"),
                new AbstractMap.SimpleEntry<>("uz_button", "Tadbirga o'tish"),

                new AbstractMap.SimpleEntry<>("ru_footer", "Это автоматическое уведомление от Place&Play."),
                new AbstractMap.SimpleEntry<>("en_footer", "This is an automated notification from Place&Play."),
                new AbstractMap.SimpleEntry<>("uz_footer", "Bu Place&Play'dan avtomatik bildirishnoma."),

                new AbstractMap.SimpleEntry<>("ru_copyright", "© 2025 Place&Play. Все права защищены."),
                new AbstractMap.SimpleEntry<>("en_copyright", "© 2025 Place&Play. All rights reserved."),
                new AbstractMap.SimpleEntry<>("uz_copyright", "© 2025 Place&Play. Barcha huquqlar himoyalangan.")
            );

            String prefix = texts.containsKey(lang + "_subject_" + event.getStatus().name().toLowerCase()) ? lang : "ru";
            String subjectKey = "subject_" + event.getStatus().name().toLowerCase();
            String messageKey = event.getStatus().name().toLowerCase();

            log.info("Processing event status change notification - Status: {}, Language: {}, Prefix: {}, MessageKey: {}", 
                event.getStatus(), lang, prefix, messageKey);

            // Format date and time according to locale
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                switch (lang) {
                    case "en" -> "MMM dd, yyyy 'at' HH:mm";
                    case "uz" -> "dd.MM.yyyy HH:mm";
                    default -> "dd.MM.yyyy HH:mm";
                }
            );

            String formattedDateTime = event.getDateTime().format(formatter);
            String templateKey = prefix + "_" + messageKey;
            String template = texts.get(templateKey);
            
            if (template == null) {
                log.error("Missing template for status change notification - TemplateKey: {}, Available keys: {}", 
                    templateKey, texts.keySet());
                throw new IllegalStateException("Missing template for status: " + event.getStatus() + " and language: " + lang);
            }

            // Add null check for organizer name
            String organizerName = event.getOrganizerEvent() != null && event.getOrganizerEvent().getOrganizerName() != null 
                ? event.getOrganizerEvent().getOrganizerName() 
                : "User";

            String statusMessage = switch (event.getStatus()) {
                case REJECTED -> String.format(template, safePlaceName, safePlacePhone);
                case CONFIRMED -> String.format(template, safePlaceName, formattedDateTime, safePlacePhone);
                case CHANGES_REQUESTED -> String.format(template, safePlaceName, safePlacePhone);
                case EXPIRED -> String.format(template, safePlacePhone);
                case IN_PROGRESS -> String.format(template, organizerName);
                case COMPLETED -> template;
                default -> "";
            };

            // Prepare email content
            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        @import url('https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap');
                        
                        body {
                            font-family: 'Roboto', Arial, sans-serif;
                            line-height: 1.6;
                            color: #2c3e50;
                            margin: 0;
                            padding: 0;
                            background-color: #f5f7fa;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background-color: #ffffff;
                            border-radius: 16px;
                            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%);
                            color: white;
                            padding: 40px 20px;
                            text-align: center;
                            position: relative;
                        }
                        .header::after {
                            content: '';
                            position: absolute;
                            bottom: 0;
                            left: 0;
                            right: 0;
                            height: 6px;
                            background: linear-gradient(90deg, rgba(255,255,255,0.2) 0%%, rgba(255,255,255,0.4) 50%%, rgba(255,255,255,0.2) 100%%);
                        }
                        .logo {
                            width: 140px;
                            height: auto;
                            margin-bottom: 20px;
                            filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
                        }
                        .header h2 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 700;
                            text-shadow: 0 2px 4px rgba(0,0,0,0.2);
                            letter-spacing: 0.5px;
                        }
                        .content {
                            padding: 40px 30px;
                            background-color: #ffffff;
                        }
                        .greeting {
                            font-size: 24px;
                            color: #2c3e50;
                            margin-bottom: 25px;
                            font-weight: 500;
                        }
                        .success-message {
                            color: #27ae60;
                            font-size: 17px;
                            margin-bottom: 30px;
                            padding: 20px;
                            background-color: #f0f9f4;
                            border-radius: 12px;
                            border-left: 5px solid #27ae60;
                            box-shadow: 0 2px 8px rgba(39, 174, 96, 0.1);
                        }
                        .event-details {
                            background-color: #f8fafc;
                            padding: 30px;
                            border-radius: 12px;
                            margin: 30px 0;
                            border: 1px solid #e2e8f0;
                            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
                        }
                        .event-details h3 {
                            color: #2c3e50;
                            margin: 0 0 25px 0;
                            font-size: 20px;
                            font-weight: 600;
                            padding-bottom: 12px;
                            border-bottom: 2px solid #e2e8f0;
                        }
                        .event-details p {
                            margin: 15px 0;
                            color: #4a5568;
                            display: flex;
                            align-items: center;
                            font-size: 16px;
                        }
                        .event-details strong {
                            color: #2c3e50;
                            min-width: 180px;
                            font-weight: 500;
                        }
                        .event-details span {
                            color: #4a5568;
                            flex: 1;
                        }
                        .status-badge {
                            display: inline-block;
                            padding: 8px 16px;
                            background-color: #fff3cd;
                            color: #856404;
                            border-radius: 20px;
                            font-size: 15px;
                            font-weight: 500;
                            box-shadow: 0 2px 4px rgba(133, 77, 4, 0.1);
                        }
                        .notification {
                            background-color: #e3f2fd;
                            padding: 20px 25px;
                            border-radius: 12px;
                            margin: 30px 0;
                            color: #1976d2;
                            font-size: 16px;
                            border-left: 5px solid #1976d2;
                            box-shadow: 0 2px 8px rgba(25, 118, 210, 0.1);
                        }
                        .button-container {
                            text-align: center;
                            margin: 35px 0;
                        }
                        .button {
                            display: inline-block;
                            padding: 16px 32px;
                            background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%);
                            color: white;
                            text-decoration: none;
                            border-radius: 30px;
                            font-weight: 500;
                            font-size: 17px;
                            transition: all 0.3s ease;
                            box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
                            letter-spacing: 0.5px;
                        }
                        .button:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 6px 16px rgba(76, 175, 80, 0.3);
                            background: linear-gradient(135deg, #45a049 0%%, #3d8b40 100%%);
                        }
                        .footer {
                            text-align: center;
                            padding: 30px;
                            background-color: #f8fafc;
                            color: #64748b;
                            font-size: 14px;
                            border-top: 1px solid #e2e8f0;
                        }
                        .footer p {
                            margin: 8px 0;
                        }
                        .social-links {
                            margin: 20px 0;
                        }
                        .social-links a {
                            display: inline-block;
                            margin: 0 12px;
                            color: #4CAF50;
                            text-decoration: none;
                            font-weight: 500;
                            transition: color 0.3s ease;
                        }
                        .social-links a:hover {
                            color: #45a049;
                        }
                        @media only screen and (max-width: 600px) {
                            .container {
                                margin: 10px;
                                border-radius: 12px;
                            }
                            .content {
                                padding: 25px;
                            }
                            .event-details {
                                padding: 20px;
                            }
                            .event-details p {
                                flex-direction: column;
                                align-items: flex-start;
                            }
                            .event-details strong {
                                min-width: auto;
                                margin-bottom: 8px;
                            }
                            .button {
                                display: block;
                                text-align: center;
                                margin: 0 auto;
                                padding: 14px 28px;
                            }
                            .header {
                                padding: 30px 15px;
                            }
                            .header h2 {
                                font-size: 24px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <img src="https://placeandplay.uz/images/logo-white.png" alt="Place&Play Logo" class="logo">
                            <h2>%s</h2>
                        </div>
                        <div class="content">
                            <div class="greeting">%s</div>
                            <div class="status-message">%s</div>
                            
                            <div class="event-details">
                                <h3>%s</h3>
                                <p>
                                    <strong>%s</strong>
                                    <span>%s</span>
                                </p>
                                <p>
                                    <strong>%s</strong>
                                    <span>%s</span>
                                </p>
                                <p>
                                    <strong>%s</strong>
                                    <span>%s</span>
                                </p>
                            </div>
                            
                            <div class="button-container">
                                <a href="https://placeandplay.uz/events/%s" class="button">%s</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p>%s</p>
                            <div class="social-links">
                                <a href="https://t.me/placeandplay" target="_blank">Telegram</a>
                                <a href="https://instagram.com/placeandplay.uz" target="_blank">Instagram</a>
                                <a href="https://facebook.com/placeandplay" target="_blank">Facebook</a>
                            </div>
                            <p>%s</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                // Status-specific colors
                switch (event.getStatus()) {
                    case REJECTED -> "#fef2f2";
                    case CONFIRMED -> "#f0fdf4";
                    case CHANGES_REQUESTED -> "#fefce8";
                    case EXPIRED -> "#f8fafc";
                    case IN_PROGRESS -> "#eff6ff";
                    case COMPLETED -> "#f0fdf4";
                    default -> "#f8fafc";
                },
                switch (event.getStatus()) {
                    case REJECTED -> "#991b1b";
                    case CONFIRMED -> "#166534";
                    case CHANGES_REQUESTED -> "#854d0e";
                    case EXPIRED -> "#475569";
                    case IN_PROGRESS -> "#1e40af";
                    case COMPLETED -> "#166534";
                    default -> "#475569";
                },
                switch (event.getStatus()) {
                    case REJECTED -> "#dc2626";
                    case CONFIRMED -> "#22c55e";
                    case CHANGES_REQUESTED -> "#eab308";
                    case EXPIRED -> "#64748b";
                    case IN_PROGRESS -> "#3b82f6";
                    case COMPLETED -> "#22c55e";
                    default -> "#64748b";
                },
                texts.get(prefix + "_" + subjectKey),
//                String.format(texts.get(prefix + "_greeting"), organizerName),
//                    String.format(texts.get(prefix + "_greeting"), event.getOrganizerEvent().getOrganizerName()),
                statusMessage,
                texts.get(prefix + "_event_details"),
                texts.get(prefix + "_name"), event.getSportEvent().getSportName(),
                texts.get(prefix + "_datetime"), formattedDateTime,
                texts.get(prefix + "_place"), safePlaceName,
                event.getEventId().toString(),
                texts.get(prefix + "_button"),
                texts.get(prefix + "_footer"),
                texts.get(prefix + "_copyright")
            );

            helper.setFrom("events@placeandplay.uz");
            helper.setTo(event.getOrganizerEvent().getEmail());
            helper.setSubject(texts.get(prefix + "_" + subjectKey));
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Event status change notification sent successfully to: {}", event.getOrganizerEvent().getEmail());

            return ResponseEntity.ok().build();
        } catch (MessagingException e) {
            log.error("Failed to send event status change notification to: {}. Error: {}", 
                event.getOrganizerEvent().getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Failed to send event status change notification",
                        "details", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Unexpected error while sending event status change notification to: {}. Error: {}", 
                event.getOrganizerEvent().getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Unexpected error occurred",
                        "details", e.getMessage()
                    ));
        }
    }

}
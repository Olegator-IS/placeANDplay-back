package com.is.org.service;

import com.is.org.model.OrganizationTelegramChat;
import com.is.org.model.OrganizationTelegramChatRequest;
import com.is.org.repository.OrganizationTelegramChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationTelegramChatService {

    private final OrganizationTelegramChatRepository repository;

    @Transactional
    public OrganizationTelegramChat saveTelegramChat(OrganizationTelegramChatRequest request) {
        log.info("Saving telegram chat for organization: {}", request.getOrgId());
        
        // Проверяем, существует ли уже активная запись с таким chatId для этой организации
        List<OrganizationTelegramChat> existingChats = repository.findByOrgIdAndActive(request.getOrgId());
        boolean chatExists = existingChats.stream()
                .anyMatch(chat -> chat.getChatId().equals(request.getChatId()));
        
        if (chatExists) {
            log.info("Chat ID {} already exists for organization: {}", request.getChatId(), request.getOrgId());
            throw new RuntimeException("Chat ID already exists for this organization");
        }

        // Создаем новую запись
        OrganizationTelegramChat newChat = new OrganizationTelegramChat();
        newChat.setOrgId(request.getOrgId());
        newChat.setChatId(request.getChatId());
        newChat.setChatName(request.getChatName());
        newChat.setBotToken(request.getBotToken());
        newChat.setIsActive(true);

        OrganizationTelegramChat saved = repository.save(newChat);
        log.info("Saved new telegram chat for organization: {}", request.getOrgId());
        
        return saved;
    }

    public List<OrganizationTelegramChat> findByOrgId(Long orgId) {
        return repository.findByOrgIdAndActive(orgId);
    }

    public Optional<OrganizationTelegramChat> findByChatId(String chatId) {
        return repository.findByChatIdAndActive(chatId);
    }

    public List<OrganizationTelegramChat> findAllActive() {
        return repository.findAllActive();
    }

    @Transactional
    public boolean deactivateByOrgId(Long orgId) {
        List<OrganizationTelegramChat> chats = repository.findByOrgIdAndActive(orgId);
        if (!chats.isEmpty()) {
            for (OrganizationTelegramChat chat : chats) {
                chat.setIsActive(false);
                repository.save(chat);
            }
            log.info("Deactivated {} telegram chats for organization: {}", chats.size(), orgId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deactivateByChatId(String chatId) {
        Optional<OrganizationTelegramChat> chat = repository.findByChatIdAndActive(chatId);
        if (chat.isPresent()) {
            OrganizationTelegramChat telegramChat = chat.get();
            telegramChat.setIsActive(false);
            repository.save(telegramChat);
            log.info("Deactivated telegram chat with chatId: {}", chatId);
            return true;
        }
        return false;
    }

    public boolean existsByOrgId(Long orgId) {
        return repository.existsByOrgIdAndIsActiveTrue(orgId);
    }

    public boolean existsByChatId(String chatId) {
        return repository.existsByChatIdAndIsActiveTrue(chatId);
    }

    @Transactional
    public boolean deactivateChatByOrgIdAndChatId(Long orgId, String chatId) {
        List<OrganizationTelegramChat> chats = repository.findByOrgIdAndActive(orgId);
        for (OrganizationTelegramChat chat : chats) {
            if (chat.getChatId().equals(chatId)) {
                chat.setIsActive(false);
                repository.save(chat);
                log.info("Deactivated telegram chat {} for organization: {}", chatId, orgId);
                return true;
            }
        }
        return false;
    }
}

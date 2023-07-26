package com.lorettabank.cardservice.service;

import com.lorettabank.cardservice.dto.CardDTO;
import com.lorettabank.cardservice.mapper.CardMapper;
import com.lorettabank.cardservice.model.Card;
import com.lorettabank.cardservice.repository.CardRepository;
import com.lorettabank.common.model.Account;
import com.lorettabank.common.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final UserService userService; // Assuming a UserService exists for fetching User details
    private final AccountService accountService; // Assuming an AccountService exists for fetching Account details

    public CardService(CardRepository cardRepository, CardMapper cardMapper, UserService userService, AccountService accountService) {
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
        this.userService = userService;
        this.accountService = accountService;
    }

    public CardDTO createCard(CardDTO cardDTO) {
        User user = userService.findById(cardDTO.getUserId());
        Account account = accountService.findById(cardDTO.getAccountId());
        Card card = cardMapper.toEntity(cardDTO);
        card.setUser(user);
        card.setAccount(account);
        cardRepository.save(card);
        return cardMapper.toDTO(card);
    }

    public CardDTO getCard(int id) {
        Card card = cardRepository.findById(id);
        return cardMapper.toDTO(card);
    }

    public List<CardDTO> getAllCards() {
        return cardRepository.findAll().stream().map(cardMapper::toDTO).collect(Collectors.toList());
    }

    public CardDTO updateCard(CardDTO cardDTO) {
        Card card = cardMapper.toEntity(cardDTO);
        User user = userService.findById(cardDTO.getUserId());
        Account account = accountService.findById(cardDTO.getAccountId());
        card.setUser(user);
        card.setAccount(account);
        cardRepository.update(card);
        return cardMapper.toDTO(card);
    }

    public void partialUpdateCard(int id, String field, Object value) {
        cardRepository.partialUpdate(id, field, value);
    }

    public void deleteCard(int id) {
        cardRepository.deleteById(id);
    }
}

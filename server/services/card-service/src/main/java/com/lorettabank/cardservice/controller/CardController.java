package com.lorettabank.cardservice.controller;

import com.lorettabank.cardservice.dto.CardDTO;
import com.lorettabank.cardservice.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<CardDTO> createCard(@RequestBody CardDTO cardDTO) {
        CardDTO createdCard = cardService.createCard(cardDTO);
        return ResponseEntity.ok(createdCard);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardDTO> getCard(@PathVariable int id) {
        CardDTO cardDTO = cardService.getCard(id);
        return ResponseEntity.ok(cardDTO);
    }

    @GetMapping
    public ResponseEntity<List<CardDTO>> getAllCards() {
        List<CardDTO> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardDTO> updateCard(@PathVariable int id, @RequestBody CardDTO cardDTO) {
        cardDTO.setId(id);
        CardDTO updatedCard = cardService.updateCard(cardDTO);
        return ResponseEntity.ok(updatedCard);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> partialUpdateCard(@PathVariable int id, @RequestBody Map<String, Object> updates) {
        updates.forEach((field, value) -> cardService.partialUpdateCard(id, field, value));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable int id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}

package com.lorettabank.cardservice.repository;

import com.lorettabank.cardservice.mapper.CardMapper;
import com.lorettabank.cardservice.model.Card;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CardRepository {
    private final JdbcTemplate jdbcTemplate;
    private final CardMapper cardMapper;

    public CardRepository(JdbcTemplate jdbcTemplate, CardMapper cardMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.cardMapper = cardMapper;
    }

    public int save(Card card) {
        return jdbcTemplate.update(
                "INSERT INTO cards (user_id, account_id, card_number, expiry_date, cvv, credit_limit, balance) VALUES (?, ?, ?, ?, ?, ?, ?)",
                card.getUser().getId(), card.getAccount().getId(), card.getCardNumber(), card.getExpiryDate(), card.getCvv(), card.getCreditLimit(), card.getBalance()
        );
    }

    public Card findById(int id) {
        return jdbcTemplate.queryForObject("SELECT * FROM cards WHERE id = ?", cardMapper, id);
    }

    public List<Card> findAll() {
        return jdbcTemplate.query("SELECT * FROM cards", cardMapper);
    }

    public int update(Card card) {
        return jdbcTemplate.update(
                "UPDATE cards SET user_id = ?, account_id = ?, card_number = ?, expiry_date = ?, cvv = ?, credit_limit = ?, balance = ? WHERE id = ?",
                card.getUser().getId(), card.getAccount().getId(), card.getCardNumber(), card.getExpiryDate(), card.getCvv(), card.getCreditLimit(), card.getBalance(), card.getId()
        );
    }

    public int partialUpdate(int id, String field, Object value) {
        return jdbcTemplate.update("UPDATE cards SET " + field + " = ? WHERE id = ?", value, id);
    }

    public int deleteById(int id) {
        return jdbcTemplate.update("DELETE FROM cards WHERE id = ?", id);
    }
}

package com.lorettabank.userservice.mapper;

import com.lorettabank.common.Card;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CardMapper implements RowMapper<Card> {
    @Override
    public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
        Card card = new Card();
        card.setId(rs.getInt("id"));
        card.setCardNumber(rs.getString("card_number"));
        card.setCvv(rs.getString("cvv"));
        return card;
    }
}

package com.lorettabank.shared.event;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MoneyWithdrawnEvent extends DomainEvent {

    private Long accountId;
    private String accountNumber;
    private BigDecimal amount;
    private String currency;
    private BigDecimal newBalance;
    private String reference;
}

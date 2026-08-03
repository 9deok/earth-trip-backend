package com.earthtrip.expense.application.port.in;import java.math.BigDecimal;import java.time.Instant;import java.util.*;
public interface FinanceLedgerUseCase{
 List<CashResult> cash(UUID trip,UUID actor);CashResult createCash(UUID trip,UUID actor,CashCommand c);CashResult updateCash(UUID trip,UUID id,UUID actor,CashCommand c);void deleteCash(UUID trip,UUID id,UUID actor,long baseVersion);List<CashBalance> balances(UUID trip,UUID actor);CashResult reconcile(UUID trip,UUID actor,UUID requestId,String currency,long countedBalance,Map<String,Object>payload);
 List<RateResult> rates(UUID trip,UUID actor);RateResult createRate(UUID trip,UUID actor,RateCommand c);
 record CashCommand(UUID requestId,String movementType,long amountMinor,String currency,Map<String,Object>payload,String status,Instant occurredAt,long baseVersion){}record CashResult(UUID id,UUID tripId,String movementType,long amountMinor,String currency,Map<String,Object>payload,String status,Instant occurredAt,long version,UUID createdBy,Instant createdAt,Instant updatedAt){}record CashBalance(String currency,long estimatedBalance,Instant calculatedAt){}
 record RateCommand(UUID requestId,String baseCurrency,String quoteCurrency,BigDecimal rate,String source,Instant observedAt){}record RateResult(UUID id,String baseCurrency,String quoteCurrency,BigDecimal rate,String source,Instant observedAt,Instant createdAt){}
}

package org.example.strategy;

import org.example.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SplitStrategy {
    // userId -> what that user owes. The payer's own entry is 0, the payer already paid it.
    Map<String, BigDecimal> calculateSplit(List<User> users, BigDecimal amount, String payerId);
}

package org.example.strategy;

import org.example.entity.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualSplit implements SplitStrategy {
    @Override
    public Map<String, BigDecimal> calculateSplit(List<User> users, BigDecimal amount, String payerId) {
        BigDecimal share = amount.divide(BigDecimal.valueOf(users.size()), 2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> split = new HashMap<>();
        for (User user : users) {
            if (user.getId().equals(payerId)) {
                split.put(user.getId(), BigDecimal.ZERO.setScale(2));
            } else {
                split.put(user.getId(), share);
            }
        }
        return split;
    }
}

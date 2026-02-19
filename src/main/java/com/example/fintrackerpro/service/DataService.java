package com.example.fintrackerpro.service;

import com.example.fintrackerpro.repository.ExpenseRepository;
import com.example.fintrackerpro.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public void deleteMonthData(Long userId, int year, int month, String type) {
        switch (type.toLowerCase()) {
            case "income" -> {
                incomeRepository.deleteByUserIdAndYearAndMonth(userId, year, month);
                log.info("🗑 Deleted income userId={} {}/{}", userId, year, month);
            }
            case "expenses" -> {
                expenseRepository.deleteByUserIdAndYearAndMonth(userId, year, month);
                log.info("🗑 Deleted expenses userId={} {}/{}", userId, year, month);
            }
            case "all" -> {
                incomeRepository.deleteByUserIdAndYearAndMonth(userId, year, month);
                expenseRepository.deleteByUserIdAndYearAndMonth(userId, year, month);
                log.info("🗑 Deleted all userId={} {}/{}", userId, year, month);
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}

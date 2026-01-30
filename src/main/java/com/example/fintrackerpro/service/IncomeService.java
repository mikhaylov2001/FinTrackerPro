package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.income.Income;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserService userService;

    public Income addIncome(IncomeRequest request) {
        User user = userService.getUserById(request.getUserId());

        Income income = Income.builder()
                .user(user)
                .amount(request.getAmount())
                .category(request.getCategory())
                .source(request.getSource())
                .date(request.getDate().atTime(LocalTime.MIDNIGHT))
                .build();

        Income saved = incomeRepository.save(income);
        log.info("✅ Income created: id={}, userId={}, amount={}, category={}",
                saved.getId(), user.getId(), saved.getAmount(), saved.getCategory());
        return saved;
    }

    public Income getIncomeById(Long incomeId) {
        return incomeRepository.findById(incomeId)
                .orElseThrow(() -> {
                    log.error("❌ Income not found with id={}", incomeId);
                    return new ResourceNotFoundException("Income not found with id: " + incomeId);
                });
    }

    public Page<Income> getIncomesByUser(Long userId, Pageable pageable) {
        userService.getUserById(userId);
        return incomeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<Income> getIncomesByUserAndMonth(Long userId, int year, int month, Pageable pageable) {
        log.info("Getting incomes for user {} {}/{}", userId, year, month);
        userService.getUserById(userId);
        return incomeRepository.findByUserIdAndYearAndMonth(userId, year, month, pageable);
    }

    public Income updateIncome(Long incomeId, IncomeRequest request) {
        Income income = getIncomeById(incomeId);

        if (request.getAmount() != null) {
            income.setAmount(request.getAmount());
        }
        if (request.getCategory() != null) {
            income.setCategory(request.getCategory());
        }
        if (request.getSource() != null) {
            income.setSource(request.getSource());
        }
        if (request.getDate() != null) {
            income.setDate(request.getDate().atTime(LocalTime.MIDNIGHT));
        }

        Income updated = incomeRepository.save(income);
        log.info("✅ Income updated: id={}, amount={}", incomeId, updated.getAmount());
        return updated;
    }

    public void deleteIncome(Long incomeId) {
        Income income = getIncomeById(incomeId);
        incomeRepository.delete(income);
        log.info("✅ Income deleted: id={}", incomeId);
    }
}

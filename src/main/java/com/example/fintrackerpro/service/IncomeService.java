package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.IncomeResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserService userService;

    public IncomeResponse addIncome(Long userId, IncomeRequest request) {
        User user = userService.getUserEntityById(userId);

        Income income = Income.builder()
                .user(user)
                .amount(request.getAmount())
                .category(request.getCategory())
                .source(request.getSource())
                .date(request.getDate().atTime(LocalTime.MIDNIGHT))
                .build();

        Income saved = incomeRepository.save(income);
        log.info("✅ Income created: id={}, userId={}, amount={}, category={}",
                saved.getId(), userId, saved.getAmount(), saved.getCategory());
        return IncomeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(Long userId, Long incomeId) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + incomeId));
        return IncomeResponse.from(income);
    }

    @Transactional(readOnly = true)
    public Page<IncomeResponse> getIncomesByUser(Long userId, Pageable pageable) {
        userService.getUserEntityById(userId);
        return incomeRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(IncomeResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<IncomeResponse> getIncomesByUserAndMonth(Long userId, int year, int month, Pageable pageable) {
        log.info("Getting incomes for user {} {}/{}", userId, year, month);
        userService.getUserEntityById(userId);
        return incomeRepository.findByUserIdAndYearAndMonth(userId, year, month, pageable).map(IncomeResponse::from);
    }

    public IncomeResponse updateIncome(Long userId, Long incomeId, IncomeRequest request) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + incomeId));

        if (request.getAmount() != null) income.setAmount(request.getAmount());
        if (request.getCategory() != null) income.setCategory(request.getCategory());
        if (request.getSource() != null) income.setSource(request.getSource());
        if (request.getDate() != null) income.setDate(request.getDate().atTime(LocalTime.MIDNIGHT));

        Income updated = incomeRepository.save(income);
        log.info("✅ Income updated: id={}, userId={}, amount={}", incomeId, userId, updated.getAmount());
        return IncomeResponse.from(updated);
    }

    public void deleteIncome(Long userId, Long incomeId) {
        Income income = incomeRepository.findByIdAndUserId(incomeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + incomeId));
        incomeRepository.delete(income);
        log.info("✅ Income deleted: id={}, userId={}", incomeId, userId);
    }
}

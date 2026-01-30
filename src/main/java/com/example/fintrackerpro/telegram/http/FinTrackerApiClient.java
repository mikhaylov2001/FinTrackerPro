package com.example.fintrackerpro.telegram.http;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinTrackerApiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl = "http://localhost:8082";

    public FinTrackerApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserDto getUserByChatId(Long chatId) {
        String url = baseUrl + "/api/users/by-chat/" + chatId;
        return restTemplate.getForObject(url, UserDto.class);
    }

    public UserDto registerUser(Long chatId, String userName) {
        String url = baseUrl + "/api/users/register";
        Map<String, Object> body = new HashMap<>();
        body.put("chatId", chatId);
        body.put("userName", userName);
        return restTemplate.postForObject(url, body, UserDto.class);
    }

    public IncomeDto addIncome(IncomeCreateRequest req) {
        String url = baseUrl + "/api/income";
        return restTemplate.postForObject(url, req, IncomeDto.class);
    }

    public record ApiPage(List<?> content, Long totalElements, int totalPages, int currentPage) {
    }



    public record ExpenseDto(
            Long id,
            BigDecimal amount,
            String category,
            LocalDateTime createdAt,
            LocalDate date,
            String description
    ) {
    }


    public ExpenseDto addExpense(ExpenseCreateRequest request) {
        String url = baseUrl + "/api/expenses";
        return restTemplate.postForObject(url, request, ExpenseDto.class);
    }


    public record IncomeDto(
            Long id,
            Long userId,
            String category,
            String source,
            String description,
            BigDecimal amount,
            String date
    ) {

    }

    public record ExpenseCreateRequest(
            Long userId,
            BigDecimal amount,
            String category,
            String description
    ) {
    }

    public record IncomeCreateRequest(
            Long userId,
            BigDecimal amount,
            String category,
            String source,
            LocalDate date,
            String description
    ) {

    }

    // DTO под ответ /api/users/*
    public record UserDto(
            Long id,
            Long chatId,
            String userName
    ) {
    }

    // DTO под ответ /api/summary/monthly
    public record MonthlySummaryDto(
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal savings,
            BigDecimal savingsRatePercent
    ) {
    }

    public MonthlySummaryDto getMonthlySummary(Long userId, int year, int month) {
        String url = baseUrl + "/api/summary/monthly"
                + "?userId=" + userId
                + "&year=" + year
                + "&month=" + month;

        return restTemplate.getForObject(url, MonthlySummaryDto.class);
    }

    public ApiPage getIncomesByUserAndMonth(Long userId, int year, int month, int page) {
        String url = String.format("%s/api/income/month/%d/%d/%d?page=%d&size=1000", baseUrl, userId,
                year, month, page);
        return restTemplate.getForObject(url, ApiPage.class);
    }

    public ApiPage getExpensesByUserAndMonth(Long userId, int year, int month) {
        String url = String.format("%s/api/expenses/month/%d/%d/%d?page=0&size=10000",
                baseUrl, userId, year, month);
        return restTemplate.getForObject(url, ApiPage.class);
    }
    public Map<String, Object> getIncomeById(Long id) {
        String url = baseUrl + "/api/income/" + id;
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> getExpenseById(Long id) {
        String url = baseUrl + "/api/expense/" + id;
        return restTemplate.getForObject(url, Map.class);
    }

    public void updateIncome(Long id, Map<String, Object> updateMap) {
        String url = baseUrl + "/api/income/" + id;

        IncomeUpdateRequest request = new IncomeUpdateRequest(
                ((Number) updateMap.get("userId")).longValue(),
                (BigDecimal) updateMap.get("amount"),
                (String) updateMap.get("category"),
                (String) updateMap.get("source"),
                LocalDate.parse((String) updateMap.get("date")),
                (String) updateMap.get("description")
        );

        restTemplate.put(url, request);
    }

    public void updateExpense(Long id, Map<String, Object> updateMap) {
        String url = baseUrl + "/api/expense/" + id;

        ExpenseUpdateRequest request = new ExpenseUpdateRequest(
                ((Number) updateMap.get("userId")).longValue(),
                (BigDecimal) updateMap.get("amount"),
                (String) updateMap.get("category"),
                LocalDate.parse((String) updateMap.get("date")),
                (String) updateMap.get("description")
        );

        restTemplate.put(url, request);
    }

    public void deleteIncome(Long incomeId) {
        String url = String.format("%s/api/income/%d", baseUrl, incomeId);
        restTemplate.delete(url);
    }

    public void deleteExpense(Long expenseId) {
        String url = String.format("%s/api/expense/%d", baseUrl, expenseId);
        restTemplate.delete(url);
    }
    public record IncomeUpdateRequest(
            Long userId,
            BigDecimal amount,
            String category,
            String source,
            LocalDate date,
            String description
    ) {}

    public record ExpenseUpdateRequest(
            Long userId,
            BigDecimal amount,
            String category,
            LocalDate date,
            String description
    ) {}

}

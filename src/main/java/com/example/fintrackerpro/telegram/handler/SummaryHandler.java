package com.example.fintrackerpro.telegram.handler;

import com.example.fintrackerpro.telegram.http.FinTrackerApiClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
@Component
public class SummaryHandler {
    private final FinTrackerApiClient apiClient;

    public SummaryHandler(FinTrackerApiClient apiClient) {
        this.apiClient = apiClient;
    }
    public String handleSummary(Long chatId, String text){
        List<String> parts = tokenize(text);

        YearMonth yearMonth;

        if(parts.size()>=2){
            yearMonth = YearMonth.parse(parts.get(1));
        }else {
            yearMonth = YearMonth.from(LocalDate.now());
        }

        try {
            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);
            FinTrackerApiClient.MonthlySummaryDto summary =
                    apiClient.getMonthlySummary(user.id(), yearMonth.getYear(), yearMonth.getMonthValue());

            return String.format(
                    "Сводка за %s:\n" +
                            "Доходы: %s ₽\n" +
                            "Расходы: %s ₽\n" +
                            "Сбережения: %s ₽\n" +
                            "Норма сбережений: %s %%\n",
                    yearMonth.toString(),
                    summary.totalIncome(),
                    summary.totalExpenses(),
                    summary.savings(),
                    summary.savingsRatePercent()
            );
        }catch (Exception e){
            e.printStackTrace();
            return "Не удалось посчитать,проверьте еще раз!\n" +
                    "Формат: /summary или /summary <год-месяц YYYY-MM>\n" +
                    "Пример: /summary 2026-01";
        }
    }

    private List<String> tokenize(String text) {

            return List.of(text.trim().split("\\s+"));
    }
}

package com.example.fintrackerpro.telegram.service;

import org.springframework.stereotype.Service;

@Service
public class CommandService {

    public String convertButtonToCommand(String buttonText) {
        if (buttonText == null) {
            return "";
        }
        return switch (buttonText.trim()) {
            case "🚀 Старт" -> "/start";
            case "📈 Новые доходы" -> "NEW_INCOME";
            case "📉 Новые расходы" -> "NEW_EXPENSE";
            case "Список доходов" -> "/incomes";
            case "Список расходов" -> "/expenses";
            case "Норма сбережений" -> "/savings";
            default -> buttonText;
        };
    }
    public boolean isCommand(String text) {
        return text != null && text.startsWith("/");
    }

    public boolean isMenuButton(String text) {
        return switch (text != null ? text.trim() : "") {
            case "🚀 Старт",
                 "📈 Новые доходы",
                 "📉 Новые расходы",
                 "Список доходов",
                 "Список расходов",
                 "Норма сбережений" -> true;
            default -> false;
        };
    }
    public boolean isIncomeCommand(String text){
        return text != null && text.startsWith("/income");
    }

    public boolean isExpenseCommand(String text){
        return text!= null && text.startsWith("/expense");
    }

    // Получает первое слово из текста
    public String getFirstWord(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int spaceIndex = text.indexOf(' ');
        return spaceIndex == -1 ? text : text.substring(0, spaceIndex);
    }
    // Проверяет, была ли команда распознана
    public boolean isKnownCommand(String command) {
        return switch (command) {
            case "/start",
                 "/incomes",
                 "/expenses",
                 "/savings",
                 "NEW_INCOME",
                 "NEW_EXPENSE" -> true;
            default -> false;
        };
    }

}

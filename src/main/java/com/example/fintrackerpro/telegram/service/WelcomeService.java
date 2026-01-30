package com.example.fintrackerpro.telegram.service;

import org.springframework.stereotype.Service;

@Service
public class WelcomeService {
    public String getNewUserWelcome(String userName){
        return "👋 Привет, " + userName + "!\n\n" +
                "Я FinTrackerBot - твой помощник в управлении финансами.\n\n" +
                "📊 Я помогу тебе:\n" +
                "• Отслеживать доходы и расходы\n" +
                "• Рассчитать норму сбережений\n\n" +
                "Давайте начнём! 🚀";
    }
    public String getReturningUserWelcome(String userName) {
        return "🎯 Добро пожаловать, " + userName + "!";
    }
    public String getUnknownCommandMessage(String command) {
        return "❓ Я не понял команду: <b>" + command + "</b>\n\n" +
                "Пожалуйста, выбери одно из действий в меню ниже.";
    }
    public String getIncomeErrorMessage() {
        return "❌ Не получилось сохранить доход.\n" +
                "Попробуй ещё раз через «📈 Новые доходы».";
    }
    public String getExpenseErrorMessage() {
        return "❌ Не получилось сохранить расход.\n" +
                "Попробуй ещё раз через «📉 Новые расходы».";
    }
    public String getInvalidAmountMessage() {
        return "❌ Сумма — только число! (1500.50)";
    }
    public String getInvalidDateMessage() {
        return "❌ Неверный формат даты!\n\n" +
                "Формат: ДД.ММ.ГГГГ\n" +
                "Пример: 15.01.2026";
    }

}

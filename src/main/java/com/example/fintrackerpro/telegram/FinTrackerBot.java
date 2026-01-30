package com.example.fintrackerpro.telegram;

import com.example.fintrackerpro.telegram.handler.SummaryHandler;
import com.example.fintrackerpro.telegram.http.FinTrackerApiClient;
import com.example.fintrackerpro.telegram.keyboard.expenses.ExpenseCategoryKeyboardFactory;
import com.example.fintrackerpro.telegram.keyboard.incomes.IncomeCategoryKeyboardFactory;
import com.example.fintrackerpro.telegram.keyboard.MainKeyboardFactory;
import com.example.fintrackerpro.telegram.keyboard.incomes.SkipKeyboardFactory;
import com.example.fintrackerpro.telegram.keyboard.incomes.TodayKeyboardFactory;
import com.example.fintrackerpro.telegram.month.YearMonthHandler;
import com.example.fintrackerpro.telegram.service.CommandService;
import com.example.fintrackerpro.telegram.service.UserServiceTelegram;
import com.example.fintrackerpro.telegram.service.WelcomeService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FinTrackerBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final FinTrackerApiClient apiClient;
    private final SummaryHandler summaryHandler;
    private final YearMonthHandler yearMonthHandler;
    private final WelcomeService welcomeService;
    private final CommandService commandService;
    private final UserServiceTelegram userService;
    private static final Map<Long, String> recordTypeBeingEdited = new HashMap<>();

    public FinTrackerBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            FinTrackerApiClient apiClient, SummaryHandler summaryHandler, YearMonthHandler yearMonthHandler, WelcomeService welcomeService, CommandService commandService, UserServiceTelegram userService
    ) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.apiClient = apiClient;
        this.summaryHandler = summaryHandler;
        this.yearMonthHandler = yearMonthHandler;
        this.welcomeService = welcomeService;
        this.commandService = commandService;
        this.userService = userService;
        log.info("🤖 FinTrackerBot инициализирован: @{}", botUsername);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update updates) {
        // Сначала обрабатываем callback queries
        if (!updates.hasMessage() || !updates.getMessage().hasText()) {
            if (updates.hasCallbackQuery()) {
                log.debug("🔘 Callback: {}", updates.getCallbackQuery().getData());
                onCallbackQueryReceived(updates.getCallbackQuery());
            }
            return;
        }


        Long chatId = updates.getMessage().getChatId();
        String text = updates.getMessage().getText().trim();
        log.info("💬 Сообщение от chatId={}: {}", chatId, text);

        String command = commandService.convertButtonToCommand(text);

        if ("/start".equals(command)) {
            handleStart(chatId, updates);
            return;
        }
        if ("/summary".equals(command) || "/savings".equals(command)) {
            String msg = summaryHandler.handleSummary(chatId, text);
            sendText(chatId, msg, MainKeyboardFactory.create());
            return;
        }

        if ("/incomes".equals(command)) {
            handleIncomesList(chatId);
            return;
        }
        if ("/expenses".equals(command)) {
            // TODO: расходы потом
            sendText(chatId, "Расходы пока не готовы", MainKeyboardFactory.create());
            return;
        }


        if ("NEW_INCOME".equals(command)) {
            startIncomeWizard(chatId);
            return;
        }
        if ("NEW_EXPENSE".equals(command)) {
            startExpenseWizard(chatId);
            return;
        }

        State state = states.getOrDefault(chatId, State.NONE);
        if (state != State.NONE) {

            // выбор номера записи для редактирования
            if (state == State.EDITING_CHOOSE_INDEX) {
                handleEditingChooseIndex(chatId, text);
                return;
            }
            if (state == State.EDITING_ENTER_AMOUNT) {
                handleEditingEnterAmount(chatId, text);
                return;
            }
            if (state == State.EDITING_ENTER_DATE) {
                handleEditingEnterDate(chatId, text);
                return;
            }
            if (state == State.EDITING_ENTER_CATEGORY) {
                handleEditingEnterCategory(chatId, text);
                return;
            }

            if (state.name().startsWith("EXPENSE_")) {
                handleExpenseWizardStep(chatId, text, state);
            } else {
                handleIncomeWizardStep(chatId, text, state);
            }
            return;
        }
        if (text.startsWith("/income")) {
            handleIncome(chatId, text);
        } else if (text.startsWith("/expense")) {
            handleExpense(chatId, text);
        }
    }

    private void handleEditingChooseIndex(Long chatId, String text) {
        try {
            if ("/skip".equals(text)) {
                states.put(chatId, State.NONE);
                sendText(chatId, "❌ Отменено", MainKeyboardFactory.create());
                return;
            }
            int recordIndex = Integer.parseInt(text.trim());
            Long recordId = yearMonthHandler.getRecordIdByIndex(recordIndex);

            if (recordId == null || recordId == 0) {
                sendText(chatId, "❌ Запись #" + recordIndex + " не найдена! Введи правильный номер.", MainKeyboardFactory.create());
                return;
            }
            String recordType = "income";
            Map<String, Object> record = null;
            try {
                record = apiClient.getIncomeById(recordId);
                recordType = "income";
            } catch (Exception e) {
                try {
                    record = apiClient.getExpenseById(recordId);
                    recordType = "expense";
                } catch (Exception exception) {
                    sendText(chatId, "❌ Не смог получить данные записи!", MainKeyboardFactory.create());
                    states.put(chatId, State.NONE);
                    return;
                }
            }
            if (record == null) {
                sendText(chatId, "❌ Запись не найдена!", MainKeyboardFactory.create());
                states.put(chatId, State.NONE);
                return;
            }
            editingRecordIds.put(chatId, recordId);
            PendingIncome pendingIncome = new PendingIncome();
            pendingIncome.amount = new BigDecimal(record.get("amount").toString());
            String dateStr = record.get("date").toString();
            if (dateStr.length() >= 10) {
                String[] parts = dateStr.substring(0, 10).split("-");
                pendingIncome.date = LocalDate.of(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

            }
            pendingIncome.category = record.get("category") != null ? record.get("category").toString() : "";
            pendingIncome.source = record.get("source") != null ? record.get("source").toString() : "";
            pendingIncome.description = record.get("description") != null ? record.get("description").toString() : "";
            pendingIncomes.put(chatId, pendingIncome);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"));
            String msg = "Редактирование записи:" + " " + recordIndex + "\n\n" +
                    "Текущие данные:\n" +
                    "💰Сумма: " + pendingIncome.amount + " ₽\n" +
                    "📁Категория: " + pendingIncome.category + "\n" +
                    "📅 Дата: " + pendingIncome.date.format(fmt) + "\n\n" +
                    "Шаг 1 из 3\n" +
                    "Давай обновим данные. Начнём с суммы:";

            states.put(chatId, State.EDITING_ENTER_AMOUNT);
            sendText(chatId, msg, SkipKeyboardFactory.create());
            recordTypeBeingEdited.put(chatId, recordType);
        } catch (NumberFormatException e) {
            sendText(chatId, "❌ Ошибка! Введи число для номера.", MainKeyboardFactory.create());
        } catch (Exception e) {
            e.printStackTrace();
            states.put(chatId, State.NONE);
            sendText(chatId, "❌ Ошибка при редактировании!", MainKeyboardFactory.create());
        }
    }

    private void handleEditingEnterAmount(Long chatId, String text) {
        PendingIncome pending = pendingIncomes.get(chatId);
        if (pending == null) {
            states.put(chatId, State.NONE);
            sendText(chatId, "❌ Ошибка! Начни заново.", MainKeyboardFactory.create());
            return;
        }
        try {
            if ("Пропустить".equals(text)) {
                // Переходим к выбору даты
                String msg = "Шаг 2 из 3\n" +
                        "📅 Введи новую дату\n" +
                        "Формат:(например: 25.01.2026)";
                states.put(chatId,  State.EDITING_ENTER_DATE );
                sendText(chatId, msg, TodayKeyboardFactory.create());
                return;
            }
            try {
                // Очищаем текст от пробелов и заменяем запятую на точку
                String cleanedText = text.trim()              // Убираем пробелы в начале/конце
                        .replace(" ", "")                      // Убираем пробелы внутри (120 000 → 120000)
                        .replace(",", ".");                    // Заменяем запятую на точку (120,50 → 120.50)

                // Парсим число
                BigDecimal amount = new BigDecimal(cleanedText);

                // Проверяем что число больше нуля
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    sendText(chatId,
                            "❌ Сумма должна быть больше нуля\n\n" +
                                    "Введи положительное число (например: 50000)",
                            SkipKeyboardFactory.create());
                    return;
                }
                pending.amount = amount;
            } catch (NumberFormatException e) {
                sendText(chatId,
                        "❌ Это не похоже на сумму\n\n" +
                                "Введи число (например: 50000 или 50000.50)",
                        SkipKeyboardFactory.create());
                return;
            }
            String msg = "Шаг 2 из 3\n" +
                    "📅 Введи новую дату\n" +
                    "Формат:(например: 25.01.2026)";

            states.put(chatId, State.EDITING_ENTER_DATE);
            sendText(chatId, msg, TodayKeyboardFactory.create());

        } catch (Exception e) {
            // Ловим любые неожиданные ошибки
            e.printStackTrace();
            states.put(chatId, State.NONE);
            sendText(chatId, "❌ Ошибка при обработке!", MainKeyboardFactory.create());

            // Очищаем временные данные
            pendingIncomes.remove(chatId);
            editingRecordIds.remove(chatId);
        }
    }

    private void handleEditingEnterDate(Long chatId, String text) {
        PendingIncome pending = pendingIncomes.get(chatId);
        if (pending == null) {
            states.put(chatId, State.NONE);
            sendText(chatId, "❌ Ошибка! Начни заново.", MainKeyboardFactory.create());
            return;
        }

        try {
            if ("Пропустить".equals(text)) {
                // Пропускаем дату, переходим к категории
                String msg = "📁 Выбери категорию...";
                states.put(chatId, State.EDITING_ENTER_CATEGORY);
                sendText(chatId, msg, IncomeCategoryKeyboardFactory.create());
                return;  // ← ВАЖНО!
            }
            if ("📅 Сегодня".equals(text) || "Сегодня".equalsIgnoreCase(text)) {
                pending.date = LocalDate.now();
                } else {
                    try {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        pending.date = LocalDate.parse(text, fmt);
                    } catch (Exception e) {
                        sendText(chatId,
                                "❌ Неверный формат даты\n\n" +
                                        "Используй: ДД.ММ.ГГГГ (например: 22.01.2026)",
                                TodayKeyboardFactory.create());
                        return;
                    }
                }
            String msg = "📁 Выбери категорию...";
            states.put(chatId, State.EDITING_ENTER_CATEGORY);
            sendText(chatId, msg, IncomeCategoryKeyboardFactory.create());
        } catch (Exception e) {
            e.printStackTrace();
            states.put(chatId, State.NONE);
            sendText(chatId, "❌ Ошибка!", MainKeyboardFactory.create());
            pendingIncomes.remove(chatId);
            editingRecordIds.remove(chatId);
        }
    }

    private void handleEditingEnterCategory(Long chatId, String text) {
        PendingIncome pending = pendingIncomes.get(chatId);
        Long recordId = editingRecordIds.get(chatId);
        if (pending == null || recordId == null) {
            states.put(chatId, State.NONE);
            sendText(chatId, "❌ Ошибка! Начни заново.", MainKeyboardFactory.create());
            return;
        }
        try {
            if (!"Пропустить".equals(text)) {
                pending.category = text;
            }
            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("userId", user.id());  // ← ВАЖНО!
            updateMap.put("amount", pending.amount);
            updateMap.put("date", pending.date.toString());
            updateMap.put("category", pending.category);
            updateMap.put("description", pending.description);
            updateMap.put("source", pending.source);

            String recordType = recordTypeBeingEdited.getOrDefault(chatId, "income");
            if ("income".equals(recordType)) {
                apiClient.updateIncome(recordId, updateMap);
            } else {
                apiClient.updateExpense(recordId, updateMap);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String msg = "✅Запись успешно отредактирована!\n\n" +
                    "Новые данные:\n" +
                    "Сумма: " + pending.amount + " ₽\n" +
                    "Дата: " + pending.date.format(fmt) + "\n" +
                    "Категория: " + pending.category;

            sendText(chatId, msg, MainKeyboardFactory.create());

            states.put(chatId, State.NONE);
            pendingIncomes.remove(chatId);
            editingRecordIds.remove(chatId);
            recordTypeBeingEdited.remove(chatId);

        } catch (Exception e) {
            e.printStackTrace();
            states.put(chatId, State.NONE);
            sendText(chatId, "❌ Ошибка при редактировании!", MainKeyboardFactory.create());
        }
    }


    /**
     * Обработка нажатия "Список доходов"
     * Показываем года для выбора через callback
     */
    private void handleIncomesList(Long chatId) {
        try {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("📅 Выбери год:");
            msg.enableHtml(true);

            // Клавиатура с годами (callback buttons)
            InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder().text("📅 2026").callbackData("incomes:2026").build(),
                            InlineKeyboardButton.builder().text("📅 2027").callbackData("incomes:2027").build(),
                            InlineKeyboardButton.builder().text("📅 2028").callbackData("incomes:2028").build()
                    ))
                    .build();
            msg.setReplyMarkup(kb);

            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId, "Ошибка при загрузке доходов", MainKeyboardFactory.create());
        }
    }

    private void handleExpenseWizardStep(Long chatId, String text, State state) {
        PendingExpense pending = pendingExpenses.get(chatId);
        if (pending == null) {
            states.put(chatId, State.NONE);
            sendText(chatId, "Что-то пошло не так, давай начнём заново: нажми «📉 Новые расходы».", MainKeyboardFactory.create());
            return;
        }

        // Проверка ОТМЕНЫ на любом шаге
        if ("❌ Отмена".equals(text)) {
            states.put(chatId, State.NONE);
            pendingExpenses.remove(chatId);
            sendText(chatId, "❌ Операция отменена. Вернёмся в меню.", MainKeyboardFactory.create());
            return;
        }

        try {
            switch (state) {
                case EXPENSE_AMOUNT -> {
                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(text.replace(",", "."));
                    } catch (NumberFormatException e) {
                        sendText(chatId, "❌ Сумма — только число! (1500.50)", TodayKeyboardFactory.create());
                        return;
                    }
                    pending.amount = amount;
                    states.put(chatId, State.EXPENSE_DATE);
                    sendTextWithKeyboard(chatId,
                            "Шаг 2 из 4.\n" +
                                    "Введи дату расхода в формате ДД.ММ.ГГГГ.\n" +
                                    "Например: 15.01.2026.\n" +
                                    "Или нажми кнопку «Сегодня» ниже.",
                            TodayKeyboardFactory.create());
                }
                case EXPENSE_DATE -> {
                    // Проверка НАЗАД
                    if ("⬅️ Назад".equals(text)) {
                        states.put(chatId, State.EXPENSE_AMOUNT);
                        sendTextWithKeyboard(chatId,
                                "Шаг 1 из 4.\n" +
                                        "Введи сумму расхода (только число, например 1500):",
                                TodayKeyboardFactory.create());
                        return;
                    }

                    LocalDate date;
                    if ("сегодня".equalsIgnoreCase(text) || "Сегодня".equals(text)) {
                        date = LocalDate.now();
                    } else {
                        try {
                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                            date = LocalDate.parse(text, fmt);
                        } catch (Exception e) {
                            sendText(chatId, "❌ Неверный формат даты!\n\nФормат: ДД.ММ.ГГГГ\nПример: 15.01.2026", TodayKeyboardFactory.create());
                            return;
                        }
                    }
                    pending.date = date;

                    states.put(chatId, State.EXPENSE_CATEGORY);
                    sendTextWithKeyboard(chatId,
                            "Шаг 3 из 4.\n" +
                                    "Выбери категорию расхода или напиши свою:\n",
                            ExpenseCategoryKeyboardFactory.create());
                }
                case EXPENSE_CATEGORY -> {
                    // Проверка НАЗАД
                    if ("⬅️ Назад".equals(text)) {
                        states.put(chatId, State.EXPENSE_DATE);
                        sendTextWithKeyboard(chatId,
                                "Шаг 2 из 4.\n" +
                                        "Введи дату расхода в формате ДД.ММ.ГГГГ.\n" +
                                        "Например: 15.01.2026.",
                                TodayKeyboardFactory.create());
                        return;
                    }

                    pending.category = text;
                    states.put(chatId, State.EXPENSE_DESCRIPTION);
                    sendTextWithKeyboard(chatId,
                            "Шаг 4 из 4.\n" +
                                    "Коротко опиши расход (или нажми «Пропустить»):\n",
                            SkipKeyboardFactory.create());
                }
                case EXPENSE_DESCRIPTION -> {
                    // Проверка НАЗАД
                    if ("⬅️ Назад".equals(text)) {
                        states.put(chatId, State.EXPENSE_CATEGORY);
                        sendTextWithKeyboard(chatId,
                                "Шаг 3 из 4.\n" +
                                        "Выбери категорию расхода или напиши свою:\n",
                                ExpenseCategoryKeyboardFactory.create());
                        return;
                    }

                    if (!"пропустить".equalsIgnoreCase(text)) {
                        pending.description = text;
                    }
                    finishExpenseWizard(chatId, pending);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId,
                    "Не смог понять ответ.\n" +
                            "Попробуй ещё раз или нажми /start, чтобы начать сначала.", MainKeyboardFactory.create());
        }
    }


    private void finishExpenseWizard(Long chatId, PendingExpense pending) {
        try {
            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);

            FinTrackerApiClient.ExpenseCreateRequest req =
                    new FinTrackerApiClient.ExpenseCreateRequest(
                            user.id(),
                            pending.amount,
                            pending.category,
                            pending.description
                    );

            FinTrackerApiClient.ExpenseDto expense = apiClient.addExpense(req);

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String dateStr = pending.date.format(dateFmt);

            StringBuilder expenseBuilder = new StringBuilder();
            expenseBuilder.append(
                    String.format("✅ Расход добавлен:\n• %s ₽ | %s", expense.amount(), dateStr));

            if (expense.category() != null && !expense.category().isEmpty()) {
                expenseBuilder.append("\n").append(expense.category());
            }
            if (expense.description() != null && !expense.description().isEmpty()) {
                expenseBuilder.append("\n").append(expense.description());
            }

            String expenseText = expenseBuilder.toString();
            sendText(chatId, expenseText, MainKeyboardFactory.create());
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId,
                    "Не получилось сохранить расход. Попробуй ещё раз через «📉 Новые расходы».", MainKeyboardFactory.create());
        } finally {
            states.put(chatId, State.NONE);
            pendingExpenses.remove(chatId);
        }
    }

    private void startExpenseWizard(Long chatId) {
        PendingExpense pendingExpense = new PendingExpense();
        pendingExpenses.put(chatId, pendingExpense);
        states.put(chatId, State.EXPENSE_AMOUNT);
        sendText(chatId,
                "Добавим расход.\n\n" +
                        "Шаг 1 из 4.\n" +
                        "Введи сумму расхода (только число, например 1500):", MainKeyboardFactory.create());
    }

    private void startIncomeWizard(Long chatId) {
        PendingIncome pending = new PendingIncome();
        pendingIncomes.put(chatId, pending);
        states.put(chatId, State.INCOME_AMOUNT);

        sendText(chatId,
                "Добавим доход.\n\n" +
                        "Шаг 1 из 5.\n" +
                        "Введи сумму дохода (только число, например 50000):", MainKeyboardFactory.create());
    }

    private void sendTextWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup kb) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.setReplyMarkup(kb);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleExpense(Long chatId, String text) {
        List<String> parts = tokenize(text);

        if (parts.size() < 3) {
            sendText(chatId,
                    "Чтобы добавить расход, напиши так:\n" +
                            "/expense <сумма> <категория> \"описание\"\n\n" +
                            "Например:\n" +
                            "/expense 1500 Продукты \"Магнит вечером\"", MainKeyboardFactory.create());
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(parts.get(1));
            String category = parts.get(2);
            String description = parts.size() >= 4 ? parts.get(3) : null;

            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);

            FinTrackerApiClient.ExpenseCreateRequest req =
                    new FinTrackerApiClient.ExpenseCreateRequest(
                            user.id(),
                            amount,
                            category,
                            description
                    );

            FinTrackerApiClient.ExpenseDto expense = apiClient.addExpense(req);

            sendText(chatId, String.format(
                    "Расход %s ₽ добавлен.\nКатегория: %s.\nОписание: %s.",
                    expense.amount(),
                    expense.category(),
                    expense.description() != null ? expense.description() : "-"
            ), MainKeyboardFactory.create());
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId,
                    "Не смог разобрать команду.\n" +
                            "Формат: /expense <сумма> <категория> \"описание\"", MainKeyboardFactory.create());
        }
    }

    private void handleStart(Long chatId, Update update) {
        String firstName = update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName();
        String userName = (firstName != null ? firstName : "") +
                (lastName != null ? " " + lastName : "");
        UserServiceTelegram.UserData userData = userService.getOrRegisterUser(chatId, firstName, lastName);

        String welcomeMessage = userData.isNewUser() ?
                welcomeService.getNewUserWelcome(userData.getDisplayName()) :
                welcomeService.getReturningUserWelcome(userData.getDisplayName());
        sendText(chatId, welcomeMessage, MainKeyboardFactory.create());
    }

    private void handleIncome(Long chatId, String text) {
        List<String> parts = tokenize(text);

        if (parts.size() < 4) {
            sendText(chatId,
                    "Формат: /income <сумма> <дата YYYY-MM-DD> <категория> \"источник\" \"описание\"\n" +
                            "Пример: /income 50000 2026-01-14 Продажи \"Интернет-магазин\" \"Продажи за январь\"", MainKeyboardFactory.create());
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(parts.get(1));
            LocalDate date = LocalDate.parse(parts.get(2));
            String category = parts.get(3);

            String source = parts.size() >= 5 ? parts.get(4) : null;
            String description = parts.size() >= 6 ? parts.get(5) : null;

            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);

            FinTrackerApiClient.IncomeCreateRequest req =
                    new FinTrackerApiClient.IncomeCreateRequest(
                            user.id(),
                            amount,
                            category,
                            source,
                            date,
                            description
                    );

            FinTrackerApiClient.IncomeDto income = apiClient.addIncome(req);

            sendText(chatId, String.format(
                    "Доход %s ₽ от %s добавлен.\nКатегория: %s.\nИсточник: %s.\nОписание: %s.",
                    income.amount(),
                    income.date(),
                    income.category(),
                    income.source() != null ? income.source() : "-",
                    income.description() != null ? income.description() : "-"
            ), MainKeyboardFactory.create());
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId,
                    "Не смог разобрать команду.\n" +
                            "Формат: /income <сумма> <дата YYYY-MM-DD> <категория> \"источник\" \"описание\"", MainKeyboardFactory.create());
        }
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder stringBuilder = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (Character.isWhitespace(c) && !inQuotes) {
                if (stringBuilder.length() > 0) {
                    tokens.add(stringBuilder.toString());
                    stringBuilder.setLength(0);
                }
            } else {
                stringBuilder.append(c);
            }
        }
        if (stringBuilder.length() > 0) {
            tokens.add(stringBuilder.toString());
        }
        return tokens;
    }

    private void sendText(Long chatId, String text, ReplyKeyboardMarkup kb) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        sendMessage.setParseMode("Markdown");
        if (kb != null) {
            sendMessage.setReplyMarkup(kb);
        }
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Для InlineKeyboardMarkup
    private void sendText(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleIncomeWizardStep(Long chatId, String text, State state) {
        PendingIncome pending = pendingIncomes.get(chatId);
        if (pending == null) {
            states.put(chatId, State.NONE);
            sendText(chatId, "Что-то пошло не так, давай начнём заново: нажми «📈 Новые доходы».", MainKeyboardFactory.create());
            return;
        }
        if ("❌ Отмена".equals(text)) {
            states.put(chatId, State.NONE);
            pendingIncomes.remove(chatId);
            sendText(chatId, "❌ Операция отменена. Вернёмся в меню.", MainKeyboardFactory.create());
            return;
        }

        try {
            switch (state) {
                case INCOME_AMOUNT -> {
                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(text.replace(",", "."));
                    } catch (NumberFormatException e) {
                        sendText(chatId, "❌ Сумма — только число! (1500.50)", MainKeyboardFactory.create());
                        return;
                    }
                    pending.amount = amount;
                    states.put(chatId, State.INCOME_DATE);
                    sendTextWithKeyboard(chatId,
                            "Шаг 2 из 5.\n" +
                                    "Введи дату дохода в формате ДД.ММ.ГГГГ.\n" +
                                    "Например: 15.01.2026.\n" +
                                    "Или нажми кнопку «Сегодня» ниже.",
                            TodayKeyboardFactory.create());
                }
                case INCOME_DATE -> {
                    if ("⬅️ Назад".equals(text)) {
                        states.put(chatId, State.INCOME_AMOUNT);
                        sendTextWithKeyboard(chatId, "Шаг 1 из 5.\n" +
                                        "Введи сумму дохода (только число, например 50000):",
                                TodayKeyboardFactory.create());
                        return;
                    }
                    LocalDate date;
                    if ("сегодня".equalsIgnoreCase(text) || "Сегодня".equals(text)) {
                        date = LocalDate.now();
                    } else {
                        try {
                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                            date = LocalDate.parse(text, fmt);
                        } catch (Exception e) {
                            sendText(chatId, "❌ Неверный формат даты!\n\nФормат: ДД.ММ.ГГГГ\nПример: 15.01.2026", TodayKeyboardFactory.create());
                            return;
                        }
                    }
                    pending.date = date;

                    states.put(chatId, State.INCOME_CATEGORY);
                    sendTextWithKeyboard(chatId,
                            "Шаг 3 из 5.\n" +
                                    "Напиши категорию дохода:\n",
                            IncomeCategoryKeyboardFactory.create());
                }
                case INCOME_CATEGORY -> {
                    if ("⬅️ Назад".equals(text)) {
                        states.put(chatId, State.INCOME_DATE);
                        sendTextWithKeyboard(chatId,
                                "Шаг 2 из 5.\n" +
                                        "Введи дату дохода в формате ДД.ММ.ГГГГ.\n" +
                                        "Например: 15.01.2026.",
                                TodayKeyboardFactory.create());
                        return;
                    }
                    pending.category = text;
                    states.put(chatId, State.INCOME_SOURCE);
                    sendTextWithKeyboard(chatId,
                            "Шаг 4 из 5.\n" +
                                    "Напиши источник:\n",
                            SkipKeyboardFactory.create());
                }
                case INCOME_SOURCE -> {
                    if ("⬅️ Назад".equals(text)) {
                        states.put(chatId, State.INCOME_CATEGORY);
                        sendTextWithKeyboard(chatId,
                                "Шаг 3 из 5.\n" +
                                        "Напиши категорию дохода:\n",
                                IncomeCategoryKeyboardFactory.create());
                        return;
                    }

                    if (!"пропустить".equalsIgnoreCase(text)) {
                        pending.source = text;
                    }
                    states.put(chatId, State.INCOME_DESCRIPTION);
                    sendTextWithKeyboard(chatId,
                            "Шаг 5 из 5.\n" +
                                    "Коротко опиши доход: \n",
                            SkipKeyboardFactory.create());
                }
                case INCOME_DESCRIPTION -> {
                    if ("⬅️ Назад".equals(text)) {
                        states.put(chatId, State.INCOME_SOURCE);
                        sendTextWithKeyboard(chatId,
                                "Шаг 4 из 5.\n" +
                                        "Напиши источник:\n",
                                SkipKeyboardFactory.create());
                        return;
                    }
                    if (!"пропустить".equalsIgnoreCase(text)) {
                        pending.description = text;
                    }
                    finishIncomeWizard(chatId, pending);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId,
                    "Не смог понять ответ.\n" +
                            "Попробуй ещё раз или нажми /start, чтобы начать сначала.",
                    MainKeyboardFactory.create());
        }
    }

    private void finishIncomeWizard(Long chatId, PendingIncome pending) {
        try {
            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);

            FinTrackerApiClient.IncomeCreateRequest req =
                    new FinTrackerApiClient.IncomeCreateRequest(
                            user.id(),
                            pending.amount,
                            pending.category,
                            pending.source,
                            pending.date,
                            pending.description
                    );

            FinTrackerApiClient.IncomeDto income = apiClient.addIncome(req);

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String dateStr = pending.date.format(dateFmt);

            StringBuilder incomeBuilder = new StringBuilder();
            incomeBuilder.append(String.format("✅ Доход добавлен:\n• %s ₽ | %s", income.amount(), dateStr));

            if (income.category() != null && !income.category().isEmpty()) {
                incomeBuilder.append("\n").append(income.category());
            }
            if (income.source() != null && !income.source().isEmpty()) {
                incomeBuilder.append(" • ").append(income.source());
            }
            if (income.description() != null && !income.description().isEmpty()) {
                incomeBuilder.append("\n").append(income.description());
            }

            String incomeText = incomeBuilder.toString();
            sendText(chatId, incomeText, MainKeyboardFactory.create());

        } catch (Exception e) {
            e.printStackTrace();
            sendText(chatId,
                    "Не получилось сохранить доход. Попробуй ещё раз через «📈 Новые доходы».", MainKeyboardFactory.create());
        } finally {
            states.put(chatId, State.NONE);
            pendingIncomes.remove(chatId);
        }
    }

    private final Map<Long, State> states = new ConcurrentHashMap<>();
    private final Map<Long, PendingIncome> pendingIncomes = new ConcurrentHashMap<>();
    private final Map<Long, PendingExpense> pendingExpenses = new ConcurrentHashMap<>();

    private static class PendingIncome {
        BigDecimal amount;
        LocalDate date;
        String category;
        String source;
        String description;
    }

    private static class PendingExpense {
        BigDecimal amount;
        LocalDate date;
        String category;
        String description;
    }

    private enum State {
        NONE,
        INCOME_AMOUNT,
        INCOME_DATE,
        INCOME_CATEGORY,
        INCOME_SOURCE,
        INCOME_DESCRIPTION,
        EXPENSE_AMOUNT,
        EXPENSE_DATE,
        EXPENSE_CATEGORY,
        EXPENSE_DESCRIPTION,
        // ← ДОБАВЬ ЭТИ:
        EDITING_CHOOSE_INDEX,        // Выбор номера записи
        EDITING_ENTER_AMOUNT,        // Ввод новой суммы
        EDITING_ENTER_DATE,          // Ввод новой даты
        EDITING_ENTER_CATEGORY
    }

    public void onCallbackQueryReceived(@NotNull CallbackQuery callbackQuery) {
        SendMessage sendMessage = new SendMessage();
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        log.info("🔘 Callback от chatId={}: {}", chatId, data);


        // 1. сначала спец-кнопки редактирования/удаления
        if ("edit_list".equals(data)) {           // <-- здесь важно чтобы data совпадало с callbackData кнопки
            handleEditListMode(callbackQuery);
            return;
        }


        // ✅ Проверка 1: Выбор типа (incomes/expenses) → годы
        if (yearMonthHandler.handleType(callbackQuery, sendMessage)) {
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        // ✅ Проверка 2: Выбор года → месяцы
        if (yearMonthHandler.handleYear(callbackQuery, sendMessage)) {
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        // ✅ Проверка 3: Выбор месяца → список доходов/расходов
        if (yearMonthHandler.handleMonth(callbackQuery, sendMessage)) {
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        // ✅ Обработка: Summary (баланс) и Savings (норма сбережений)
        if ("summary".equals(data) || "savings".equals(data)) {
            String msg = summaryHandler.handleSummary(chatId, data);
            sendMessage.setChatId(chatId.toString());
            sendMessage.setText(msg);
            sendMessage.setReplyMarkup(MainKeyboardFactory.create());
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        // ✅ Главное меню
        if ("main".equals(data)) {
            sendMessage.setChatId(chatId.toString());
            sendMessage.setText("🏠 Главное меню:");
            sendMessage.setReplyMarkup(MainKeyboardFactory.create());
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

//        if (data.startsWith("delete_list:")) {
//            handleDeleteListMode(callbackQuery);
//            return;
//        }

        if (data.startsWith("delete:")) {
            handleDeleteCallback(callbackQuery);
            return;
        }


        // ❌ Неизвестная кнопка
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("❓ Неизвестная кнопка: " + data);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void registerBotCommands() throws TelegramApiException {
        var commands = java.util.List.of(
                new BotCommand("/start", "🚀 Запустить бота и показать приветствие"),
                new BotCommand("/incomes", "📈 Показать список доходов"),
                new BotCommand("/expenses", "📉 Показать список расходов"),
                new BotCommand("/savings", "💰 Норма сбережений"),
                new BotCommand("/help", "❓ Справка по командам")
        );

        SetMyCommands setMyCommands = new SetMyCommands(commands, new BotCommandScopeDefault(), null);
        execute(setMyCommands);

        log.info("📝 Зарегистрировано {} команд", commands.size());
    }

    private void handleEditListMode(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("✏️ <b>Редактирование записи</b>\n\n" +
                "📝 Введи <b>номер записи</b> которую хочешь отредактировать.\n\n" +
                "Например: <code>10</code>");
        message.enableHtml(true);
        message.setParseMode("HTML");
        message.setReplyMarkup(SkipKeyboardFactory.create());

        try {
            execute(message);
            states.put(chatId, State.EDITING_CHOOSE_INDEX);

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            answer.setText("✏️ Введи номер записи");
            execute(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleDeleteCallback(CallbackQuery callbackQuery) {
        try {
            String data = callbackQuery.getData();
            Long recordId = Long.parseLong(data.split(":")[1]);
            Long chatId = callbackQuery.getMessage().getChatId();

            try {
                apiClient.deleteIncome(recordId);
            } catch (Exception e) {
                try {
                    apiClient.deleteExpense(recordId);
                } catch (Exception ex) {
                    throw new Exception("Запись не найдена");
                }
            }

            sendText(chatId, "✅ <b>Запись #" + recordId + " удалена!</b>", MainKeyboardFactory.create());

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            answer.setText("✅ Удалено!");
            execute(answer);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(callbackQuery.getId());
                answer.setText("❌ Ошибка удаления!");
                answer.setShowAlert(true);
                execute(answer);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Map<Long, Long> editingRecordIds = new ConcurrentHashMap<>();


    private InlineKeyboardMarkup getSkipButton() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton skipBtn = new InlineKeyboardButton();
        skipBtn.setText("⏭️ Пропустить");
        skipBtn.setCallbackData("/skip");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(skipBtn);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);
        keyboard.setKeyboard(rows);
        return keyboard;
    }
}
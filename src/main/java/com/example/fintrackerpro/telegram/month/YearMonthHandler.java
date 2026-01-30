package com.example.fintrackerpro.telegram.month;

import com.example.fintrackerpro.telegram.http.FinTrackerApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor

public class YearMonthHandler {
    private final FinTrackerApiClient apiClient;

    private int recordIndex = 1;
    private Map<Integer, Long> recordIndexToId = new ConcurrentHashMap<>();

    public boolean handleType(CallbackQuery callbackQuery, SendMessage message) {
        String data = callbackQuery.getData();
        if (!("incomes".equals(data) || "expenses".equals(data))) {
            return false;
        }

        Long chatId = callbackQuery.getMessage().getChatId();
        message.setChatId(chatId.toString());
        message.setText("📅 Выбери год:");
        message.enableHtml(true);

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("📅 2026").callbackData(data + ":2026").build(),
                        InlineKeyboardButton.builder().text("📅 2027").callbackData(data + ":2027").build(),
                        InlineKeyboardButton.builder().text("📅 2028").callbackData(data + ":2028").build()
                ))
                .build();
        message.setReplyMarkup(kb);
        return true;
    }

    public boolean handleYear(CallbackQuery callbackQuery, SendMessage message) {
        String[] parts = callbackQuery.getData().split(":");
        if (parts.length != 2) {
            return false;
        }

        String type = parts[0]; // incomes/expenses
        String year = parts[1]; // 2026

        Long chatId = callbackQuery.getMessage().getChatId();
        message.setChatId(chatId.toString());
        message.setText("📅 Месяцы " + year + ":");
        message.enableHtml(true);

        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(monthButtons(1, 4, type, year))
                .keyboardRow(monthButtons(5, 8, type, year))
                .keyboardRow(monthButtons(9, 12, type, year))
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder().text("◀️ Годы").callbackData(type).build()
                ))
                .build();
        message.setReplyMarkup(kb);
        return true;
    }

    private List<InlineKeyboardButton> monthButtons(int start, int end, String type, String year) {
        return IntStream.rangeClosed(start, end)
                .mapToObj(m -> InlineKeyboardButton.builder()
                        .text(getShortMonth(m))
                        .callbackData(type + ":" + year + ":" + m)
                        .build())
                .collect(Collectors.toList());
    }

    private String getShortMonth(int month) {
        return switch (month) {
            case 1 -> "Янв";
            case 2 -> "Фев";
            case 3 -> "Мар";
            case 4 -> "Апр";
            case 5 -> "Май";
            case 6 -> "Июн";
            case 7 -> "Июл";
            case 8 -> "Авг";
            case 9 -> "Сен";
            case 10 -> "Окт";
            case 11 -> "Ноя";
            case 12 -> "Дек";
            default -> month + "";
        };
    }

    public boolean handleMonth(CallbackQuery callbackQuery, SendMessage message) {
        String[] parts = callbackQuery.getData().split(":");
        if (parts.length < 3)
            return false;

        String type = parts[0]; // incomes/expenses
        int year = Integer.parseInt(parts[1]);
        int month = Integer.parseInt(parts[2]);
        int page = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;

        Long chatId = callbackQuery.getMessage().getChatId();
        message.setChatId(chatId.toString());

        try {
            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);
            FinTrackerApiClient.ApiPage apiPage;
            if ("incomes".equals(type)) {
                apiPage = apiClient.getIncomesByUserAndMonth(user.id(), year, month, page);
            } else {
                apiPage = apiClient.getExpensesByUserAndMonth(user.id(), year, month);
            }

            String monthName = getMonthName(month);
            String title = String.format("📊 %s %d (%d записей)\n\n", monthName, year, apiPage.totalElements());

            StringBuilder sb = new StringBuilder(title);
            recordIndex = 1;
            recordIndexToId.clear();
            for (Object obj : apiPage.content()) {
                if (obj instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> record = (Map<String, Object>) m;
                    Object idObj = record.get("id");
                    Long recordId = idObj != null ? ((Number) idObj).longValue() : 0L;
                    recordIndexToId.put(recordIndex, recordId);
                    sb.append(formatRecord(record, type));
                }
            }

            message.setText(sb.toString());
            message.enableHtml(true);

            // Кнопка навигации
            InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(InlineKeyboardButton.builder()
                                    .text("✏️ Редактировать")
                                    .callbackData("edit_list")
                                    .build()
                    ))
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder()
                                    .text("◀️ " + getTypeName(type))
                                    .callbackData(type + ":" + year)
                                    .build()
                    ))
                    .build();
            message.setReplyMarkup(kb);

        } catch (Exception e) {
            message.setText("❌ Ошибка загрузки списка за период.");
        }

        return true;
    }

    private String getTypeName(String type) {
        return "incomes".equals(type) ? "История доходов" : "История расходов";
    }


    private String getMonthName(int month) {
        return switch (month) {
            case 1 -> "Январь";
            case 2 -> "Февраль";
            case 3 -> "Март";
            case 4 -> "Апрель";
            case 5 -> "Май";
            case 6 -> "Июнь";
            case 7 -> "Июль";
            case 8 -> "Август";
            case 9 -> "Сентябрь";
            case 10 -> "Октябрь";
            case 11 -> "Ноябрь";
            case 12 -> "Декабрь";
            default -> String.valueOf(month);
        };
    }


    private String formatRecord(Map<String, Object> record, String type) {
        BigDecimal amount = getAmount(record.get("amount"));

        String dateStr = String.valueOf(record.getOrDefault("date", ""));
        String dateShort = dateStr.length() >= 10
                ? dateStr.substring(8, 10) + "." + dateStr.substring(5, 7) : dateStr;

        String category = String.valueOf(record.getOrDefault("category", "Без категории"));
        String description = String.valueOf(record.getOrDefault("description", ""));
        String source = String.valueOf(record.getOrDefault("source", ""));

        // Фильтруем "null" и пустые строки
        String extra = "";
        if (description != null && !description.isBlank() && !"null".equals(description)) {
            extra = description;
        } else if (source != null && !source.isBlank() && !"null".equals(source)) {
            extra = source;
        }

        String result = String.format(
                "%d. • %,.0f ₽ | %s%n %s%s%n%n",
                recordIndex,
                amount,
                dateShort,
                category,
                extra.isBlank() ? "" : " • " + extra
        );

        recordIndex++;
        return result;
    }
    public Long getRecordIdByIndex(int index) {
        return recordIndexToId.get(index);
    }

    private BigDecimal getAmount(Object amountObj) {
        if (amountObj instanceof Number num) return new BigDecimal(num.toString());
        return new BigDecimal(amountObj.toString());
    }

}

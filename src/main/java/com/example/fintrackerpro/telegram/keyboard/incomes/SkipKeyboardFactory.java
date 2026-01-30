package com.example.fintrackerpro.telegram.keyboard.incomes;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class SkipKeyboardFactory {

    public static ReplyKeyboardMarkup create() {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setSelective(true);
        kb.setResizeKeyboard(true);
        kb.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Ряд 1: Пропустить
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Пропустить"));
        keyboard.add(row1);

        // Ряд 2: Назад и Отмена
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⬅️ Назад"));
        row2.add(new KeyboardButton("❌ Отмена"));
        keyboard.add(row2);

        kb.setKeyboard(keyboard);
        return kb;
    }
}

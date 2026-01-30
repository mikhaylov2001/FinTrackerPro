package com.example.fintrackerpro.telegram.keyboard.expenses;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class ExpenseCategoryKeyboardFactory {
    public static ReplyKeyboardMarkup create() {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setSelective(true);
        kb.setResizeKeyboard(true);
        kb.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();


        KeyboardRow row1 = new KeyboardRow();
        row1.add("Продукты");
        row1.add("Жильё");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Транспорт");
        row2.add("Фитнес");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Развлечения");
        row3.add("Другое");
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("⬅️ Назад"));
        row4.add(new KeyboardButton("❌ Отмена"));
        keyboard.add(row4);

        kb.setKeyboard(keyboard);
        return kb;

    }
}
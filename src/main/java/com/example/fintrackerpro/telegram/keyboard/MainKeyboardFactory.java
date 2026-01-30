package com.example.fintrackerpro.telegram.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;


    public class MainKeyboardFactory {

        public static ReplyKeyboardMarkup create() {
            ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
            kb.setResizeKeyboard(true);
            kb.setOneTimeKeyboard(false);

            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton("🚀 Старт"));
            keyboard.add(row1);

            KeyboardRow row2 = new KeyboardRow();
            row2.add("📈 Новые доходы");
            row2.add("📉 Новые расходы");
            keyboard.add(row2);

            // Ряд 3: Списки
            KeyboardRow row3 = new KeyboardRow();
            row3.add("Список доходов");
            row3.add("Список расходов");
            keyboard.add(row3);


            // Ряд 4: Сводка
            KeyboardRow row4 = new KeyboardRow();
            row4.add("Норма сбережений");
            keyboard.add(row4);

            kb.setKeyboard(keyboard);
            return kb;
        }
    }

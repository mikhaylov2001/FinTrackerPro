-- Итоговый доход по пользователям
CREATE OR REPLACE VIEW user_income_summary AS
SELECT
    u.id,
    u.chat_id,
    u.user_name,
    COUNT(i.id)                         AS income_count,
    COALESCE(SUM(i.amount), 0)          AS total_income,
    COALESCE(AVG(i.amount), 0)          AS avg_income,
    MAX(i.date)                         AS last_income_date
FROM users u
         LEFT JOIN incomes i ON u.id = i.user_id
GROUP BY u.id, u.chat_id, u.user_name;

-- Итоговые расходы по пользователям
CREATE OR REPLACE VIEW user_expense_summary AS
SELECT
    u.id,
    u.chat_id,
    u.user_name,
    COUNT(e.id)                         AS expense_count,
    COALESCE(SUM(e.amount), 0)          AS total_expenses,
    COALESCE(AVG(e.amount), 0)          AS avg_expense,
    MAX(e.date)                         AS last_expense_date
FROM users u
         LEFT JOIN expenses e ON u.id = e.user_id
GROUP BY u.id, u.chat_id, u.user_name;

-- Финансовый баланс по пользователям
CREATE OR REPLACE VIEW user_financial_summary AS
SELECT
    u.id,
    u.chat_id,
    u.user_name,
    COALESCE(uis.total_income, 0)           AS total_income,
    COALESCE(ues.total_expenses, 0)         AS total_expenses,
    COALESCE(uis.total_income, 0)
        - COALESCE(ues.total_expenses, 0)   AS balance
FROM users u
         LEFT JOIN user_income_summary  uis ON u.id = uis.id
         LEFT JOIN user_expense_summary ues ON u.id = ues.id;
ALTER TABLE incomes
    ADD CONSTRAINT check_income_amount_positive
        CHECK (amount > 0);

-- Расход > 0
ALTER TABLE expenses
    ADD CONSTRAINT check_expense_amount_positive
        CHECK (amount > 0);

COMMENT ON CONSTRAINT check_income_amount_positive ON incomes
    IS 'Доход не может быть отрицательным или нулевым';

COMMENT ON CONSTRAINT check_expense_amount_positive ON expenses
    IS 'Расход не может быть отрицательным или нулевым';

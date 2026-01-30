CREATE INDEX IF NOT EXISTS idx_incomes_user_date ON incomes (user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_expenses_user_date ON expenses (user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_incomes_source ON incomes (source);
CREATE INDEX IF NOT EXISTS idx_expenses_created_at ON expenses (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_incomes_created_at ON incomes (created_at DESC);
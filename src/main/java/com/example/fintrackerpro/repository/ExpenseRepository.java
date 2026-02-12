package com.example.fintrackerpro.repository;

import com.example.fintrackerpro.entity.expense.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByUserId(Long userId, Pageable pageable);

    List<Expense> findByUserId(Long userId);

    Optional<Expense> findById(Long id);

    // Получить сумму расходов за месяц
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.user.id = :userId " +
            "AND YEAR(e.date) = :year " +
            "AND MONTH(e.date) = :month")
    BigDecimal getTotalExpenseByUserAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );

    // Получить расходы за период
    @Query("SELECT e FROM Expense e " +
            "WHERE e.user.id = :userId " +
            "AND YEAR(e.date) = :year " +
            "AND MONTH(e.date) = :month " +
            "ORDER BY e.date DESC")
    Page<Expense> findByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    @Query(value = """
    SELECT DISTINCT 
        EXTRACT(YEAR FROM date) as year,
        EXTRACT(MONTH FROM date) as month
    FROM expenses
    WHERE user_id = :userId
    ORDER BY year DESC, month DESC
""", nativeQuery = true)
    List<Object[]> findUsedMonthsByUser(@Param("userId") Long userId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);
}


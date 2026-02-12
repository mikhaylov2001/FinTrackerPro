package com.example.fintrackerpro.repository;

import com.example.fintrackerpro.entity.income.Income;
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
public interface IncomeRepository extends JpaRepository<Income, Long> {

    Optional<Income> findByIdAndUserId(Long id, Long userId);

    Page<Income> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);


    Optional<Income> findById(Long id);

    // Получить сумму доходов за месяц
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i " +
            "WHERE i.user.id = :userId " +
            "AND YEAR(i.date) = :year " +
            "AND MONTH(i.date) = :month")
    BigDecimal getTotalIncomeByUserAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );

    // Получить доходы за период
    @Query("SELECT i FROM Income i " +
            "WHERE i.user.id = :userId " +
            "AND YEAR(i.date) = :year " +
            "AND MONTH(i.date) = :month " +
            "ORDER BY i.date DESC")
    Page<Income> findByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    @Query(value = """
    SELECT DISTINCT 
        EXTRACT(YEAR FROM date) as year,
        EXTRACT(MONTH FROM date) as month
    FROM incomes
    WHERE user_id = :userId
    ORDER BY year DESC, month DESC
""", nativeQuery = true)
    List<Object[]> findUsedMonthsByUser(@Param("userId") Long userId);



}

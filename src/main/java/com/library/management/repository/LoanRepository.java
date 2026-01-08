package com.library.management.repository;

import com.library.management.entity.Loan;
import com.library.management.entity.LoanStatus;
import com.library.management.model.CategoryReport;
import com.library.management.model.MemberActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByStatus(LoanStatus status);

    Page<Loan> findByMemberId(Long memberId, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.loanDate < :date")
    List<Loan> findOverdueLoans(@Param("date") LocalDateTime date);

    @Query("SELECT l FROM Loan l WHERE l.loanDate BETWEEN :startDate AND :endDate")
    List<Loan> findLoansInDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByMemberIdAndBookCopyBookId(Long memberId, Long bookId);

    @Query("SELECT new com.library.management.model.CategoryReport(c.id, c.name, COUNT(l)) " +
            "FROM Loan l JOIN l.bookCopy bc JOIN bc.book b JOIN b.categories c " +
            "GROUP BY c.id, c.name ORDER BY COUNT(l) DESC")
    List<CategoryReport> findMostReadCategories(Pageable pageable);

    @Query("SELECT new com.library.management.model.MemberActivity(m.id, CONCAT(m.firstName, ' ', m.lastName), m.email, COUNT(l)) " +
            "FROM Loan l JOIN l.member m " +
            "GROUP BY m.id, m.firstName, m.lastName, m.email ORDER BY COUNT(l) DESC")
    List<MemberActivity> findMostActiveMembers(Pageable pageable);
}

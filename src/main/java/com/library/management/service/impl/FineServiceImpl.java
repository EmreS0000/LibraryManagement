package com.library.management.service.impl;

import com.library.management.entity.*;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FineServiceImpl implements FineService {

    private final FineRepository fineRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("1.00");

    @Override
    public void calculateOverdueFines() {
        List<Loan> activeLoans = loanRepository.findByStatus(LoanStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        for (Loan loan : activeLoans) {
            if (now.isAfter(loan.getDueDate())) {
                createOrUpdateFine(loan);
            }
        }
    }

    @Override
    public void createOrUpdateFine(Loan loan) {
        LocalDateTime now = LocalDateTime.now();
        // If returned, calculate until returnDate, else until now
        LocalDateTime endDate = (loan.getReturnDate() != null) ? loan.getReturnDate() : now;

        long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), endDate);

        if (overdueDays > 0) {
            BigDecimal fineAmount = FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));

            Fine fine = fineRepository.findByLoanId(loan.getId())
                    .orElse(new Fine());

            if (fine.getId() == null) {
                fine.setLoan(loan);
                fine.setMember(loan.getMember());
                fine.setStatus(FineStatus.UNPAID);
                fine.setFineDate(now);
            }

            // Should verify if fine is already PAID? If paid, maybe we shouldn't update
            // amount?
            // Requirement says "Member balance tracking". Typically if paid, it's settled.
            // But if book is still out and fine was paid partially? Assuming full payment
            // clears it.
            // If status is UNPAID, we update the amount.
            if (fine.getStatus() == FineStatus.UNPAID) {
                // Update Member Balance
                // Logic: Remove old fine amount from balance, add new fine amount.
                BigDecimal oldAmount = (fine.getAmount() != null) ? fine.getAmount() : BigDecimal.ZERO;
                Member member = loan.getMember();

                member.setBalance(member.getBalance().subtract(oldAmount).add(fineAmount));
                memberRepository.save(member);

                fine.setAmount(fineAmount);
                fine.setLastUpdated(now);
                fineRepository.save(fine);
            }
        }
    }

    @Override
    public void payFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new BusinessException("Fine is already paid");
        }

        Member member = fine.getMember();
        member.setBalance(member.getBalance().subtract(fine.getAmount()));
        memberRepository.save(member);

        fine.setStatus(FineStatus.PAID);
        fine.setLastUpdated(LocalDateTime.now());
        fineRepository.save(fine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fine> getFinesByMember(Long memberId) {
        return fineRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }
}

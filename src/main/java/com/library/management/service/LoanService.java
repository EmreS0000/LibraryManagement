package com.library.management.service;

import com.library.management.entity.Loan;
import java.util.List;

public interface LoanService {
    Loan borrowBook(Loan loan);

    Loan returnBook(Long loanId);

    List<Loan> getAllLoans();

    List<Loan> getLoansByMember(Long memberId);
}

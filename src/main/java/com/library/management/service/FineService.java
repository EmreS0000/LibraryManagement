package com.library.management.service;

import com.library.management.entity.Fine;
import com.library.management.entity.Loan;
import java.util.List;

public interface FineService {
    void calculateOverdueFines();

    void createOrUpdateFine(Loan loan);

    void payFine(Long fineId);

    List<Fine> getFinesByMember(Long memberId);

    List<Fine> getAllFines();
}

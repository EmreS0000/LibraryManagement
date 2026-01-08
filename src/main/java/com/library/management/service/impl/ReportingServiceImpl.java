package com.library.management.service.impl;

import com.library.management.model.BookStatusReport;
import com.library.management.model.CategoryReport;
import com.library.management.model.MemberActivity;
import com.library.management.repository.BookCopyRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingServiceImpl implements ReportingService {

    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;

    @Override
    public List<CategoryReport> getMostReadCategories(int limit) {
        return loanRepository.findMostReadCategories(PageRequest.of(0, limit));
    }

    @Override
    public List<MemberActivity> getMostActiveMembers(int limit) {
        return loanRepository.findMostActiveMembers(PageRequest.of(0, limit));
    }

    @Override
    public List<BookStatusReport> getBookStatusDistribution() {
        return bookCopyRepository.countByStatusGrouped();
    }
}

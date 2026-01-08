package com.library.management.service;

import com.library.management.model.BookStatusReport;
import com.library.management.model.CategoryReport;
import com.library.management.model.MemberActivity;

import java.util.List;

public interface ReportingService {
    List<CategoryReport> getMostReadCategories(int limit);

    List<MemberActivity> getMostActiveMembers(int limit);

    List<BookStatusReport> getBookStatusDistribution();
}

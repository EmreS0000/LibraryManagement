package com.library.management.service;

import com.library.management.model.BookStatusReport;
import com.library.management.model.CategoryReport;
import com.library.management.model.MemberActivity;
import com.library.management.repository.BookCopyRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.service.impl.ReportingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportingService Unit Tests")
class ReportingServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @InjectMocks
    private ReportingServiceImpl reportingService;

    private CategoryReport testCategoryReport;
    private MemberActivity testMemberActivity;
    private BookStatusReport testBookStatusReport;

    @BeforeEach
    void setUp() {
        testCategoryReport = new CategoryReport() {
            @Override
            public String getCategoryName() {
                return "Fiction";
            }

            @Override
            public Long getLoanCount() {
                return 150L;
            }
        };

        testMemberActivity = new MemberActivity() {
            @Override
            public Long getMemberId() {
                return 1L;
            }

            @Override
            public String getMemberName() {
                return "John Doe";
            }

            @Override
            public Long getLoanCount() {
                return 25L;
            }
        };

        testBookStatusReport = new BookStatusReport() {
            @Override
            public String getStatus() {
                return "AVAILABLE";
            }

            @Override
            public Long getCount() {
                return 100L;
            }
        };
    }

    @Test
    @DisplayName("Should get most read categories successfully")
    void testGetMostReadCategories_Success() {
        // Given
        CategoryReport report2 = new CategoryReport() {
            @Override
            public String getCategoryName() {
                return "Science";
            }

            @Override
            public Long getLoanCount() {
                return 120L;
            }
        };

        List<CategoryReport> reports = Arrays.asList(testCategoryReport, report2);
        when(loanRepository.findMostReadCategories(any(PageRequest.class))).thenReturn(reports);

        // When
        List<CategoryReport> result = reportingService.getMostReadCategories(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryName()).isEqualTo("Fiction");
        assertThat(result.get(0).getLoanCount()).isEqualTo(150L);
        verify(loanRepository, times(1)).findMostReadCategories(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should get most read categories with custom limit")
    void testGetMostReadCategories_CustomLimit() {
        // Given
        List<CategoryReport> reports = Arrays.asList(testCategoryReport);
        when(loanRepository.findMostReadCategories(any(PageRequest.class))).thenReturn(reports);

        // When
        List<CategoryReport> result = reportingService.getMostReadCategories(5);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(loanRepository, times(1)).findMostReadCategories(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("Should return empty list when no categories have loans")
    void testGetMostReadCategories_EmptyList() {
        // Given
        when(loanRepository.findMostReadCategories(any(PageRequest.class))).thenReturn(Arrays.asList());

        // When
        List<CategoryReport> result = reportingService.getMostReadCategories(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get most active members successfully")
    void testGetMostActiveMembers_Success() {
        // Given
        MemberActivity activity2 = new MemberActivity() {
            @Override
            public Long getMemberId() {
                return 2L;
            }

            @Override
            public String getMemberName() {
                return "Jane Smith";
            }

            @Override
            public Long getLoanCount() {
                return 20L;
            }
        };

        List<MemberActivity> activities = Arrays.asList(testMemberActivity, activity2);
        when(loanRepository.findMostActiveMembers(any(PageRequest.class))).thenReturn(activities);

        // When
        List<MemberActivity> result = reportingService.getMostActiveMembers(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMemberName()).isEqualTo("John Doe");
        assertThat(result.get(0).getLoanCount()).isEqualTo(25L);
        verify(loanRepository, times(1)).findMostActiveMembers(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should get most active members with custom limit")
    void testGetMostActiveMembers_CustomLimit() {
        // Given
        List<MemberActivity> activities = Arrays.asList(testMemberActivity);
        when(loanRepository.findMostActiveMembers(any(PageRequest.class))).thenReturn(activities);

        // When
        List<MemberActivity> result = reportingService.getMostActiveMembers(3);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(loanRepository, times(1)).findMostActiveMembers(PageRequest.of(0, 3));
    }

    @Test
    @DisplayName("Should return empty list when no members have loans")
    void testGetMostActiveMembers_EmptyList() {
        // Given
        when(loanRepository.findMostActiveMembers(any(PageRequest.class))).thenReturn(Arrays.asList());

        // When
        List<MemberActivity> result = reportingService.getMostActiveMembers(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get book status distribution successfully")
    void testGetBookStatusDistribution_Success() {
        // Given
        BookStatusReport report2 = new BookStatusReport() {
            @Override
            public String getStatus() {
                return "LOANED";
            }

            @Override
            public Long getCount() {
                return 50L;
            }
        };

        BookStatusReport report3 = new BookStatusReport() {
            @Override
            public String getStatus() {
                return "RESERVED";
            }

            @Override
            public Long getCount() {
                return 10L;
            }
        };

        List<BookStatusReport> reports = Arrays.asList(testBookStatusReport, report2, report3);
        when(bookCopyRepository.countByStatusGrouped()).thenReturn(reports);

        // When
        List<BookStatusReport> result = reportingService.getBookStatusDistribution();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.get(0).getCount()).isEqualTo(100L);
        assertThat(result.get(1).getStatus()).isEqualTo("LOANED");
        assertThat(result.get(1).getCount()).isEqualTo(50L);
        verify(bookCopyRepository, times(1)).countByStatusGrouped();
    }

    @Test
    @DisplayName("Should return empty list when no book copies exist")
    void testGetBookStatusDistribution_EmptyList() {
        // Given
        when(bookCopyRepository.countByStatusGrouped()).thenReturn(Arrays.asList());

        // When
        List<BookStatusReport> result = reportingService.getBookStatusDistribution();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle single status in distribution")
    void testGetBookStatusDistribution_SingleStatus() {
        // Given
        List<BookStatusReport> reports = Arrays.asList(testBookStatusReport);
        when(bookCopyRepository.countByStatusGrouped()).thenReturn(reports);

        // When
        List<BookStatusReport> result = reportingService.getBookStatusDistribution();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("Should handle large numbers in category reports")
    void testGetMostReadCategories_LargeNumbers() {
        // Given
        CategoryReport report = new CategoryReport() {
            @Override
            public String getCategoryName() {
                return "Popular Category";
            }

            @Override
            public Long getLoanCount() {
                return 10000L;
            }
        };

        List<CategoryReport> reports = Arrays.asList(report);
        when(loanRepository.findMostReadCategories(any(PageRequest.class))).thenReturn(reports);

        // When
        List<CategoryReport> result = reportingService.getMostReadCategories(1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get(0).getLoanCount()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("Should handle zero loan counts")
    void testGetMostActiveMembers_ZeroLoans() {
        // Given
        MemberActivity activity = new MemberActivity() {
            @Override
            public Long getMemberId() {
                return 1L;
            }

            @Override
            public String getMemberName() {
                return "Inactive Member";
            }

            @Override
            public Long getLoanCount() {
                return 0L;
            }
        };

        List<MemberActivity> activities = Arrays.asList(activity);
        when(loanRepository.findMostActiveMembers(any(PageRequest.class))).thenReturn(activities);

        // When
        List<MemberActivity> result = reportingService.getMostActiveMembers(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoanCount()).isEqualTo(0L);
    }
}

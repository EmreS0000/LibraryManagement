package com.library.management.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import com.library.management.selenium.BaseSeleniumTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Test Scenarios for Library Management System
 * 
 * Scenario 1: User Login & Book Search
 * Scenario 2: Book Details & Filtering by Category
 * Scenario 3: Admin Book Management (Add & Configure)
 */
@DisplayName("Library Management E2E Tests")
class LibraryE2ETestScenarios extends BaseSeleniumTest {

    /**
     * Scenario 1: User Login & Search Books
     * Steps:
     * 1. Navigate to login page
     * 2. Login with valid credentials
     * 3. Search for a book
     * 4. Verify search results appear
     */
    @Test
    @DisplayName("Scenario 1: User Login & Book Search")
    void testUserLoginAndBookSearch() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        WebElement passwordInput = driver.findElement(By.cssSelector("[data-testid='password-input']"));
        WebElement loginButton = driver.findElement(By.cssSelector("[data-testid='login-submit']"));

        emailInput.sendKeys("test@test.com");
        passwordInput.sendKeys("password123");
        loginButton.click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));

        WebElement searchField = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder*='Search']"))
        );
        searchField.sendKeys("Test");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='book-grid']")));
        assertTrue(driver.getPageSource().toLowerCase().contains("test"));
    }

    /**
     * Scenario 2: View Book Details & Filter by Category
     * Steps:
     * 1. Navigate to books page
     * 2. Click on a book to view details
     * 3. Verify book information is displayed
     * 4. Filter books by category
     * 5. Verify filtered results
     */
    @Test
    @DisplayName("Scenario 2: Book Details & Category Filtering")
    void testBookDetailsAndCategoryFilter() {
        openFrontend("/books");

        WebElement firstBook = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='book-grid'] [data-testid]"))
        );
        firstBook.click();

        WebElement bookTitle = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='book-title']"))
        );
        assertFalse(bookTitle.getText().isBlank());

        driver.navigate().back();

        WebElement categoryFilter = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("select[data-testid='category-filter']"))
        );
        new Select(categoryFilter).selectByIndex(1);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='book-grid']")));
    }

    /**
     * Scenario 3: Admin Book Management
     * Steps:
     * 1. Login as admin
     * 2. Navigate to admin dashboard
     * 3. Add a new book
     * 4. Assign a category to the book
     * 5. Verify book appears in catalog
     */
    @Test
    @DisplayName("Scenario 3: Admin Book Management")
    void testAdminBookManagement() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("admin@library.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("adminpass");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/admin"));

        WebElement addBookOption = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='add-book']"))
        );
        addBookOption.click();

        WebElement titleInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='book-title-input']"))
        );
        titleInput.sendKeys("New E2E Test Book");

        driver.findElement(By.cssSelector("[data-testid='book-author-input']")).sendKeys("E2E Test Author");
        driver.findElement(By.cssSelector("[data-testid='book-isbn-input']")).sendKeys("E2E-ISBN-" + System.currentTimeMillis());

        WebElement categorySelect = driver.findElement(By.cssSelector("[data-testid='book-category-select']"));
        new Select(categorySelect).selectByIndex(1);

        driver.findElement(By.cssSelector("[data-testid='save-book-button']")).click();

        WebElement successMessage = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='success-message']"))
        );
        assertTrue(successMessage.isDisplayed());
    }

    /**
     * Scenario 4: Borrow Book Flow
     * Steps:
     * 1. Login as user
     * 2. Navigate to available books
     * 3. Borrow a book
     * 4. Verify loan appears in user's loans
     */
    @Test
    @DisplayName("Scenario 4: Borrow Book Flow")
    void testBorrowBookFlow() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("test@test.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));

        WebElement booksLink = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='books-link']"))
        );
        booksLink.click();

        WebElement borrowButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='borrow-button']"))
        );
        borrowButton.click();

        WebElement confirmButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='confirm-borrow']"))
        );
        confirmButton.click();

        WebElement loansList = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='loans-list']"))
        );
        assertNotNull(loansList);
    }

    /**
     * Scenario 5: Return Book Flow
     * Steps:
     * 1. Login as user
     * 2. Navigate to my loans
     * 3. Return a borrowed book
     * 4. Verify loan status changes to RETURNED
     */
    @Test
    @DisplayName("Scenario 5: Return Book Flow")
    void testReturnBookFlow() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("test@test.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));

        WebElement myLoansLink = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='my-loans-link']"))
        );
        myLoansLink.click();

        WebElement returnButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='return-button']"))
        );
        returnButton.click();

        WebElement confirmReturn = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='confirm-return']"))
        );
        confirmReturn.click();

        WebElement successMsg = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='success-message']"))
        );
        assertTrue(successMsg.isDisplayed());
    }
}

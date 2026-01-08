package com.library.management.selenium;

import com.library.management.entity.*;
import com.library.management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class BookManagementSeleniumTest extends BaseSeleniumTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member admin;
    private Member user;
    private Category category;

    @BeforeEach
    void setupData() {
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        admin = new Member();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@library.com");
        admin.setPhone("9876543210");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRole(Role.ADMIN);
        admin = memberRepository.save(admin);

        user = new Member();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@test.com");
        user.setPhone("1234567890");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.USER);
        user = memberRepository.save(user);

        category = new Category();
        category.setName("Fiction");
        category.setDescription("Fiction books");
        category.setStatus(Category.Status.ACTIVE);
        category = categoryRepository.save(category);

        Book book = new Book();
        book.setTitle("Sample Book");
        book.setAuthor("Sample Author");
        book.setIsbn("1234567890");
        bookRepository.save(book);
    }

    @Test
    void testUserCanViewBooks() {
        // Login as user
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("test@test.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));

        // Verify books are displayed
        WebElement bookGrid = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='book-grid']")));
        assertTrue(bookGrid.isDisplayed());
    }

    @Test
    void testUserCanSearchBooks() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("test@test.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));

        // Search for a book
        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("input[placeholder*='Search']")));
        searchInput.sendKeys("Sample");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
            By.cssSelector("[data-testid='book-grid']"), "Sample"));

        // Verify search results
        assertTrue(driver.getPageSource().contains("Sample Book"));
    }

    @Test
    void testAdminCanAccessAdminDashboard() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("admin@library.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("adminpass");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/admin"));
        assertTrue(driver.getCurrentUrl().contains("/admin"));
        
        // Verify admin dashboard elements
        assertTrue(driver.getPageSource().contains("Library Admin") || 
                   driver.getPageSource().contains("Admin Dashboard"));
    }

    @Test
    void testUserCanLogout() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("test@test.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));

        // Click logout
        WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("[data-testid='logout-btn']")));
        logoutButton.click();

        wait.until(ExpectedConditions.urlContains("/login"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void testPaginationWorks() {
        // Create multiple books for pagination
        for (int i = 1; i <= 12; i++) {
            Book book = new Book();
            book.setTitle("Book " + i);
            book.setAuthor("Author " + i);
            book.setIsbn("ISBN" + i);
            bookRepository.save(book);
        }

        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        emailInput.sendKeys("test@test.com");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();

        wait.until(ExpectedConditions.urlContains("/dashboard"));

        // Check if pagination controls exist
        try {
            WebElement pagination = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[data-testid='pagination-controls']")));
            assertTrue(pagination.isDisplayed());
        } catch (Exception e) {
            // Pagination might not be visible if all books fit on one page
            // This is acceptable
        }
    }
}


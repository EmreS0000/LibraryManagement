package com.library.management.selenium;

import com.library.management.entity.Member;
import com.library.management.entity.Role;
import com.library.management.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class LoginSeleniumTest extends BaseSeleniumTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setupData() {
        memberRepository.deleteAll();
        
        Member member = new Member();
        member.setFirstName("Test");
        member.setLastName("User");
        member.setEmail("test@test.com");
        member.setPhone("1234567890");
        member.setPassword(passwordEncoder.encode("password123"));
        member.setRole(Role.USER);
        memberRepository.save(member);

        Member admin = new Member();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@library.com");
        admin.setPhone("9876543210");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRole(Role.ADMIN);
        memberRepository.save(admin);
    }

    @Test
    void testSuccessfulLogin() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        WebElement passwordInput = driver.findElement(By.cssSelector("[data-testid='password-input']"));
        WebElement loginButton = driver.findElement(By.cssSelector("[data-testid='login-submit']"));

        emailInput.sendKeys("test@test.com");
        passwordInput.sendKeys("password123");
        loginButton.click();

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/dashboard"),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='logout-btn']"))
        ));
        assertTrue(driver.getCurrentUrl().contains("/dashboard")
            || driver.getPageSource().contains("Active Loans"));
    }

    @Test
    void testFailedLoginInvalidCredentials() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        WebElement passwordInput = driver.findElement(By.cssSelector("[data-testid='password-input']"));
        WebElement loginButton = driver.findElement(By.cssSelector("[data-testid='login-submit']"));

        emailInput.sendKeys("test@test.com");
        passwordInput.sendKeys("wrongpassword");
        loginButton.click();

        wait.until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='login-error']")),
            ExpectedConditions.urlContains("/login")
        ));

        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    void testAdminLogin() {
        openFrontend("/login");

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='email-input']")));
        WebElement passwordInput = driver.findElement(By.cssSelector("[data-testid='password-input']"));
        WebElement loginButton = driver.findElement(By.cssSelector("[data-testid='login-submit']"));

        emailInput.sendKeys("admin@library.com");
        passwordInput.sendKeys("adminpass");
        loginButton.click();

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/admin"),
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='logout-btn']"))
        ));
        assertTrue(driver.getCurrentUrl().contains("/admin") || driver.getPageSource().toLowerCase().contains("admin"));
    }

    @Test
    void testNavigateToRegister() {
        openFrontend("/login");

        WebElement registerLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("[data-testid='register-link']")));
        registerLink.click();

        wait.until(ExpectedConditions.urlContains("/register"));
        assertTrue(driver.getCurrentUrl().contains("/register"));
    }
}


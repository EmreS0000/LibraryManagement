package com.library.management.selenium;

import com.library.management.entity.MembershipType;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.MembershipTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class RegisterSeleniumTest extends BaseSeleniumTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @BeforeEach
    void setupData() {
        memberRepository.deleteAll();

        if (membershipTypeRepository.count() == 0) {
            MembershipType type = new MembershipType();
            type.setName("Standard");
            type.setMaxBooks(5);
            type.setMaxLoanDays(14);
            membershipTypeRepository.save(type);
        }
    }

    @Test
    void testSuccessfulRegistration() {
        openFrontend("/register");

        WebElement firstNameInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='firstname-input']")));
        WebElement lastNameInput = driver.findElement(By.cssSelector("[data-testid='lastname-input']"));
        WebElement emailInput = driver.findElement(By.cssSelector("[data-testid='email-input']"));
        WebElement phoneInput = driver.findElement(By.cssSelector("[data-testid='phone-input']"));
        WebElement passwordInput = driver.findElement(By.cssSelector("[data-testid='password-input']"));
        WebElement confirmPasswordInput = driver.findElement(By.cssSelector("input[name='confirmPassword']"));
        WebElement registerButton = driver.findElement(By.cssSelector("[data-testid='register-button']"));

        firstNameInput.sendKeys("John");
        lastNameInput.sendKeys("Doe");
        emailInput.sendKeys("john.doe@test.com");
        phoneInput.sendKeys("1234567890");
        passwordInput.sendKeys("password123");
        confirmPasswordInput.sendKeys("password123");

        registerButton.click();

        wait.until(d -> memberRepository.findByEmail("john.doe@test.com").isPresent());

        assertTrue(driver.getCurrentUrl().contains("/login")
                || driver.getPageSource().contains("Registration Successful")
                || driver.getPageSource().contains("Redirecting to login"));
    }

    @Test
    void testRegistrationValidationErrors() {
        openFrontend("/register");

        WebElement registerButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='register-button']")));

        registerButton.click();
        wait.until(ExpectedConditions.urlContains("/register"));
        assertTrue(driver.getCurrentUrl().contains("/register"));
    }

    @Test
    void testDuplicateEmailRegistration() {
        openFrontend("/register");

        WebElement firstNameInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='firstname-input']")));
        firstNameInput.sendKeys("John");
        driver.findElement(By.cssSelector("[data-testid='lastname-input']")).sendKeys("Doe");
        driver.findElement(By.cssSelector("[data-testid='email-input']")).sendKeys("duplicate@test.com");
        driver.findElement(By.cssSelector("[data-testid='phone-input']")).sendKeys("1234567890");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("input[name='confirmPassword']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='register-button']")).click();

        wait.until(d -> memberRepository.findByEmail("duplicate@test.com").isPresent());

        openFrontend("/register");

        firstNameInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='firstname-input']")));
        firstNameInput.sendKeys("Jane");
        driver.findElement(By.cssSelector("[data-testid='lastname-input']")).sendKeys("Smith");
        driver.findElement(By.cssSelector("[data-testid='email-input']")).sendKeys("duplicate@test.com");
        driver.findElement(By.cssSelector("[data-testid='phone-input']")).sendKeys("9876543210");
        driver.findElement(By.cssSelector("[data-testid='password-input']")).sendKeys("password123");
        driver.findElement(By.cssSelector("input[name='confirmPassword']")).sendKeys("password123");
        driver.findElement(By.cssSelector("[data-testid='register-button']")).click();

        WebElement errorAlert = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='register-error']")));
        assertTrue(errorAlert.isDisplayed());
    }
}


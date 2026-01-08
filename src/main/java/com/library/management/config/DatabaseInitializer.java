package com.library.management.config;

import com.library.management.entity.MembershipType;
import com.library.management.repository.MembershipTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
public class DatabaseInitializer implements CommandLineRunner {

    private final MembershipTypeRepository membershipTypeRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Initialize default membership types if they don't exist
        createMembershipTypeIfNotExists("Standard", 5, 14);
        createMembershipTypeIfNotExists("Premium", 10, 21);
        createMembershipTypeIfNotExists("VIP", 20, 30);
        System.out.println("✓ Default membership types checked/initialized successfully");
    }
    
    private void createMembershipTypeIfNotExists(String name, int maxBooks, int maxLoanDays) {
        if (!membershipTypeRepository.existsByName(name)) {
            MembershipType type = new MembershipType();
            type.setName(name);
            type.setMaxBooks(maxBooks);
            type.setMaxLoanDays(maxLoanDays);
            membershipTypeRepository.save(type);
        }
    }
}

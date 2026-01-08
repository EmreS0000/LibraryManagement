package com.library.management.job;

import com.library.management.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FineScheduler {

    private final FineService fineService;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void calculateOverdueFines() {
        fineService.calculateOverdueFines();
    }
}

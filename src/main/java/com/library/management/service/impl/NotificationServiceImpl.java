package com.library.management.service.impl;

import com.library.management.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifyReservationReady(String email, String bookTitle) {
        log.info("Sending notification to {}: Your reservation for '{}' is ready for pickup.", email, bookTitle);
        // In a real system, this would send an email.
    }
}

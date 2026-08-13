package com.enterprise.order.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates unique order numbers in the format: ORD-YYYYMMDD-XXXXX
 * Example: ORD-20260813-00142
 *
 * In a real system, this could use a database sequence or distributed ID generator.
 * For this project, a timestamp + random approach is sufficient and demonstrates
 * the concept without adding infrastructure complexity.
 */
@Component
public class OrderNumberGenerator {

    private static final String PREFIX = "ORD";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String date = LocalDateTime.now().format(DATE_FORMAT);
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return String.format("%s-%s-%05d", PREFIX, date, random % 100000);
    }
}

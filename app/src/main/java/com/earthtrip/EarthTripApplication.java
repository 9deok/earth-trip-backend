package com.earthtrip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EarthTripApplication {

    public static void main(String[] args) {
        SpringApplication.run(EarthTripApplication.class, args);
    }
}

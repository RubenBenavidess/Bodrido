package com.espe.edu.ec.billing_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@Configuration
@EnableJpaAuditing
@EnableFeignClients
public class BillingMsApplication {

        public static void main(String[] args) {
                SpringApplication.run(BillingMsApplication.class, args);
        }

}
package com.espe.edu.ec.billing_ms.event_producers;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "order-ms", url = "${ORDER_SERVICE_URL:http://order-service:8080}")
public interface OrderEventProducer {

    @PostMapping("/orders/exists/{id}")
    boolean orderExists(@PathVariable UUID id);

}

package com.espe.edu.ec.order_ms.event_producers;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "driver-ms", url = "http://localhost:5173") // GATEWAY API
public interface DriverEventProducer {

    @PostMapping("/drivers/cancel-trip")
    boolean updateStatus(@RequestBody UUID driverId); 

}

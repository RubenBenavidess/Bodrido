package com.espe.edu.ec.order_ms.dtos.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderNotificationEvent {

    private UUID id;
    private final String microservice = "order-ms";
    private String action;

    private UUID orderId;
    private final String entityType = "ORDER";
    
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private Map<String, Object> data;
    private String severity;

    public String getTimeStamp(){
        return timestamp != null ? 
        timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : 
        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }



}


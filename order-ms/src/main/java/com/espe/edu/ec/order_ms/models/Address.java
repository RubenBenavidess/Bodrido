package com.espe.edu.ec.order_ms.models;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable{

    private String street;
    private String city;
    private Coordinates coordinates;
    private String instructions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinates implements Serializable {
        private Double longitude;
        private Double latitude;
    }

}

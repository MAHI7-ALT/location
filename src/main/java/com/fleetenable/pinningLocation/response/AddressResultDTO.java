package com.fleetenable.pinningLocation.response;

import lombok.Data;

@Data
public class AddressResultDTO {
    private String address;
    private GeoCode geoCode;
    private GeoCode geoCode2; // Assuming similar structure to GeoCode
    private Melisa melisa;
}
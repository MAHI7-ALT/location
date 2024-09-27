package com.fleetenable.pinningLocation.response;

import lombok.Data;

@Data
public class GeoCode {
    private String addressString;
    private Float[] latLng;
    private boolean partialMatch;
}

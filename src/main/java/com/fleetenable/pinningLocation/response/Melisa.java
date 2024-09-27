package com.fleetenable.pinningLocation.response;

import lombok.Data;

@Data
public class Melisa {
    private String addressString;
    private Float[] latLag;
    private boolean partialMatch;
}
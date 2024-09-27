package com.fleetenable.pinningLocation.request;

import lombok.Data;

import java.util.List;

@Data
public class AddressRequest {
    private List<String> addresses;
}

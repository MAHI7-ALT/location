package com.fleetenable.pinningLocation.request;

import lombok.Data;

@Data
public class Address {

    private String add1;

    private String add2;

    private String city;

    private String state;

    private String country;

    private String zip;
}

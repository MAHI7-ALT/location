package com.fleetenable.pinningLocation.response;

import lombok.Data;

import java.util.List;
@Data
public class AddressResponseDTO {
    private List<AddressResultDTO> result;

}
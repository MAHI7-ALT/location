package com.fleetenable.pinningLocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @PostMapping("/format")
    public ResponseEntity<CombinedResponseDTO> formatAndVerifyAddress(@RequestBody String address) {
        String cleanAddress = cleanAddress(address);

        if (!isLengthValid(cleanAddress)) {
            return ResponseEntity.badRequest().body(null);
        }

        CombinedResponseDTO combinedResponseDTO = addressService.formatCombinedResponse(cleanAddress);
        return ResponseEntity.ok(combinedResponseDTO);
    }

    private String cleanAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.replaceAll("\\s+", " ").trim();
    }

    private boolean isLengthValid(String address) {
        String[] parts = address.split(",");
        return parts.length == 5 || parts.length == 6;
    }
}

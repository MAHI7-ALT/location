package com.fleetenable.pinningLocation;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetenable.pinningLocation.request.AddressRequest;
import com.fleetenable.pinningLocation.response.AddressResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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


    @PostMapping("/getLatLon")
    public AddressResponseDTO handleAddresses(@RequestBody AddressRequest addressRequest) {
        // You can access the addresses here
        return addressService.processAddress(addressRequest);
    }


    @PostMapping("/upload-addresses")
    public AddressResponseDTO uploadAddresses(@RequestParam("file") MultipartFile file) {
        try {
            // Check if the file is empty
            if (file.isEmpty()) {
                System.out.println("file must not be null");
            }

            // Parse the JSON file using ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            AddressRequest addressData = objectMapper.readValue(file.getInputStream(), AddressRequest.class);

            // Process the addresses
            return addressService.processAddress(addressData);
        } catch (IOException e) {
            // Handle exception if parsing fails
            e.printStackTrace();
        }
        return null;
    }

}

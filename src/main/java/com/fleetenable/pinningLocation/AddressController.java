package com.fleetenable.pinningLocation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    private RestTemplate restTemplate;

    @PostMapping("/format")
    public ResponseEntity<List<CombinedResponseDTO>> formatAndVerifyAddresses(@RequestBody List<String> addresses) {
        List<CombinedResponseDTO> responseList = new ArrayList<>();

        for (String address : addresses) {
            String cleanAddress = addressService.cleanAddress(address);

            // Make API calls
            String geoMapUrl = "https://maps.googleapis.com/maps/api/geocode/json?key=xxxxxxxxxxxxxxxxxxxxxx&address=" + cleanAddress;
            String melisaUrl = "https://address.data.net/V3/WEB/GlobalAddress/doGlobalAddress?id=RsJPpXGR-TqvzQ1aVY-&ctry=USA&format=JSON&a1=" + cleanAddress;

            String geoMapResponse = restTemplate.getForObject(geoMapUrl, String.class);
            String melisaResponse = restTemplate.getForObject(melisaUrl, String.class);

            // Call the service to process address and responses
            CombinedResponseDTO combinedResponseDTO = addressService.processAddress(cleanAddress, geoMapResponse, melisaResponse);
            responseList.add(combinedResponseDTO);
        }

        return ResponseEntity.ok(responseList);
    }
}

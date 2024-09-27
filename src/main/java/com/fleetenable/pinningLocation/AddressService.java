
package com.fleetenable.pinningLocation;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AddressService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Main method that calls both GeoMap and Melisa APIs and performs format verification
    public CombinedResponseDTO formatCombinedResponse(String address) {
        CombinedResponseDTO combinedResponseDTO = new CombinedResponseDTO();
        combinedResponseDTO.setAddress(address); // Set the original address from the request body

        // Generate the four address formats from the request body
        List<String> formattedAddresses = formatAddress(address);

        // Check address format in GeoMap API response
        String geoMapUrl = "https://maps.googleapis.com/maps/api/geocode/json?key=xxxxxxxxxxxxxxxxxxxxxx&address=" + address;
        boolean isGeoMapValid = false;

        try {
            String geoResponse = restTemplate.getForObject(geoMapUrl, String.class);
            if (geoResponse != null) {
                String lat = extractValue(geoResponse, "\"lat\":");
                String lng = extractValue(geoResponse, "\"lng\":");

                // Verify if the formatted address matches the GeoMap response
                isGeoMapValid = verifyAddressFormat(formattedAddresses, geoResponse);

                // Populate GeoCodeDTO
                CombinedResponseDTO.GeoCodeDTO geoCodeDTO = new CombinedResponseDTO.GeoCodeDTO();
                if (lat != null && lng != null) {
                    geoCodeDTO.setLat(Double.parseDouble(lat));
                    geoCodeDTO.setLng(Double.parseDouble(lng));
                }
                combinedResponseDTO.setGeoCode(geoCodeDTO);
            }
        } catch (HttpClientErrorException e) {
            combinedResponseDTO.setStatus("Error fetching GeoMap data: " + e.getMessage());
        }

        // Check address format in Melisa API response
        String melisaUrl = "https://address.data.net/V3/WEB/GlobalAddress/doGlobalAddress?id=RsJPpXGR-TqvzQ1aVY-&ctry=USA&format=JSON&a1=" + address;
        boolean isMelisaValid = false;

        try {
            String melisaResponse = restTemplate.getForObject(melisaUrl, String.class);
            if (melisaResponse != null) {
                String melisaLat = extractValue(melisaResponse, "\"latitude\":\"");
                String melisaLng = extractValue(melisaResponse, "\"longitude\":\"");

                // Verify if the formatted address matches the Melisa response
                isMelisaValid = verifyAddressFormat(formattedAddresses, melisaResponse);

                // Populate MelisaDTO
                CombinedResponseDTO.MelisaDTO melisaDTO = new CombinedResponseDTO.MelisaDTO();
                if (melisaLat != null && melisaLng != null) {
                    melisaDTO.setLat(Double.parseDouble(melisaLat));
                    melisaDTO.setLng(Double.parseDouble(melisaLng));
                }
                combinedResponseDTO.setMelisa(melisaDTO);
            }
        } catch (HttpClientErrorException e) {
            combinedResponseDTO.setStatus("Error fetching Melisa data: " + e.getMessage());
        }

        // Set final verification status based on both APIs
        combinedResponseDTO.setStatus(isGeoMapValid && isMelisaValid ? "Valid Formats" : "Invalid Formats");

        return combinedResponseDTO;
    }

    // Helper method to extract values from response string
    private String extractValue(String response, String key) {
        int keyIndex = response.indexOf(key);
        if (keyIndex == -1) return null;

        int startIndex = keyIndex + key.length();
        int endIndex = response.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = response.indexOf("}", startIndex);
        }

        return response.substring(startIndex, endIndex).replace("\"", "").trim();
    }

    // Address format method
    private List<String> formatAddress(String address) {
        String[] parts = address.split(",");
        List<String> formatted = new ArrayList<>();

        if (parts.length == 5) {
            // Format 1: (addressLine1 city state country zip)
            formatted.add(parts[0].trim() + " " + parts[1].trim() + " " + parts[2].trim() + " " + parts[3].trim() + " " + parts[4].trim());
            // Format 3: (country, state zip, addressLine1, city)
            formatted.add(parts[3].trim() + ", " + parts[2].trim() + " " + parts[4].trim() + ", " + parts[0].trim() + ", " + parts[1].trim());
            // Format 4: (addressLine1, city, state zip, country)
            formatted.add(parts[0].trim() + ", " + parts[1].trim() + ", " + parts[2].trim() + " " + parts[4].trim() + ", " + parts[3].trim());
        } else if (parts.length == 6) {
            // Format 2: (addressLine1 addressLine2 city state country zip)
            formatted.add(parts[0].trim() + " " + parts[1].trim() + " " + parts[2].trim() + " " + parts[3].trim() + " " + parts[4].trim() + " " + parts[5].trim());
        }

        return formatted;
    }

    // Method to verify address format against API response
    private boolean verifyAddressFormat(List<String> formattedAddresses, String apiResponse) {
        for (String formattedAddress : formattedAddresses) {
            if (apiResponse.contains(formattedAddress)) {
                return true; // Address format matches API response
            }
        }
        return false; // No match found
    }
}

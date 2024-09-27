package com.fleetenable.pinningLocation;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AddressService {

    private final RestTemplate restTemplate; // For making API calls

    public AddressService(RestTemplate restTemplate) { // Constructor injection
        this.restTemplate = restTemplate;
    }

    // Method to handle full processing of the address
    public CombinedResponseDTO processAddress(String address, String geoMapResponse, String melisaResponse) {
        CombinedResponseDTO combinedResponseDTO = new CombinedResponseDTO();
        combinedResponseDTO.setAddress(address); // Set the original address

        // Generate formatted addresses
        List<String> formattedAddresses = formatAddress(address);

        // Check address format in GeoMap API response
        boolean isGeoMapValid = false;
        CombinedResponseDTO.GeoCodeDTO geoCodeDTO = null;

        if (geoMapResponse != null) {
            // First API response parsing
            JSONObject geoMapJson = new JSONObject(geoMapResponse);
            Double lat = geoMapJson.optDouble("lat", Double.NaN);
            Double lng = geoMapJson.optDouble("lng", Double.NaN);
            boolean partialMatch = geoMapJson.optBoolean("partialMatch", false);

            // Verify if the formatted address matches the GeoMap response
            isGeoMapValid = verifyAddressFormat(formattedAddresses, geoMapResponse);

            // Populate GeoCodeDTO from the first response
            geoCodeDTO = new CombinedResponseDTO.GeoCodeDTO();
            if (!lat.isNaN() && !lng.isNaN()) {
                geoCodeDTO.setLat(lat);
                geoCodeDTO.setLng(lng);
                geoCodeDTO.setPartialMatch(partialMatch);
            }
            combinedResponseDTO.setGeoCode(geoCodeDTO);
        }

        // Check if we need to hit the GeoCode API again
        if (geoCodeDTO != null && geoCodeDTO.getPartialMatch()) {
            // Call GeoCode API again
            String newGeoMapResponse = callGeoCodeApi(address);

            if (newGeoMapResponse != null) {
                JSONObject newGeoMapJson = new JSONObject(newGeoMapResponse);
                Double newLat = newGeoMapJson.optDouble("lat", Double.NaN);
                Double newLng = newGeoMapJson.optDouble("lng", Double.NaN);
                boolean newPartialMatch = newGeoMapJson.optBoolean("partialMatch", false);

                // Update geoCodeDTO with new coordinates only if they are valid
                if (!newLat.isNaN() && !newLng.isNaN()) {
                    geoCodeDTO.setLat(newLat);
                    geoCodeDTO.setLng(newLng);
                }

                // Update partialMatch based on the new response
                geoCodeDTO.setPartialMatch(newPartialMatch);
            }
        }

        // Check address format in Melisa API response
        boolean isMelisaValid = false;
        if (melisaResponse != null) {
            JSONObject melisaJson = new JSONObject(melisaResponse);
            Double melisaLat = melisaJson.optDouble("latitude", Double.NaN);
            Double melisaLng = melisaJson.optDouble("longitude", Double.NaN);
            String melisaResults = melisaJson.optString("results", "");

            // Verify if the formatted address matches the Melisa response
            isMelisaValid = verifyAddressFormat(formattedAddresses, melisaResponse);

            // If the Melisa API response doesn't match, retry with 6-part format
            if (!isMelisaValid && address.split(",").length == 6) {
                formattedAddresses = formatAddress(address); // Reformat with 6-part structure
                isMelisaValid = verifyAddressFormat(formattedAddresses, melisaResponse);
            }

            // Populate MelisaDTO
            CombinedResponseDTO.MelisaDTO melisaDTO = new CombinedResponseDTO.MelisaDTO();
            if (!melisaLat.isNaN() && !melisaLng.isNaN()) {
                melisaDTO.setLat(melisaLat);
                melisaDTO.setLng(melisaLng);
                melisaDTO.setResults(melisaResults);
            }
            combinedResponseDTO.setMelisa(melisaDTO);
        }

        // Set final verification status based on both APIs
        combinedResponseDTO.setStatus(isGeoMapValid && isMelisaValid ? "Valid Formats" : "Invalid Formats");

        return combinedResponseDTO;
    }

    // Method to call the GeoCode API
    private String callGeoCodeApi(String address) {
        String geoCodeApiUrl = "https://api.example.com/geocode?address=" + address; // Replace with actual URL
        try {
            // Make a GET request to the GeoCode API
            return restTemplate.getForObject(geoCodeApiUrl, String.class);
        } catch (Exception e) {
            // Handle exceptions as needed
            return null; // Return null or handle error
        }
    }

    // Method to clean address
    public String cleanAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.replaceAll("\\s+", " ").trim();
    }

    // Method to check address length validity
    public boolean isLengthValid(String address) {
        String[] parts = address.split(",");
        return parts.length == 5 || parts.length == 6;
    }

    private List<String> formatAddress(String address) {
        String[] parts = address.split(",");
        List<String> formatted = new ArrayList<>();

        // Format assuming the address has 5 parts (general case: addressLine1, city, state, country, zip)
        formatted.add(parts[0].trim() + " " + parts[1].trim() + " " + parts[2].trim() + " " + parts[3].trim() + " " + parts[4].trim());
        formatted.add(parts[3].trim() + ", " + parts[2].trim() + " " + parts[4].trim() + ", " + parts[0].trim() + ", " + parts[1].trim());
        formatted.add(parts[0].trim() + ", " + parts[1].trim() + ", " + parts[2].trim() + " " + parts[4].trim() + ", " + parts[3].trim());

        // If the number of parts equals 6, also generate the formats for a 6-part address
        if (parts.length == 6) {
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

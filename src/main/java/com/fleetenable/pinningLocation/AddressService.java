
package com.fleetenable.pinningLocation;
import java.net.URI;
import java.util.*;

import com.fleetenable.pinningLocation.request.Address;
import com.fleetenable.pinningLocation.request.AddressRequest;
import com.fleetenable.pinningLocation.response.AddressResponseDTO;
import com.fleetenable.pinningLocation.response.AddressResultDTO;
import com.fleetenable.pinningLocation.response.GeoCode;
import com.fleetenable.pinningLocation.response.Melisa;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AddressService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final RestClient restClient;

    private final String geoCode ="https://maps.googleapis.com/maps/api/geocode/json";

    private final String melissaApiKey="RsJPpXGR-TqvzQ1aVY-yXo**";

    private  final String googleAPIKey="AIzaSyAZhRwDMDEqMDauWR0ON1hByv1ftHRjGvU";

    private final String melissaApi="https://address.melissadata.net/V3/WEB/GlobalAddress/doGlobalAddress";

    public AddressService(RestClient restClient) {
        this.restClient = restClient;
    }

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

    public AddressResponseDTO processAddress(AddressRequest addressRequest) {
        AddressResponseDTO responseDTO = new AddressResponseDTO();
        List<String> addresses = addressRequest.getAddresses();
        List<Address> addressList = new ArrayList<>();
        List<String> invaildStrings = new ArrayList<>();
        for(String requestString : addresses) {
            Address address = new Address();
            String[] addressParts = requestString.split(",");
            if(addressParts.length == 5){
                address.setAdd1(addressParts[0]);
                address.setCity(addressParts[1]);
                address.setState(addressParts[2]);
                address.setCountry(addressParts[3]);
                address.setZip(addressParts[4]);
            } else if ( addressParts.length == 6) {
                address.setAdd1(addressParts[0]);
                address.setAdd2(addressParts[1]);
                address.setCity(addressParts[2]);
                address.setState(addressParts[3]);
                address.setCountry(addressParts[4]);
                address.setZip(addressParts[5]);
            } else {
                System.out.println("address parts are more then 6 parts {}" + requestString);
                invaildStrings.add(requestString);
            }
            addressList.add(address);
        }
        responseDTO = getGeoCoadingDetails(addressList);

        System.out.println("invalid address:" + invaildStrings);

    return responseDTO;
    }

    private AddressResponseDTO getGeoCoadingDetails(List<Address> addressList) {
        AddressResponseDTO addressResponseDTO = new AddressResponseDTO();
        List<AddressResultDTO> addressResultDTOS = new ArrayList<>();
        for(Address address : addressList) {
            AddressResultDTO addressResultDTO = new AddressResultDTO();
            String addressString = getAddressString(address, false);
            JSONObject locationResponse = geocodingAPI(addressString);
            boolean isPartialMatch = locationResponse.optBoolean("partial_match", false);
            if (locationResponse == null || locationResponse.optJSONArray("results").isNull(0)) {
                //log.info("location response not found with address string 1, so include address string 2");
                String addressString2 = getAddressString(address, true);
                JSONObject responseWithLine2 = geocodingAPI(addressString2);
                locationResponse = responseWithLine2;
                addressString = addressString2;
            }
            GeoCode geoCode1 = setDataInLocation(locationResponse, isPartialMatch);
            geoCode1.setAddressString(addressString);
            addressResultDTO.setGeoCode(geoCode1);


            //Call geoCode Api for second formate
            String address2String = getAddress2String(address, false);
            JSONObject locationResponseee = geocodingAPI(address2String);
            boolean isPartialMatch1 = locationResponseee.optBoolean("partial_match", false);
            if(isPartialMatch1) {
                if (locationResponseee == null || locationResponseee.optJSONArray("results").isNull(0) ) {
                    String addressString22 = getAddress3String(address, true);
                    JSONObject responseWithLine22 = geocodingAPI(addressString22);
                    boolean isPartialMatch123 = responseWithLine22.optBoolean("partial_match", false);
                    if(!isPartialMatch123){
                        isPartialMatch1 = isPartialMatch123;
                        locationResponseee = responseWithLine22;
                        address2String = addressString22;
                    }
                }else {
                    String addressString22 = getAddress3String(address, true);
                    JSONObject responseWithLine22 = geocodingAPI(addressString22);
                    boolean isPartialMatch123 = responseWithLine22.optBoolean("partial_match", false);
                    if(!isPartialMatch123){
                        isPartialMatch1 = isPartialMatch123;
                        locationResponseee = responseWithLine22;
                        address2String = addressString22;
                    }
                }
            }else {
                if (locationResponseee == null || locationResponseee.optJSONArray("results").isNull(0) ) {
                    String addressString22 = getAddress3String(address, true);
                    JSONObject responseWithLine22 = geocodingAPI(addressString22);
                    boolean isPartialMatch123 = responseWithLine22.optBoolean("partial_match", false);
                    if(!isPartialMatch123){
                        isPartialMatch1 = isPartialMatch123;
                        locationResponseee = responseWithLine22;
                        address2String = addressString22;
                    }
                }
            }

            GeoCode geoCode2 = setDataInLocation(locationResponseee, isPartialMatch1);
            geoCode2.setAddressString(address2String);
            addressResultDTO.setGeoCode2(geoCode2);

            ResponseEntity<String> response = callMellisaApi(address);
            if(response != null && response.getStatusCode().is2xxSuccessful()) {
                Melisa melisa = addDataInLocation(new JSONObject(response.getBody()), getAddressString(address, false));
                addressResultDTO.setMelisa(melisa);
            }
            addressResultDTOS.add(addressResultDTO);
        }
        addressResponseDTO.setResult(addressResultDTOS);
        return addressResponseDTO;
    }

    private String getAddress3String(Address address, boolean b) {
        StringJoiner joiner = new StringJoiner(", ");  // Use comma space for the general joiner

        // Add components in the specified order
        joiner.add(StringUtils.defaultIfBlank(address.getAdd1(), ""));
        joiner.add(StringUtils.defaultIfBlank(address.getCity(), ""));

        // State and ZIP should be joined with a space
        String stateWithZip = StringUtils.defaultIfBlank(address.getState(), "") +
                (StringUtils.isNotBlank(address.getZip()) ? " " + address.getZip() : "");
        joiner.add(stateWithZip.trim());

        joiner.add(StringUtils.defaultIfBlank(address.getCountry(), ""));

        return joiner.toString().trim();
    }

    /*private String getAddress2String(Address address, boolean b) {
        country, state zipcode, add1, city
        add1, city, state zipcode, country
    }*/
    private String getAddress2String(Address address, boolean isAddressLine2) {
        StringJoiner joiner = new StringJoiner(", ");  // Use comma space for the general joiner

        // Add components in the specified order
        joiner.add(StringUtils.defaultIfBlank(address.getCountry(), ""));

        // State and ZIP should be joined with a space
        String stateWithZip = StringUtils.defaultIfBlank(address.getState(), "") +
                (StringUtils.isNotBlank(address.getZip()) ? " " + address.getZip() : "");
        joiner.add(stateWithZip.trim());  // Add trimmed state and ZIP string

        joiner.add(StringUtils.defaultIfBlank(address.getAdd1(), ""));
        joiner.add(StringUtils.defaultIfBlank(address.getCity(), ""));

        return joiner.toString().trim();
    }


    private Melisa addDataInLocation(JSONObject jsonObject, String addressString) {
        Melisa melisa = new Melisa();
        JSONArray recordObject = jsonObject.getJSONArray("Records");
        if(ObjectUtils.isNotEmpty(recordObject)) {
            JSONObject record = recordObject.getJSONObject(0);

            // set country and lat longs
            Float[] coordinates = new Float[]{record.getFloat("Longitude"), record.getFloat("Latitude")};
            melisa.setAddressString(addressString);
            melisa.setLatLag(coordinates);
            melisa.setPartialMatch(setPartialMatch(jsonObject));
        }
        return melisa;
    }

    private Boolean setPartialMatch(JSONObject jsonObject) {
        List<String> melissaGoodAddressCodes = List.of("AV25", "AV24", "AV23", "AV22", "AV14", "AV13");
        String transmissionResultCode = jsonObject.getString("TransmissionResults");
        return !melissaGoodAddressCodes.contains(transmissionResultCode);
    }

    private GeoCode setDataInLocation(JSONObject locationResponse, boolean isPartialMatch) {
        GeoCode result = new GeoCode();
        JSONObject resultsJsonObj = locationResponse.optJSONArray("results").optJSONObject(0);
        String country = getCountryFromJSON(resultsJsonObj);
        JSONObject geometryObj = resultsJsonObj.getJSONObject("geometry");
        JSONObject locationObj = geometryObj.getJSONObject("location");
        result.setLatLng(new Float[]{locationObj.getFloat("lng"), locationObj.getFloat("lat")});
        result.setPartialMatch(isPartialMatch);
        return result;
    }

    private String getCountryFromJSON(JSONObject resultsJsonObj) {
        JSONArray addressComponents = resultsJsonObj.optJSONArray("address_components");
        String country = null;
        for (int i = 0; i < addressComponents.length(); i++) {
            JSONObject addressComponent = (JSONObject) addressComponents.get(i);
            JSONArray addressTypes = addressComponent.optJSONArray("types");
            for (int j = 0; j < addressTypes.length(); j++) {
                if (addressTypes.getString(j).equals("country")) {
                    country = addressComponent.optString("short_name");
                }
            }
        }
        return country;
    }

    /*private void addDataInLocation(Locations location, JSONObject jsonObject) {
        JSONArray recordObject = jsonObject.getJSONArray("Records");
        if(ObjectUtils.isNotEmpty(recordObject)) {
            JSONObject record = recordObject.getJSONObject(0);

            // set country and lat longs
            Float[] coordinates = new Float[]{record.getFloat("Longitude"), record.getFloat("Latitude")};
            location.getLAddress().setCoordinates(coordinates);
            location.setLatLong(coordinates);
            location.setLocationPartialMatch(setPartialMatch(jsonObject));
            location.setInvalidAddress(false);
            location.getLAddress().setCountry(record.getString("CountryISO3166_1_Alpha2"));
            location.setAddressType(record.getString("DeliveryIndicator"));
            setZipCodeData(location, record.getString("PostalCode"));
        }
    }
    }*/
    public ResponseEntity<String> callMellisaApi(Address address) {
        Map<String, String> paramsMap = addMandatoryKeys(address);
        if(ObjectUtils.isEmpty(paramsMap))
            return null;
        ResponseEntity<String> response = callGlobalAddressAPI(paramsMap);
        return response;
    }

    public ResponseEntity<String> callGlobalAddressAPI(Map<String, String> paramsMap) {
        return callAPIUsingAuthToken(null, paramsMap,
                null, "get", melissaApi);
    }

    public ResponseEntity<String> callAPIUsingAuthToken(Map<String, String> headersMap, Map<String, String> params,
                                                        JSONObject requestBody, String httpMethod, String requestURL) {
        HttpHeaders headers = generateHttpEntityFromHeaders(headersMap);
        HttpEntity<String> httpEntity = requestBody == null ? new HttpEntity<>(headers) :
                new HttpEntity<>(requestBody.toString(), headers);
        URI uri = addParams(params, requestURL);
        return executeRestClient(httpEntity, params, HttpMethod.GET, uri);
    }

    private static URI addParams(Map<String, String> paramsMap, String requestURL) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(requestURL);
        if(paramsMap != null) {
            for(Map.Entry<String, String> param : paramsMap.entrySet()) {
                uri.queryParam(param.getKey(), param.getValue());
            }
        }
        return uri.build().toUri();
    }

    private ResponseEntity<String> executeRestClient(HttpEntity<String> httpEntity, Map<String, String> params, HttpMethod method, URI uri) {
        try {
            return restTemplate.exchange(uri, method, httpEntity, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private HttpHeaders generateHttpEntityFromHeaders(Map<String, String> headersMap) {
        if(headersMap != null) {
            HttpHeaders headers = new HttpHeaders();
            for (Map.Entry<String, String> map : headersMap.entrySet()) {
                headers.add(map.getKey(), map.getValue());
            }
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            return headers;
        }
        return null;
    }
    private Map<String, String> addMandatoryKeys(Address address) {
        Map<String, String> paramsMap = new HashMap<>();
        if(StringUtils.isNotBlank(address.getAdd1())) {
            paramsMap.put("id", melissaApiKey);
            paramsMap.put("format", "JSON");
            paramsMap.put("a1", getAddressString(address, false));
            paramsMap.put("ctry", StringUtils.isNotBlank(address.getCountry()) ? address.getCountry() : "US");
            paramsMap.put("opt", "Delivery Lines:ON,ExtendedDateTime:ON");
            return paramsMap;
        }
        return null;
    }
    public JSONObject geocodingAPI(String address) {
        HttpResponse<JsonNode> response = Unirest.get(geoCode)
                .queryString("address", address)
                .queryString("key", googleAPIKey)
                .asJson();
        //logger.info(String.format("response from geo coding api, status code:%s, body:%s", response.getStatus(), response.getBody().getObject()));
        return response.getBody().getObject();
    }

    private String getAddressString(Address address, boolean isAddressLine2) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(StringUtils.defaultIfBlank(address.getAdd1(), ""));
        if (isAddressLine2)
            joiner.add(StringUtils.defaultIfBlank(address.getAdd2(), ""));
        joiner.add(StringUtils.defaultIfBlank(address.getCity(), ""));
        joiner.add(StringUtils.defaultIfBlank(address.getState(), ""));
        joiner.add(StringUtils.defaultIfBlank(address.getCountry(), ""));
        joiner.add(StringUtils.defaultIfBlank(address.getZip(), ""));
        return joiner.toString();
    }

}

package com.fleetenable.pinningLocation;


public class CombinedResponseDTO {
    private String address;
    private GeoCodeDTO geoCode;
    private MelisaDTO melisa;
    private String status;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public GeoCodeDTO getGeoCode() {
        return geoCode;
    }

    public void setGeoCode(GeoCodeDTO geoCode) {
        this.geoCode = geoCode;
    }

    public MelisaDTO getMelisa() {
        return melisa;
    }

    public void setMelisa(MelisaDTO melisa) {
        this.melisa = melisa;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static class GeoCodeDTO {
        private double lat;
        private double lng;
        private boolean partialMatch;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public boolean isPartialMatch() {
            return partialMatch;
        }

        public void setPartialMatch(boolean partialMatch) {
            this.partialMatch = partialMatch;
        }
    }

    // Inner DTO class MelisaDTO with Getters and Setters
    public static class MelisaDTO {
        private double lat;
        private double lng;
        private String results;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public String getResults() {
            return results;
        }

        public void setResults(String results) {
            this.results = results;
        }
    }
}

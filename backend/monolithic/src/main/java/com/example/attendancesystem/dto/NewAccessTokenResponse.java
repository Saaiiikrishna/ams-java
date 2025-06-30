// src/main/java/com/example/attendancesystem/dto/NewAccessTokenResponse.java
package com.example.attendancesystem.dto;

public class NewAccessTokenResponse {

    private String accessToken;
    private String refreshToken; // Uncommented

    // Old constructor removed for clarity, or could be kept if needed for other use cases
    // public NewAccessTokenResponse(String accessToken) {
    //     this.accessToken = accessToken;
    // }

    public NewAccessTokenResponse(String accessToken, String refreshToken) { // Updated constructor
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() { // Uncommented
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) { // Uncommented
        this.refreshToken = refreshToken;
    }
}

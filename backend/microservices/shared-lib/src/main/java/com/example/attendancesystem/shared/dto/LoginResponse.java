package com.example.attendancesystem.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Login response DTO for authentication
 */
public class LoginResponse {

    @JsonProperty("jwt")
    private String jwt; // This is the access token

    @JsonProperty("refreshToken")
    private String refreshToken;

    public LoginResponse(String jwt, String refreshToken) {
        this.jwt = jwt;
        this.refreshToken = refreshToken;
    }

    // Getter for jwt (access token)
    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    // Getter for refreshToken
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }


}

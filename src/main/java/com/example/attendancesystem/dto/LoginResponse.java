package com.example.attendancesystem.dto;

public class LoginResponse {
    private String jwt;

    public LoginResponse(String jwt) {
        this.jwt = jwt;
    }

    // Getter
    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}

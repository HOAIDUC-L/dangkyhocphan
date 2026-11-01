package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String clientVersion = "1.0.0";

    public LoginRequestDto() {
    }

    public LoginRequestDto(String username, String password) {
        this.username = username;
        this.password = password;
        this.clientVersion = "1.0.0";
    }

    public LoginRequestDto(String username, String password, String clientVersion) {
        this.username = username;
        this.password = password;
        this.clientVersion = clientVersion != null ? clientVersion : "1.0.0";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }
}

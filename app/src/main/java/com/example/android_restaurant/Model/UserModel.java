package com.example.android_restaurant.Model;

import java.util.List;

import lombok.Data;

@Data
public class UserModel {
    private boolean success;
    private String message;
    private List<User> result;
}

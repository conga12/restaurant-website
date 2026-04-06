package com.tt.Restaurant.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    @GetMapping("/booking-restaurant")
    public String test() {
        return "Connected!";
    }
}
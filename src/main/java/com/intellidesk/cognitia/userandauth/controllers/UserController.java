package com.intellidesk.cognitia.userandauth.controllers;



import org.springframework.web.bind.annotation.*;

import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.services.impl.UserServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserServiceImpl service;

    public UserController(UserServiceImpl service) {
        this.service = service;
    }

    @GetMapping
    public List<User> getAll() {
        return service.getAll();
    }

    @PostMapping
    public User create(@RequestBody User c) {
        return service.create(c);
    }
}

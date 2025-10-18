package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.repository.UserRepository;
import com.intellidesk.cognitia.userandauth.services.UserService;

@Service
public class UserServiceImpl implements UserService{

    private final UserRepository repo;

    public UserServiceImpl(UserRepository repo) {
        this.repo = repo;
    }

    public List<User> getAll() {
        return repo.findAll(); // tenant filter automatically applied
    }

    public User create(User c) {
        return repo.save(c);
    }
    
}

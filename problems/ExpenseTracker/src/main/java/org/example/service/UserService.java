package org.example.service;

import org.example.entity.User;
import org.example.exception.UserAlreadyExistException;
import org.example.exception.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;

public class UserService {
    private final List<User> users = new ArrayList<>();

    public boolean checkUserExist(String userId) {
        for (User user : users) {
            if (user.getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    public User getUser(String userId) {
        for (User user : users) {
            if (user.getId().equals(userId)) {
                return user;
            }
        }
        throw new UserNotFoundException(userId);
    }

    public void signUpNewUser(User user) {
        if (checkUserExist(user.getId())) {
            throw new UserAlreadyExistException(user.getId());
        }
        users.add(user);
    }
}

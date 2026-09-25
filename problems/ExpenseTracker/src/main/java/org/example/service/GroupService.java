package org.example.service;

import org.example.entity.Expense;
import org.example.entity.Group;
import org.example.entity.User;
import org.example.exception.GroupNotExistException;
import org.example.exception.UserNotFoundException;
import org.example.exception.UserNotGroupMemberException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupService {
    private final ExpenseService expenseService;
    private final UserService userService;
    private final Map<String, List<Group>> userGroups = new HashMap<>(); // userId -> groups they belong to
    private final List<Group> groups = new ArrayList<>();

    public GroupService(ExpenseService expenseService, UserService userService) {
        this.expenseService = expenseService;
        this.userService = userService;
    }

    public void addUserToGroup(String userId, String groupId) {
        Group group = getGroup(groupId);
        if (!userService.checkUserExist(userId)) {
            throw new UserNotFoundException(userId);
        }

        User user = userService.getUser(userId);
        group.add(user);
        userGroups.computeIfAbsent(userId, id -> new ArrayList<>()).add(group);
    }

    // Works for one user or many: every user in the list is added to the new group.
    public Group newGroup(String name, List<User> users) {
        Group group = new Group(UUID.randomUUID().toString(), name);
        for (User user : users) {
            group.add(user);
            userGroups.computeIfAbsent(user.getId(), id -> new ArrayList<>()).add(group);
        }
        groups.add(group);
        return group;
    }

    public void removeFromGroup() {
        // TODO: implement later, removal rules not decided yet
        System.out.println("removeFromGroup: TODO implement later");
    }

    // My groups
    public List<Group> getUserGroups(String userId) {
        if (!userService.checkUserExist(userId)) {
            throw new UserNotFoundException(userId);
        }
        return new ArrayList<>(userGroups.getOrDefault(userId, Collections.emptyList()));
    }

    public List<User> getGroupUsers(String userId, String groupId) {
        return getGroup(checkUserPartOfGroup(userId, groupId)).getMembers();
    }

    public List<Expense> getGroupExpenses(String userId, String groupId) {
        return expenseService.getGroupExpenses(checkUserPartOfGroup(userId, groupId));
    }

    public Map<String, BigDecimal> getUserBalance(String userId, String groupId) {
        return expenseService.getUserBalance(userId, checkUserPartOfGroup(userId, groupId));
    }

    // Gate for the three read methods above: userId must be a member of groupId (covers an unknown
    // groupId too, since it can't be in anyone's group list). Returns groupId so the caller can chain it.
    private String checkUserPartOfGroup(String userId, String groupId) {
        List<Group> memberGroups = userGroups.getOrDefault(userId, Collections.emptyList());
        for (Group group : memberGroups) {
            if (group.getId().equals(groupId)) {
                return groupId;
            }
        }
        throw new UserNotGroupMemberException(userId, groupId);
    }

    private Group getGroup(String groupId) {
        for (Group group : groups) {
            if (group.getId().equals(groupId)) {
                return group;
            }
        }
        throw new GroupNotExistException(groupId);
    }
}

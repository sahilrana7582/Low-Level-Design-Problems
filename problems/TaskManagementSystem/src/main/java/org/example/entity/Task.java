package org.example.entity;

import org.example.enums.TaskPriority;
import org.example.enums.TaskStatus;
import org.example.exceptions.AssigneeAlreadyExistsException;
import org.example.exceptions.TaskAlreadyClosedException;
import org.example.exceptions.UnauthorizedTaskModificationException;
import org.example.exceptions.UserNotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Task {

    private final UUID id;

    private String name;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;

    private final UUID creator;
    private final Set<UUID> assignees;

    public Task(
            UUID id,
            String name,
            String description,
            UUID creator,
            TaskPriority priority,
            Set<UUID> assignees
    ) {
        this.id = Objects.requireNonNull(
                id,
                "Task id must not be null"
        );

        this.name = validateText(
                name,
                "Task name"
        );

        this.description = validateText(
                description,
                "Task description"
        );

        this.priority = Objects.requireNonNull(
                priority,
                "Task priority must not be null"
        );

        this.creator = Objects.requireNonNull(
                creator,
                "Task creator must not be null"
        );

        Objects.requireNonNull(
                assignees,
                "Assignees must not be null"
        );

        if (assignees.contains(null)) {
            throw new IllegalArgumentException(
                    "Assignees must not contain null user ids"
            );
        }

        // LinkedHashSet: getAssignees() should return assignees in the order they were added,
        // not whatever order a plain HashSet happens to iterate in.
        this.assignees = new LinkedHashSet<>(assignees);

        /*
         * A newly created task starts in TODO state.
         */
        this.status = TaskStatus.TODO;
    }

    public void setName(String name, UUID userId) {
        validateCreator(userId);

        this.name = validateText(
                name,
                "Task name"
        );
    }

    public void setDescription(String description, UUID userId) {
        validateCreator(userId);

        this.description = validateText(
                description,
                "Task description"
        );
    }

    public void setStatus(TaskStatus status, UUID userId) {
        validateCreator(userId);

        Objects.requireNonNull(
                status,
                "Task status must not be null"
        );

        // DONE and CANCELLED are terminal: once a task is closed, its status is locked,
        // so it can't be silently reopened (or re-closed) later.
        if (this.status == TaskStatus.DONE || this.status == TaskStatus.CANCELLED) {
            throw new TaskAlreadyClosedException(
                    "Task " + id + " is already " + this.status
                            + " and its status cannot be changed"
            );
        }

        this.status = status;
    }

    public void setPriority(TaskPriority priority, UUID userId) {
        validateCreator(userId);

        this.priority = Objects.requireNonNull(
                priority,
                "Task priority must not be null"
        );
    }

    public void addAssignee(UUID userId, UUID assigneeId) {
        validateCreator(userId);

        Objects.requireNonNull(
                assigneeId,
                "Assignee id must not be null"
        );

        if (assignees.contains(assigneeId)) {
            throw new AssigneeAlreadyExistsException(
                    "User " + assigneeId +
                            " is already assigned to task " + id
            );
        }

        assignees.add(assigneeId);
    }

    public void removeAssignee(UUID userId, UUID assigneeId) {
        validateCreator(userId);

        Objects.requireNonNull(
                assigneeId,
                "Assignee id must not be null"
        );

        if (!assignees.contains(assigneeId)) {
            throw new UserNotFoundException(
                    "User " + assigneeId +
                            " is not assigned to task " + id
            );
        }

        assignees.remove(assigneeId);
    }

    public List<UUID> getAssignees() {
        return assignees.stream()
                .toList();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public UUID getCreator() {
        return creator;
    }

    private void validateCreator(UUID userId) {
        Objects.requireNonNull(
                userId,
                "User id must not be null"
        );

        if (!creator.equals(userId)) {
            throw new UnauthorizedTaskModificationException(
                    "User " + userId +
                            " is not authorized to modify task " + id
            );
        }
    }

    private static String validateText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        String trimmedValue = value.trim();

        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be empty"
            );
        }

        return trimmedValue;
    }
}
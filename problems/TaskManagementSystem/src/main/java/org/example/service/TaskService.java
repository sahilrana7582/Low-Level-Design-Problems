package org.example.service;

import org.example.entity.Task;
import org.example.entity.User;
import org.example.enums.TaskPriority;
import org.example.enums.TaskStatus;
import org.example.exceptions.TaskNotExistsException;

import java.util.*;

public class TaskService {

    private final UserService userService;
    private final Map<UUID, Task> tasks;
    private final Map<UUID, List<Task>> usersTasks;

    public TaskService(UserService userService) {
        this.userService = Objects.requireNonNull(
                userService,
                "UserService must not be null"
        );

        this.tasks = new HashMap<>();
        this.usersTasks = new HashMap<>();
    }

    public Task newTask(
            String name,
            String description,
            UUID creatorId,
            TaskPriority priority,
            Set<UUID> assignees
    ) {
        Objects.requireNonNull(
                creatorId,
                "Creator id must not be null"
        );

        // A task must not reference a user id that doesn't actually exist. getUser throws
        // UserNotFoundException on its own, so a bad id is rejected right here, not later
        // when something eventually tries to resolve it.
        userService.getUser(creatorId);
        if (assignees != null) {
            assignees.forEach(userService::getUser);
        }

        UUID taskId = UUID.randomUUID();

        Task task = new Task(
                taskId,
                name,
                description,
                creatorId,
                priority,
                assignees
        );

        if (!assignees.isEmpty()) {
            assignees.forEach(userId ->
                    this.usersTasks
                            .computeIfAbsent(userId, id -> new ArrayList<>())
                            .add(task)
            );
        }

        tasks.put(taskId, task);
        return task;
    }

    public Task getTask(UUID taskId) {
        validateTask(taskId);

        return tasks.get(taskId);
    }

    public List<User> getAssignedUser(UUID taskId) {
        validateTask(taskId);

        Task task = tasks.get(taskId);

        return task.getAssignees()
                .stream()
                .map(userService::getUser)
                .toList();
    }

    public void assignNewUser(
            UUID taskId,
            UUID userId,
            UUID assigneeId
    ) {
        validateTask(taskId);

        Objects.requireNonNull(
                userId,
                "User id must not be null"
        );

        Objects.requireNonNull(
                assigneeId,
                "Assignee id must not be null"
        );

        userService.getUser(assigneeId); // rejects an assignee id that isn't a real user

        Task task = tasks.get(taskId);

        task.addAssignee(userId, assigneeId);

        usersTasks
                .computeIfAbsent(
                        assigneeId,
                        id -> new ArrayList<>()
                )
                .add(task);
    }

    public List<Task> getTasksForUser(
            UUID userId,
            TaskPriority priority,
            TaskStatus status
    ) {
        Objects.requireNonNull(
                userId,
                "User id must not be null"
        );

        List<Task> userTasks = usersTasks.getOrDefault(
                userId,
                List.of()
        );

        return userTasks.stream()
                .filter(task -> priority == null || task.getPriority() == priority)
                .filter(task -> status == null || task.getStatus() == status)
                .toList();
    }

    public void cancelTask(UUID taskId, UUID userId) {
        validateTask(taskId);

        Objects.requireNonNull(
                userId,
                "User id must not be null"
        );

        Task task = tasks.get(taskId);

        task.setStatus(TaskStatus.CANCELLED, userId);
    }



    private void validateTask(UUID taskId) {
        Objects.requireNonNull(
                taskId,
                "Task id must not be null"
        );

        if (!tasks.containsKey(taskId)) {
            throw new TaskNotExistsException(
                    "Task does not exist with id: " + taskId
            );
        }
    }
}
package org.example;

import org.example.entity.Task;
import org.example.entity.User;
import org.example.enums.TaskPriority;
import org.example.enums.TaskStatus;
import org.example.exceptions.AssigneeAlreadyExistsException;
import org.example.exceptions.TaskAlreadyClosedException;
import org.example.exceptions.TaskNotExistsException;
import org.example.exceptions.UnauthorizedTaskModificationException;
import org.example.exceptions.UserAlreadyExistsException;
import org.example.exceptions.UserNotFoundException;
import org.example.service.TaskService;
import org.example.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Main {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        UserService userService = new UserService();
        TaskService taskService = new TaskService(userService);
        UUID unknownId = UUID.randomUUID();

        // ---------------------------------------------------------------
        section("Users");
        User alice = new User(UUID.randomUUID(), "Alice", "alice@mail.com");
        User bob = new User(UUID.randomUUID(), "Bob", "bob@mail.com");
        User carol = new User(UUID.randomUUID(), "Carol", "carol@mail.com");
        userService.addUser(alice);
        userService.addUser(bob);
        userService.addUser(carol);

        expect("email is actually stored as the email (not the name)", "alice@mail.com", alice.getEmail());
        expect("getUser returns Bob", "Bob", userService.getUser(bob.getId()).getName());
        expectError("getUser of an unknown id", UserNotFoundException.class,
                () -> userService.getUser(unknownId));
        expectError("add a user with the same email again", UserAlreadyExistsException.class,
                () -> userService.addUser(new User(UUID.randomUUID(), "Alice Two", "alice@mail.com")));
        expectError("add a user with the same email in capital letters", UserAlreadyExistsException.class,
                () -> userService.addUser(new User(UUID.randomUUID(), "Alice Two", "ALICE@MAIL.COM")));

        // ---------------------------------------------------------------
        section("Create a task");
        // A plain HashSet has no defined iteration order, so a LinkedHashSet is used here to make
        // the initial assignment order well-defined enough to assert on below.
        Task task1 = taskService.newTask("Design schema", "Draft the DB schema", alice.getId(),
                TaskPriority.HIGH, new LinkedHashSet<>(Arrays.asList(bob.getId(), carol.getId())));
        expect("task starts as TODO", TaskStatus.TODO, task1.getStatus());
        expect("task creator", alice.getId(), task1.getCreator());
        expect("task priority", TaskPriority.HIGH, task1.getPriority());
        expect("task assignees", assigneeSet(bob, carol), new HashSet<>(task1.getAssignees()));

        expectError("create a task for an unknown creator", UserNotFoundException.class,
                () -> taskService.newTask("Ghost task", "desc", unknownId, TaskPriority.LOW, Collections.emptySet()));
        expectError("create a task with an unknown assignee", UserNotFoundException.class,
                () -> taskService.newTask("Ghost task", "desc", alice.getId(), TaskPriority.LOW,
                        Collections.singleton(unknownId)));
        expectError("create a task with a blank name", IllegalArgumentException.class,
                () -> taskService.newTask("   ", "desc", alice.getId(), TaskPriority.LOW, Collections.emptySet()));
        expect("no ghost task leaked into anyone's list", 0, taskService.getTasksForUser(alice.getId(), null, null).size());

        // ---------------------------------------------------------------
        section("getTask / getAssignedUser");
        expect("getTask returns the same task", task1.getId(), taskService.getTask(task1.getId()).getId());
        expectError("getTask of an unknown id", TaskNotExistsException.class,
                () -> taskService.getTask(unknownId));

        expect("assigned users, in assignment order", Arrays.asList("Bob", "Carol"), names(taskService.getAssignedUser(task1.getId())));
        expectError("getAssignedUser of an unknown task", TaskNotExistsException.class,
                () -> taskService.getAssignedUser(unknownId));

        // ---------------------------------------------------------------
        section("Only the creator can modify a task");
        expectError("Bob (not the creator) renames the task", UnauthorizedTaskModificationException.class,
                () -> task1.setName("Renamed", bob.getId()));
        task1.setName("Design the DB schema", alice.getId());
        expect("Alice (the creator) renamed it", "Design the DB schema", task1.getName());

        expectError("Bob sets the status", UnauthorizedTaskModificationException.class,
                () -> task1.setStatus(TaskStatus.IN_PROGRESS, bob.getId()));
        task1.setStatus(TaskStatus.IN_PROGRESS, alice.getId());
        expect("Alice set the status", TaskStatus.IN_PROGRESS, task1.getStatus());

        expectError("Bob sets the priority", UnauthorizedTaskModificationException.class,
                () -> task1.setPriority(TaskPriority.LOW, bob.getId()));
        task1.setPriority(TaskPriority.MEDIUM, alice.getId());
        expect("Alice set the priority", TaskPriority.MEDIUM, task1.getPriority());

        // ---------------------------------------------------------------
        section("Assigning and removing users");
        expectError("Carol (not the creator) adds an assignee", UnauthorizedTaskModificationException.class,
                () -> taskService.assignNewUser(task1.getId(), carol.getId(), bob.getId()));
        expectError("assign an unknown user", UserNotFoundException.class,
                () -> taskService.assignNewUser(task1.getId(), alice.getId(), unknownId));
        expectError("assign an unknown task", TaskNotExistsException.class,
                () -> taskService.assignNewUser(unknownId, alice.getId(), bob.getId()));

        User dave = new User(UUID.randomUUID(), "Dave", "dave@mail.com");
        userService.addUser(dave);
        taskService.assignNewUser(task1.getId(), alice.getId(), dave.getId());
        expect("assignees after adding Dave", Arrays.asList("Bob", "Carol", "Dave"), names(taskService.getAssignedUser(task1.getId())));
        expect("Dave's task list now includes task1", Collections.singletonList(task1.getId()),
                taskIds(taskService.getTasksForUser(dave.getId(), null, null)));

        expectError("add Bob again (already assigned)", AssigneeAlreadyExistsException.class,
                () -> task1.addAssignee(alice.getId(), bob.getId()));
        expectError("Bob (not the creator) removes an assignee", UnauthorizedTaskModificationException.class,
                () -> task1.removeAssignee(bob.getId(), carol.getId()));

        task1.removeAssignee(alice.getId(), carol.getId());
        expect("assignees after removing Carol", Arrays.asList("Bob", "Dave"), names(taskService.getAssignedUser(task1.getId())));
        expectError("remove Carol again (not assigned any more)", UserNotFoundException.class,
                () -> task1.removeAssignee(alice.getId(), carol.getId()));

        // ---------------------------------------------------------------
        section("Filtering tasks by priority and status");
        Task task2 = taskService.newTask("Write tests", "Cover the schema", alice.getId(),
                TaskPriority.LOW, Collections.singleton(bob.getId()));
        Task task3 = taskService.newTask("Fix bug", "Null pointer on save", alice.getId(),
                TaskPriority.HIGH, Collections.singleton(bob.getId()));
        task3.setStatus(TaskStatus.DONE, alice.getId());

        // Bob is on task1 (MEDIUM, IN_PROGRESS), task2 (LOW, TODO) and task3 (HIGH, DONE)
        expect("Bob, no filter", Arrays.asList(task1.getId(), task2.getId(), task3.getId()),
                taskIds(taskService.getTasksForUser(bob.getId(), null, null)));
        expect("Bob, priority=HIGH", Collections.singletonList(task3.getId()),
                taskIds(taskService.getTasksForUser(bob.getId(), TaskPriority.HIGH, null)));
        expect("Bob, status=TODO", Collections.singletonList(task2.getId()),
                taskIds(taskService.getTasksForUser(bob.getId(), null, TaskStatus.TODO)));
        expect("Bob, priority=HIGH and status=DONE", Collections.singletonList(task3.getId()),
                taskIds(taskService.getTasksForUser(bob.getId(), TaskPriority.HIGH, TaskStatus.DONE)));
        expect("Bob, priority=HIGH and status=TODO (none match)", Collections.emptyList(),
                taskIds(taskService.getTasksForUser(bob.getId(), TaskPriority.HIGH, TaskStatus.TODO)));
        expect("unknown user has no tasks", Collections.emptyList(), taskIds(taskService.getTasksForUser(unknownId, null, null)));

        // getTasksForUser builds its result with Stream.toList(), which is unmodifiable by design
        // (stronger than a mutable defensive copy: the caller can't even attempt to mutate it).
        List<Task> bobTasks = taskService.getTasksForUser(bob.getId(), null, null);
        expectError("the returned list can't be mutated", UnsupportedOperationException.class, bobTasks::clear);
        expect("nothing changed", 3, taskService.getTasksForUser(bob.getId(), null, null).size());

        // ---------------------------------------------------------------
        section("Cancel task");
        expectError("Bob (not the creator) cancels task2", UnauthorizedTaskModificationException.class,
                () -> taskService.cancelTask(task2.getId(), bob.getId()));
        taskService.cancelTask(task2.getId(), alice.getId());
        expect("task2 status is CANCELLED", TaskStatus.CANCELLED, task2.getStatus());
        expectError("cancel an unknown task", TaskNotExistsException.class,
                () -> taskService.cancelTask(unknownId, alice.getId()));

        // ---------------------------------------------------------------
        section("Once a task is DONE or CANCELLED, its status is locked");
        expectError("re-cancelling an already-cancelled task", TaskAlreadyClosedException.class,
                () -> taskService.cancelTask(task2.getId(), alice.getId()));
        expectError("reopening a cancelled task", TaskAlreadyClosedException.class,
                () -> task2.setStatus(TaskStatus.TODO, alice.getId()));
        expectError("changing a done task's status", TaskAlreadyClosedException.class,
                () -> task3.setStatus(TaskStatus.IN_PROGRESS, alice.getId()));

        // task1 is still open (IN_PROGRESS), so it can still move normally...
        task1.setStatus(TaskStatus.DONE, alice.getId());
        expect("task1 can still finish normally", TaskStatus.DONE, task1.getStatus());
        // ...and once it does, it locks too.
        expectError("task1 is now locked as well", TaskAlreadyClosedException.class,
                () -> task1.setStatus(TaskStatus.IN_PROGRESS, alice.getId()));

        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("=== Result: " + passed + " passed, " + failed + " failed ===");
    }

    // ----- small helpers -----

    private static Set<UUID> assigneeSet(User... users) {
        Set<UUID> ids = new HashSet<>();
        for (User user : users) {
            ids.add(user.getId());
        }
        return ids;
    }

    private static List<String> names(List<User> users) {
        return users.stream().map(User::getName).toList();
    }

    private static List<UUID> taskIds(List<Task> tasks) {
        return tasks.stream().map(Task::getId).toList();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void expect(String scenario, Object expected, Object actual) {
        record(Objects.equals(expected, actual), scenario, String.valueOf(expected), String.valueOf(actual));
    }

    // Runs the action and expects exactly this exception type
    private static void expectError(String scenario, Class<? extends RuntimeException> expected, Runnable action) {
        String actual;
        boolean ok;
        try {
            action.run();
            actual = "no exception";
            ok = false;
        } catch (RuntimeException e) {
            actual = e.getClass().getSimpleName() + " (" + e.getMessage() + ")";
            ok = e.getClass().equals(expected);
        }
        record(ok, scenario, expected.getSimpleName(), actual);
    }

    private static void record(boolean ok, String scenario, String expected, String actual) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println("  " + (ok ? "PASS" : "FAIL") + " | " + scenario
                + " | expected: " + expected + " | actual: " + actual);
    }
}

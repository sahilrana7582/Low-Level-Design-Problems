package entity;

public class User {
    private final String id;
    private final String name;
    private final int age;
    private final String email;
    private final String phoneNumber;

    public User(
            String id,
            String name,
            int age,
            String email,
            String phoneNumber
    ) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}

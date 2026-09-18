import java.util.*;

public class Main {

    public static void main(String[] args) {

        ArrayList<Comparable> users = new ArrayList<>();

        users.add(new User("Sahil", 25));
        users.add(new User("Rahul", 20));
        users.add(new User("Amit", 30));
        users.add(new User("John", 18));
        users.add(new User("David", 25));

        System.out.println("Before sorting:");

        for (Comparable user : users) {
            System.out.println(user);
        }

        Collections.sort(users);

        System.out.println("\nAfter sorting:");

        for (Comparable user : users) {
            System.out.println(user);
        }
    }
}
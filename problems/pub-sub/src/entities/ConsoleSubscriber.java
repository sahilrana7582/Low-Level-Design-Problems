package entities;

public final class ConsoleSubscriber implements Subscriber {

    private final String name;

    public ConsoleSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void receive(Message message) {
        System.out.println(
                "[" + name + "] received -> " + message
        );
    }

    @Override
    public String toString() {
        return "ConsoleSubscriber{" +
                "name='" + name + '\'' +
                '}';
    }
}
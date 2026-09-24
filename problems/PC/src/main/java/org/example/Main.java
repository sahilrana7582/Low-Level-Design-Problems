package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {    static void work() {
    while (true) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }
}

    public static void main(String[] args) {
        Thread t1 = new Thread(Main::work, "worker-1");
        Thread t2 = new Thread(Main::work, "worker-2");

        t1.start();
        t2.start();

        work();
    }
}

package entities;

import java.time.LocalDate;

public abstract class Indentity {

    private final String userName;
    private final LocalDate userDob;

    public Indentity(String userName, LocalDate userDob) {
        this.userName = userName;
        this.userDob = userDob;
    }

    public String getUserName() {
        return userName;
    }

    public LocalDate getUserDob() {
        return userDob;
    }

    public abstract String getIdentityNumber();
}

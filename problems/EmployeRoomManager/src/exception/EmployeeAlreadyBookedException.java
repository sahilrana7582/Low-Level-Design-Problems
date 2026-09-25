package exception;

public class EmployeeAlreadyBookedException extends RuntimeException {
    public EmployeeAlreadyBookedException(String message) {
        super(message);
    }
}

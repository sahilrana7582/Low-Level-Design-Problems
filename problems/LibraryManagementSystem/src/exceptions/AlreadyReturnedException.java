package exceptions;

public class AlreadyReturnedException extends RuntimeException {

    public AlreadyReturnedException(String loanId) {
        super("Loan has already been returned: " + loanId);
    }
}
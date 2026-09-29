package org.example.exceptions;

public class TaskNotExistsException extends RuntimeException{
    public TaskNotExistsException(String message){
        super(message);
    }
}

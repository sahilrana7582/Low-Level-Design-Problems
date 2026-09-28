package org.example.exception;

public class RecipeNotExistException extends RuntimeException{
    public RecipeNotExistException(String message){
        super(message);
    }
}

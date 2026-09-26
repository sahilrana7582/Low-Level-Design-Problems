package org.example.exception;

public class ArtistAlreadyExists extends RuntimeException {

    public ArtistAlreadyExists(String artistName) {
        super(String.format("%s artist already exists", artistName));
    }
}

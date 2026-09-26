package org.example.exception;

public class SongAlreadyExists extends RuntimeException{

    public SongAlreadyExists(String songName, String artistName) {
        super(String.format("%s song already exist for %s Artist", songName, artistName));
    }
}

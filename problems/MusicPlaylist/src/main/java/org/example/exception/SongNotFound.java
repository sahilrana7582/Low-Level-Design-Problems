package org.example.exception;

public class SongNotFound extends RuntimeException{

    public SongNotFound(String songName, String artistName) {
        super(String.format("%s song not found for %s Artist", songName, artistName));
    }
}

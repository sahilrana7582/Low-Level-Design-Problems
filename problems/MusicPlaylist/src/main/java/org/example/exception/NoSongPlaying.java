package org.example.exception;

public class NoSongPlaying extends RuntimeException {

    public NoSongPlaying(String userName) {
        super(String.format("%s is not playing any song", userName));
    }
}

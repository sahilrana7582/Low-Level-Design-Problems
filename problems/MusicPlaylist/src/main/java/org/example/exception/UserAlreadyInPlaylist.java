package org.example.exception;

public class UserAlreadyInPlaylist extends RuntimeException {

    public UserAlreadyInPlaylist(String userName, String playlistName) {
        super(String.format("%s is already in playlist %s", userName, playlistName));
    }
}

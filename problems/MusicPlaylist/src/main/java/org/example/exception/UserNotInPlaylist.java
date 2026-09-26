package org.example.exception;

import java.util.UUID;

public class UserNotInPlaylist extends RuntimeException {

    public UserNotInPlaylist(UUID userId, String playlistName) {
        super(String.format("User %s is not in playlist %s", userId, playlistName));
    }
}

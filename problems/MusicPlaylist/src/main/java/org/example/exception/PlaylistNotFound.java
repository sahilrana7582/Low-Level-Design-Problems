package org.example.exception;

import java.util.UUID;

public class PlaylistNotFound extends RuntimeException {

    public PlaylistNotFound(UUID playlistId, UUID userId) {
        super(String.format("Playlist %s not found for user %s", playlistId, userId));
    }
}

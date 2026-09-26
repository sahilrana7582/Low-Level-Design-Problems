package org.example.exception;

import java.util.UUID;

public class PlayableNotFound extends RuntimeException {

    public PlayableNotFound(UUID playableId, String playlistName) {
        super(String.format("Playable %s not found in playlist %s", playableId, playlistName));
    }
}

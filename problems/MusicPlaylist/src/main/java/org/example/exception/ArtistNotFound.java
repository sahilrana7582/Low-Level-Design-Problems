package org.example.exception;

import java.util.UUID;

public class ArtistNotFound extends RuntimeException {

    public ArtistNotFound(UUID artistId) {
        super(String.format("Artist not found for id %s", artistId));
    }
}

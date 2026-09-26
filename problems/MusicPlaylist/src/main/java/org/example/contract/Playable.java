package org.example.contract;

import org.example.entity.Artist;

import java.util.UUID;

public interface Playable {
    UUID getId();
    String getName();
    void play();
    void stop();
    Artist getArtist();
}

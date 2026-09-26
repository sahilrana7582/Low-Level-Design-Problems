package org.example.entity;

import org.example.contract.Playable;

import java.util.UUID;

public class Song implements Playable {
    private UUID id;
    private String name;
    private Artist artist;

    public Song(String name, Artist artist){
        this.id = UUID.randomUUID();
        this.name = name;
        this.artist = artist;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void play() {
        System.out.printf("%s music is playing...", this.name);
    }

    @Override
    public void stop() {
        System.out.printf("%s music is stopped...", this.name);
    }

    @Override
    public Artist getArtist() {
        return this.artist;
    }
}

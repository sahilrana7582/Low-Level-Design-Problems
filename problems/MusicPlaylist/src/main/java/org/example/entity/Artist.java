package org.example.entity;

import org.example.contract.Identity;
import org.example.exception.SongAlreadyExists;

import java.util.*;

public class Artist implements Identity {
    private User user;
    private List<Song> songs;

    public Artist(User user) {
        this.user = user;
        this.songs = new ArrayList<>();
    }

    @Override
    public UUID getId() {
        return this.user.getId();
    }

    @Override
    public String getName() {
        return this.user.getName();
    }

    @Override
    public String getEmail() {
        return this.user.getEmail();
    }

    public void addNewSong(String songName) {
        Optional<Song> song = songs.stream()
                .filter(s -> s.getName().equalsIgnoreCase(songName))
                .findFirst();
        if(song.isPresent()){
            throw new SongAlreadyExists(songName, this.getName());
        }
        Song newSong = new Song(songName, this);
        songs.add(newSong);
    }

    public void removeSong(Song song) {
        songs.removeIf(s -> s.getId().equals(song.getId()));
    }

    public List<Song> getSong(){
        return this.songs;
    }
}

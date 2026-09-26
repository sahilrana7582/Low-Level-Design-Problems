package org.example.entity;

import org.example.contract.Identity;

import java.util.UUID;

public class User implements Identity {
    private Account account;
    private Song currentTrack;

    public User(Account account) {
        this.account = account;
    }

    @Override
    public UUID getId() {
        return this.account.getId();
    }

    @Override
    public String getName() {
        return this.account.getName();
    }

    @Override
    public String getEmail() {
        return this.account.getEmail();
    }

    public void playSong(Song song){
        song.play();
        this.currentTrack = song;
    }

    public void stopSong(){
        this.currentTrack.stop();
    }
}

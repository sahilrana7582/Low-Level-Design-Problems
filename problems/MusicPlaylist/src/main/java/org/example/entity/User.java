package org.example.entity;

import org.example.contract.Identity;
import org.example.exception.NoSongPlaying;

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

    // Playing a new song first stops the one that is playing.
    public void playSong(Song song){
        if (this.currentTrack != null) {
            this.currentTrack.stop();
        }
        song.play();
        this.currentTrack = song;
    }

    public void stopSong(){
        if (this.currentTrack == null) {
            throw new NoSongPlaying(this.getName());
        }
        this.currentTrack.stop();
        this.currentTrack = null;
    }
}

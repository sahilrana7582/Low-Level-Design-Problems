package org.example.exception;

public class PlayableAlreadyExist extends RuntimeException{
    public PlayableAlreadyExist(String playableName, String playlistName){
        super(String.format("%s playable already exist in %s playlist", playableName, playlistName));
    }
}

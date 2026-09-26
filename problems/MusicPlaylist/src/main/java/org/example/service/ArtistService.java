package org.example.service;

import org.example.entity.Artist;
import org.example.entity.Song;
import org.example.entity.User;
import org.example.exception.ArtistAlreadyExists;
import org.example.exception.ArtistNotFound;
import org.example.exception.SongNotFound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArtistService {
    private final UserService userService;
    private final Map<UUID, Artist> artists = new HashMap<>();

    public ArtistService(UserService userService) {
        this.userService = userService;
    }

    // An artist is created from an existing user, so the artist id is the user id.
    public Artist createArtist(UUID userId) {
        User user = userService.getUser(userId);
        if (artists.containsKey(userId)) {
            throw new ArtistAlreadyExists(user.getName());
        }
        Artist artist = new Artist(user);
        artists.put(artist.getId(), artist);
        return artist;
    }

    public Artist getArtist(UUID artistId) {
        Artist artist = artists.get(artistId);
        if (artist == null) {
            throw new ArtistNotFound(artistId);
        }
        return artist;
    }

    public boolean checkArtistExist(UUID artistId) {
        return artists.containsKey(artistId);
    }

    // Throws SongAlreadyExists if the artist already has a song with this name.
    public void addSong(UUID artistId, String songName) {
        getArtist(artistId).addNewSong(songName);
    }

    public Song getSong(UUID artistId, String songName) {
        Artist artist = getArtist(artistId);
        for (Song song : artist.getSong()) {
            if (song.getName().equalsIgnoreCase(songName)) {
                return song;
            }
        }
        throw new SongNotFound(songName, artist.getName());
    }

    public void removeSong(UUID artistId, String songName) {
        Song song = getSong(artistId, songName);
        getArtist(artistId).removeSong(song);
    }

    public List<Song> getSongs(UUID artistId) {
        return new ArrayList<>(getArtist(artistId).getSong());
    }
}

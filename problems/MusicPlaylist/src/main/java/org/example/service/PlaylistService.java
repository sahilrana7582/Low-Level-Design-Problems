package org.example.service;

import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.entity.User;
import org.example.exception.PlaylistNotFound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Users and songs are never passed in as objects: they are looked up by id through UserService and
// ArtistService, so an unregistered user or a song that is not in an artist's catalogue is rejected.
// Every method that changes a playlist takes the id of the user doing it. The playlist is looked up
// in that user's own list, so a user can only change playlists they are part of.
public class PlaylistService {
    private final UserService userService;
    private final ArtistService artistService;
    private final Map<UUID, List<Playlist>> userPlaylists = new HashMap<>(); // userId -> playlists they are in

    public PlaylistService(UserService userService, ArtistService artistService) {
        this.userService = userService;
        this.artistService = artistService;
    }

    // Throws UserNotFound if the creator is not a registered user.
    public Playlist createPlaylist(String name, UUID creatorId) {
        User creator = userService.getUser(creatorId);
        Playlist playlist = new Playlist(name, creator);
        userPlaylists.computeIfAbsent(creatorId, id -> new ArrayList<>()).add(playlist);
        return playlist;
    }

    // All playlists of the user (empty if none)
    public List<Playlist> getUserPlaylists(UUID userId) {
        return new ArrayList<>(userPlaylists.getOrDefault(userId, Collections.emptyList()));
    }

    // The song is taken from the artist's own songs. Throws ArtistNotFound or SongNotFound if it is not there.
    public void addNewPlayable(UUID userId, UUID playlistId, UUID artistId, String songName) {
        Playlist playlist = findPlaylist(userId, playlistId);
        Song song = artistService.getSong(artistId, songName);
        playlist.addNewPlayable(song);
    }

    public void removePlayable(UUID userId, UUID playlistId, UUID playableId) {
        findPlaylist(userId, playlistId).removePlayable(playableId);
    }

    // userId is the user adding, newUserId is the user being added. Both then see the playlist.
    // Throws UserNotFound if the new user is not a registered user.
    public void addNewUser(UUID userId, UUID playlistId, UUID newUserId) {
        Playlist playlist = findPlaylist(userId, playlistId);
        User newUser = userService.getUser(newUserId);
        playlist.addNewUser(newUser);
        userPlaylists.computeIfAbsent(newUserId, id -> new ArrayList<>()).add(playlist);
    }

    // userId is the user removing, userToRemoveId is the user being removed. The playlist leaves their list.
    public void removeUser(UUID userId, UUID playlistId, UUID userToRemoveId) {
        Playlist playlist = findPlaylist(userId, playlistId);
        playlist.removeUser(userToRemoveId);
        userPlaylists.get(userToRemoveId).removeIf(p -> p.getId().equals(playlistId));
    }

    private Playlist findPlaylist(UUID userId, UUID playlistId) {
        for (Playlist playlist : userPlaylists.getOrDefault(userId, Collections.emptyList())) {
            if (playlist.getId().equals(playlistId)) {
                return playlist;
            }
        }
        throw new PlaylistNotFound(playlistId, userId);
    }
}

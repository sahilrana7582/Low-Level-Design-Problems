package org.example.service;

import org.example.contract.Playable;
import org.example.entity.Playlist;
import org.example.entity.User;
import org.example.exception.PlaylistNotFound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Every method that changes a playlist takes the id of the user doing it. The playlist is looked up
// in that user's own list, so a user can only change playlists they are part of.
public class PlaylistService {
    private final Map<UUID, List<Playlist>> userPlaylists = new HashMap<>(); // userId -> playlists they are in

    public Playlist createPlaylist(String name, User creator) {
        Playlist playlist = new Playlist(name, creator);
        userPlaylists.computeIfAbsent(creator.getId(), id -> new ArrayList<>()).add(playlist);
        return playlist;
    }

    // All playlists of the user (empty if none)
    public List<Playlist> getUserPlaylists(UUID userId) {
        return new ArrayList<>(userPlaylists.getOrDefault(userId, Collections.emptyList()));
    }

    public void addNewPlayable(UUID userId, UUID playlistId, Playable playable) {
        findPlaylist(userId, playlistId).addNewPlayable(playable);
    }

    public void removePlayable(UUID userId, UUID playlistId, UUID playableId) {
        findPlaylist(userId, playlistId).removePlayable(playableId);
    }

    // userId is the user adding, newUser is the user being added. Both then see the playlist.
    public void addNewUser(UUID userId, UUID playlistId, User newUser) {
        Playlist playlist = findPlaylist(userId, playlistId);
        playlist.addNewUser(newUser);
        userPlaylists.computeIfAbsent(newUser.getId(), id -> new ArrayList<>()).add(playlist);
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

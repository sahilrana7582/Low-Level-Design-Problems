package org.example.entity;

import org.example.contract.Playable;
import org.example.exception.PlayableAlreadyExist;
import org.example.exception.PlayableNotFound;
import org.example.exception.UserAlreadyInPlaylist;
import org.example.exception.UserNotInPlaylist;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playlist {
    private final UUID id;
    private final String name;
    private final List<Playable> playables = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    // The creator is the first user of the playlist.
    public Playlist(String name, User creator) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.users.add(creator);
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void addNewPlayable(Playable playable) {
        if (findPlayable(playable.getId()) != null) {
            throw new PlayableAlreadyExist(playable.getName(), this.name);
        }
        playables.add(playable);
    }

    public void removePlayable(UUID playableId) {
        boolean removed = playables.removeIf(p -> p.getId().equals(playableId));
        if (!removed) {
            throw new PlayableNotFound(playableId, this.name);
        }
    }

    public List<Playable> getAllPlayables() {
        return new ArrayList<>(playables);
    }

    public Playable getOnePlayable(UUID playableId) {
        Playable playable = findPlayable(playableId);
        if (playable == null) {
            throw new PlayableNotFound(playableId, this.name);
        }
        return playable;
    }

    public void addNewUser(User user) {
        if (isUserInPlaylist(user.getId())) {
            throw new UserAlreadyInPlaylist(user.getName(), this.name);
        }
        users.add(user);
    }

    public void removeUser(UUID userId) {
        boolean removed = users.removeIf(u -> u.getId().equals(userId));
        if (!removed) {
            throw new UserNotInPlaylist(userId, this.name);
        }
    }

    public List<User> getUsers() {
        return new ArrayList<>(users);
    }

    private Playable findPlayable(UUID playableId) {
        for (Playable playable : playables) {
            if (playable.getId().equals(playableId)) {
                return playable;
            }
        }
        return null;
    }

    private boolean isUserInPlaylist(UUID userId) {
        for (User user : users) {
            if (user.getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }
}

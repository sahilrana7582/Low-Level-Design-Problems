package org.example;

import org.example.contract.Playable;
import org.example.entity.Account;
import org.example.entity.Artist;
import org.example.entity.Playlist;
import org.example.entity.Song;
import org.example.entity.User;
import org.example.exception.AccountAlreadyExists;
import org.example.exception.AccountNotFound;
import org.example.exception.ArtistAlreadyExists;
import org.example.exception.ArtistNotFound;
import org.example.exception.NoSongPlaying;
import org.example.exception.PlayableAlreadyExist;
import org.example.exception.PlayableNotFound;
import org.example.exception.PlaylistNotFound;
import org.example.exception.SongAlreadyExists;
import org.example.exception.SongNotFound;
import org.example.exception.UserAlreadyExists;
import org.example.exception.UserAlreadyInPlaylist;
import org.example.exception.UserNotFound;
import org.example.exception.UserNotInPlaylist;
import org.example.service.AccountService;
import org.example.service.ArtistService;
import org.example.service.PlaylistService;
import org.example.service.UserService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Main {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        AccountService accountService = new AccountService();
        UserService userService = new UserService(accountService);
        ArtistService artistService = new ArtistService(userService);
        PlaylistService playlistService = new PlaylistService(userService, artistService);
        UUID unknownId = UUID.randomUUID();

        // ---------------------------------------------------------------
        section("Accounts");
        Account alice = accountService.createAccount("Alice", "alice@mail.com");
        Account bob = accountService.createAccount("Bob", "bob@mail.com");
        Account carol = accountService.createAccount("Carol", "carol@mail.com");
        Account dave = accountService.createAccount("Dave", "dave@mail.com");
        Account weeknd = accountService.createAccount("The Weeknd", "weeknd@mail.com");
        Account dua = accountService.createAccount("Dua Lipa", "dua@mail.com");
        Account eve = accountService.createAccount("Eve", "eve@mail.com"); // never becomes a user
        expect("account id is assigned", true, alice.getId() != null);
        expect("getAccount returns Alice", "Alice", accountService.getAccount(alice.getId()).getName());
        expect("Alice's account exists", true, accountService.checkAccountExist(alice.getId()));
        expect("unknown account does not exist", false, accountService.checkAccountExist(unknownId));
        expectError("same email again", AccountAlreadyExists.class,
                () -> accountService.createAccount("Alice Two", "alice@mail.com"));
        expectError("same email in capital letters", AccountAlreadyExists.class,
                () -> accountService.createAccount("Alice Two", "ALICE@MAIL.COM"));
        expectError("getAccount of unknown id", AccountNotFound.class,
                () -> accountService.getAccount(unknownId));

        // ---------------------------------------------------------------
        section("Accounts: empty input is rejected and nothing is stored");
        expectError("null email", IllegalArgumentException.class,
                () -> accountService.createAccount("N", null));
        expectError("empty email", IllegalArgumentException.class,
                () -> accountService.createAccount("N", ""));
        expectError("null name", IllegalArgumentException.class,
                () -> accountService.createAccount(null, "n@mail.com"));
        expectError("empty name", IllegalArgumentException.class,
                () -> accountService.createAccount("", "n@mail.com"));
        expectError("blank name", IllegalArgumentException.class,
                () -> accountService.createAccount("   ", "n@mail.com"));
        expect("the next createAccount still works", "Frank",
                accountService.createAccount("Frank", "frank@mail.com").getName());

        // ---------------------------------------------------------------
        section("Users (created from an account)");
        User aliceUser = userService.createUser(alice.getId());
        User bobUser = userService.createUser(bob.getId());
        userService.createUser(carol.getId());
        userService.createUser(dave.getId());
        User weekndUser = userService.createUser(weeknd.getId());
        User duaUser = userService.createUser(dua.getId());
        expect("user id is the account id", alice.getId(), aliceUser.getId());
        expect("user name", "Alice", aliceUser.getName());
        expect("user email", "alice@mail.com", aliceUser.getEmail());
        expect("getUser returns Bob", "Bob", userService.getUser(bob.getId()).getName());
        expect("Alice is a user", true, userService.checkUserExist(alice.getId()));
        expect("Eve is not a user", false, userService.checkUserExist(eve.getId()));
        expectError("create Alice's user again", UserAlreadyExists.class,
                () -> userService.createUser(alice.getId()));
        expectError("create a user for an unknown account", AccountNotFound.class,
                () -> userService.createUser(unknownId));
        expectError("getUser of unknown id", UserNotFound.class,
                () -> userService.getUser(unknownId));

        // ---------------------------------------------------------------
        section("Artists (created from a user)");
        Artist weekndArtist = artistService.createArtist(weekndUser.getId());
        artistService.createArtist(duaUser.getId());
        expect("artist id is the user id", weekndUser.getId(), weekndArtist.getId());
        expect("artist name", "The Weeknd", weekndArtist.getName());
        expect("getArtist returns Dua Lipa", "Dua Lipa", artistService.getArtist(dua.getId()).getName());
        expect("The Weeknd is an artist", true, artistService.checkArtistExist(weeknd.getId()));
        expect("Alice is not an artist", false, artistService.checkArtistExist(alice.getId()));
        expectError("create The Weeknd's artist again", ArtistAlreadyExists.class,
                () -> artistService.createArtist(weekndUser.getId()));
        expectError("create an artist for an unknown user", UserNotFound.class,
                () -> artistService.createArtist(unknownId));
        expectError("create an artist for Eve (account but no user)", UserNotFound.class,
                () -> artistService.createArtist(eve.getId()));
        expectError("getArtist of unknown id", ArtistNotFound.class,
                () -> artistService.getArtist(unknownId));

        // ---------------------------------------------------------------
        section("Songs of an artist");
        artistService.addSong(weeknd.getId(), "Blinding Lights");
        artistService.addSong(weeknd.getId(), "Save Your Tears");
        artistService.addSong(dua.getId(), "Levitating");
        expect("The Weeknd's songs", Arrays.asList("Blinding Lights", "Save Your Tears"),
                songNames(artistService.getSongs(weeknd.getId())));
        expect("Dua Lipa's songs", Collections.singletonList("Levitating"),
                songNames(artistService.getSongs(dua.getId())));
        expectError("add the same song again", SongAlreadyExists.class,
                () -> artistService.addSong(weeknd.getId(), "Blinding Lights"));
        expectError("add the same song in other letter case", SongAlreadyExists.class,
                () -> artistService.addSong(weeknd.getId(), "blinding lights"));
        expectError("add a song to an unknown artist", ArtistNotFound.class,
                () -> artistService.addSong(unknownId, "Ghost Song"));
        expectError("add a song with a null name", IllegalArgumentException.class,
                () -> artistService.addSong(weeknd.getId(), null));
        expectError("add a song with a blank name", IllegalArgumentException.class,
                () -> artistService.addSong(weeknd.getId(), "  "));
        expect("empty song names were not stored", 2, artistService.getSongs(weeknd.getId()).size());

        Song blinding = artistService.getSong(weeknd.getId(), "blinding lights");
        expect("getSong ignores letter case", "Blinding Lights", blinding.getName());
        expect("song id is assigned", true, blinding.getId() != null);
        expect("song knows its artist", "The Weeknd", blinding.getArtist().getName());
        expectError("getSong that does not exist", SongNotFound.class,
                () -> artistService.getSong(weeknd.getId(), "Starboy"));
        expectError("getSong of unknown artist", ArtistNotFound.class,
                () -> artistService.getSong(unknownId, "Blinding Lights"));

        artistService.removeSong(weeknd.getId(), "Save Your Tears");
        expect("songs after removing Save Your Tears", Collections.singletonList("Blinding Lights"),
                songNames(artistService.getSongs(weeknd.getId())));
        expectError("remove Save Your Tears again", SongNotFound.class,
                () -> artistService.removeSong(weeknd.getId(), "Save Your Tears"));
        expectError("remove a song of an unknown artist", ArtistNotFound.class,
                () -> artistService.removeSong(unknownId, "Blinding Lights"));
        artistService.addSong(weeknd.getId(), "Save Your Tears");
        expect("a song can be added again after removing", Arrays.asList("Blinding Lights", "Save Your Tears"),
                songNames(artistService.getSongs(weeknd.getId())));
        artistService.getSongs(weeknd.getId()).clear();
        expect("clearing the returned song list changes nothing", 2, artistService.getSongs(weeknd.getId()).size());

        Song tears = artistService.getSong(weeknd.getId(), "Save Your Tears");
        Song levitating = artistService.getSong(dua.getId(), "Levitating");

        // ---------------------------------------------------------------
        section("Play and stop (User.playSong / stopSong)");
        expectError("Alice stops with nothing playing", NoSongPlaying.class, aliceUser::stopSong);
        expect("Alice plays Blinding Lights", "Blinding Lights music is playing...",
                captureOutput(() -> aliceUser.playSong(blinding)));
        expect("Alice plays another song: the first one is stopped first",
                "Blinding Lights music is stopped...Save Your Tears music is playing...",
                captureOutput(() -> aliceUser.playSong(tears)));
        expect("Alice stops", "Save Your Tears music is stopped...", captureOutput(aliceUser::stopSong));
        expectError("Alice stops again", NoSongPlaying.class, aliceUser::stopSong);
        expect("stopping twice prints nothing the second time", "", captureOutput(() -> {
            try {
                aliceUser.stopSong();
            } catch (NoSongPlaying ignored) {
                // expected
            }
        }));
        expect("Bob is not affected by Alice's playback", "Levitating music is playing...",
                captureOutput(() -> bobUser.playSong(levitating)));
        expect("Bob stops", "Levitating music is stopped...", captureOutput(bobUser::stopSong));

        // ---------------------------------------------------------------
        section("Playlists: create and list");
        Playlist roadTrip = playlistService.createPlaylist("Road Trip", alice.getId());
        expect("playlist id is assigned", true, roadTrip.getId() != null);
        expect("playlist name", "Road Trip", roadTrip.getName());
        expect("creator is the first user", Collections.singletonList("Alice"), userNames(roadTrip.getUsers()));
        expect("new playlist has no playables", Collections.emptyList(), playableNames(roadTrip.getAllPlayables()));
        Playlist gym = playlistService.createPlaylist("Gym", alice.getId());
        expect("Alice's playlists", Arrays.asList("Road Trip", "Gym"), playlistNames(playlistService.getUserPlaylists(alice.getId())));
        expect("Bob has no playlists yet", Collections.emptyList(), playlistNames(playlistService.getUserPlaylists(bob.getId())));
        expect("unknown user has no playlists", Collections.emptyList(), playlistNames(playlistService.getUserPlaylists(unknownId)));
        playlistService.getUserPlaylists(alice.getId()).clear();
        expect("clearing the returned playlist list changes nothing", 2, playlistService.getUserPlaylists(alice.getId()).size());
        expectError("Eve (account but no user) creates a playlist", UserNotFound.class,
                () -> playlistService.createPlaylist("Ghost List", eve.getId()));
        expectError("unknown user creates a playlist", UserNotFound.class,
                () -> playlistService.createPlaylist("Ghost List", unknownId));
        expect("Eve has no playlists", Collections.emptyList(), playlistNames(playlistService.getUserPlaylists(eve.getId())));
        expectError("playlist with a null name", IllegalArgumentException.class,
                () -> playlistService.createPlaylist(null, alice.getId()));
        expectError("playlist with an empty name", IllegalArgumentException.class,
                () -> playlistService.createPlaylist("", alice.getId()));
        expect("failed creates were not stored", 2, playlistService.getUserPlaylists(alice.getId()).size());

        // ---------------------------------------------------------------
        section("Playlists: playables");
        playlistService.addNewPlayable(alice.getId(), roadTrip.getId(), weeknd.getId(), "Blinding Lights");
        playlistService.addNewPlayable(alice.getId(), roadTrip.getId(), weeknd.getId(), "Save Your Tears");
        playlistService.addNewPlayable(alice.getId(), roadTrip.getId(), dua.getId(), "Levitating");
        expect("Road Trip playables", Arrays.asList("Blinding Lights", "Save Your Tears", "Levitating"),
                playableNames(roadTrip.getAllPlayables()));
        expectError("add Blinding Lights again", PlayableAlreadyExist.class,
                () -> playlistService.addNewPlayable(alice.getId(), roadTrip.getId(), weeknd.getId(), "Blinding Lights"));
        expectError("add a song the artist never added", SongNotFound.class,
                () -> playlistService.addNewPlayable(alice.getId(), roadTrip.getId(), weeknd.getId(), "Starboy"));
        expectError("add a song of an unknown artist", ArtistNotFound.class,
                () -> playlistService.addNewPlayable(alice.getId(), roadTrip.getId(), unknownId, "Blinding Lights"));
        expectError("add a song of Alice (not an artist)", ArtistNotFound.class,
                () -> playlistService.addNewPlayable(alice.getId(), roadTrip.getId(), alice.getId(), "Blinding Lights"));
        expect("rejected songs were not added", 3, roadTrip.getAllPlayables().size());
        expect("Gym is not affected", Collections.emptyList(), playableNames(gym.getAllPlayables()));
        expect("getOnePlayable", "Save Your Tears", roadTrip.getOnePlayable(tears.getId()).getName());
        expectError("getOnePlayable of unknown id", PlayableNotFound.class,
                () -> roadTrip.getOnePlayable(unknownId));
        expectError("Bob (not in the playlist) adds a playable", PlaylistNotFound.class,
                () -> playlistService.addNewPlayable(bob.getId(), roadTrip.getId(), weeknd.getId(), "Blinding Lights"));
        expectError("Alice adds to an unknown playlist", PlaylistNotFound.class,
                () -> playlistService.addNewPlayable(alice.getId(), unknownId, weeknd.getId(), "Blinding Lights"));

        playlistService.removePlayable(alice.getId(), roadTrip.getId(), levitating.getId());
        expect("Road Trip playables after removing Levitating", Arrays.asList("Blinding Lights", "Save Your Tears"),
                playableNames(roadTrip.getAllPlayables()));
        expectError("remove Levitating again", PlayableNotFound.class,
                () -> playlistService.removePlayable(alice.getId(), roadTrip.getId(), levitating.getId()));
        expectError("Dave (not in the playlist) removes a playable", PlaylistNotFound.class,
                () -> playlistService.removePlayable(dave.getId(), roadTrip.getId(), blinding.getId()));
        roadTrip.getAllPlayables().clear();
        expect("clearing the returned playable list changes nothing", 2, roadTrip.getAllPlayables().size());

        // ---------------------------------------------------------------
        section("Playlists: users");
        playlistService.addNewUser(alice.getId(), roadTrip.getId(), bob.getId());
        expect("Road Trip users", Arrays.asList("Alice", "Bob"), userNames(roadTrip.getUsers()));
        expect("Bob now has Road Trip", Collections.singletonList("Road Trip"), playlistNames(playlistService.getUserPlaylists(bob.getId())));
        expect("Bob sees the same playlist", roadTrip.getId(), playlistService.getUserPlaylists(bob.getId()).get(0).getId());
        expectError("add Bob again", UserAlreadyInPlaylist.class,
                () -> playlistService.addNewUser(alice.getId(), roadTrip.getId(), bob.getId()));
        expectError("add the creator Alice again", UserAlreadyInPlaylist.class,
                () -> playlistService.addNewUser(bob.getId(), roadTrip.getId(), alice.getId()));
        expect("failed adds do not duplicate Bob's list", 1, playlistService.getUserPlaylists(bob.getId()).size());
        expectError("add Eve (account but no user)", UserNotFound.class,
                () -> playlistService.addNewUser(alice.getId(), roadTrip.getId(), eve.getId()));
        expectError("add an unknown user", UserNotFound.class,
                () -> playlistService.addNewUser(alice.getId(), roadTrip.getId(), unknownId));
        expect("Road Trip users are unchanged", Arrays.asList("Alice", "Bob"), userNames(roadTrip.getUsers()));
        expect("Eve got no playlist", Collections.emptyList(), playlistNames(playlistService.getUserPlaylists(eve.getId())));

        playlistService.addNewPlayable(bob.getId(), roadTrip.getId(), dua.getId(), "Levitating");
        expect("Bob (a member) adds a playable", Arrays.asList("Blinding Lights", "Save Your Tears", "Levitating"),
                playableNames(roadTrip.getAllPlayables()));
        playlistService.addNewUser(bob.getId(), roadTrip.getId(), carol.getId());
        expect("Bob (a member) adds Carol", Arrays.asList("Alice", "Bob", "Carol"), userNames(roadTrip.getUsers()));
        expect("Carol has Road Trip", Collections.singletonList("Road Trip"), playlistNames(playlistService.getUserPlaylists(carol.getId())));
        expectError("Dave (not in the playlist) adds a user", PlaylistNotFound.class,
                () -> playlistService.addNewUser(dave.getId(), roadTrip.getId(), dave.getId()));

        playlistService.removeUser(alice.getId(), roadTrip.getId(), carol.getId());
        expect("Road Trip users after removing Carol", Arrays.asList("Alice", "Bob"), userNames(roadTrip.getUsers()));
        expect("Carol no longer has Road Trip", Collections.emptyList(), playlistNames(playlistService.getUserPlaylists(carol.getId())));
        expect("Bob still has Road Trip", Collections.singletonList("Road Trip"), playlistNames(playlistService.getUserPlaylists(bob.getId())));
        expectError("Carol (removed) adds a playable", PlaylistNotFound.class,
                () -> playlistService.addNewPlayable(carol.getId(), roadTrip.getId(), weeknd.getId(), "Blinding Lights"));
        expectError("remove Carol again", UserNotInPlaylist.class,
                () -> playlistService.removeUser(alice.getId(), roadTrip.getId(), carol.getId()));
        expectError("Dave (not in the playlist) removes a user", PlaylistNotFound.class,
                () -> playlistService.removeUser(dave.getId(), roadTrip.getId(), bob.getId()));
        expectError("remove a user from an unknown playlist", PlaylistNotFound.class,
                () -> playlistService.removeUser(alice.getId(), unknownId, bob.getId()));
        expect("Alice still has both playlists", Arrays.asList("Road Trip", "Gym"), playlistNames(playlistService.getUserPlaylists(alice.getId())));
        expect("Gym still has only Alice", Collections.singletonList("Alice"), userNames(gym.getUsers()));

        // ---------------------------------------------------------------
        System.out.println();
        System.out.println("=== Result: " + passed + " passed, " + failed + " failed ===");
    }

    // ----- small helpers -----

    private static List<String> songNames(List<Song> songs) {
        List<String> names = new ArrayList<>();
        for (Song song : songs) {
            names.add(song.getName());
        }
        return names;
    }

    private static List<String> playableNames(List<Playable> playables) {
        List<String> names = new ArrayList<>();
        for (Playable playable : playables) {
            names.add(playable.getName());
        }
        return names;
    }

    private static List<String> userNames(List<User> users) {
        List<String> names = new ArrayList<>();
        for (User user : users) {
            names.add(user.getName());
        }
        return names;
    }

    private static List<String> playlistNames(List<Playlist> playlists) {
        List<String> names = new ArrayList<>();
        for (Playlist playlist : playlists) {
            names.add(playlist.getName());
        }
        return names;
    }

    // Song.play() and stop() print without a new line, so the output is captured and compared instead
    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void expect(String scenario, Object expected, Object actual) {
        record(Objects.equals(expected, actual), scenario, String.valueOf(expected), String.valueOf(actual));
    }

    // Runs the action and expects exactly this exception type
    private static void expectError(String scenario, Class<? extends RuntimeException> expected, Runnable action) {
        String actual;
        boolean ok;
        try {
            action.run();
            actual = "no exception";
            ok = false;
        } catch (RuntimeException e) {
            actual = e.getClass().getSimpleName() + " (" + e.getMessage() + ")";
            ok = e.getClass().equals(expected);
        }
        record(ok, scenario, expected.getSimpleName(), actual);
    }

    private static void record(boolean ok, String scenario, String expected, String actual) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println("  " + (ok ? "PASS" : "FAIL") + " | " + scenario
                + " | expected: " + expected + " | actual: " + actual);
    }
}

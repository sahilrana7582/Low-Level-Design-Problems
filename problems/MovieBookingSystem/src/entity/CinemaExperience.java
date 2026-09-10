package entity;

import enums.*;

public class CinemaExperience {

    private final String id;

    private final DisplayType displayType;
    private final Resolution resolution;
    private final AudioSystem audioSystem;

    public CinemaExperience(
            String id,
            DisplayType displayType,
            Resolution resolution,
            AudioSystem audioSystem
    ) {
        this.id = id;
        this.displayType = displayType;
        this.resolution = resolution;
        this.audioSystem = audioSystem;
    }

    public String getId() {
        return id;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public AudioSystem getAudioSystem() {
        return audioSystem;
    }
}
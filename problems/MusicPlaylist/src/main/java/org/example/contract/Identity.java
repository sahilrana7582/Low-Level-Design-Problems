package org.example.contract;

import java.util.UUID;

public interface Identity {
    UUID getId();
    String getName();
    String getEmail();
}

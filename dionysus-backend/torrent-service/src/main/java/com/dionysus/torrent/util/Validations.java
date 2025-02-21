package com.dionysus.torrent.util;

import com.dionysus.torrent.exception.InvalidMagnetLinkException;

public class Validations {

    private Validations() {
    }

    public static void validateMagnetLink(String magnetLink) {
        if ((magnetLink == null) || (!magnetLink.startsWith("http://") && !magnetLink.startsWith("https://"))) {
            throw new InvalidMagnetLinkException("Invalid torrent link format, should start with 'http://' or 'https://'.");
        }
        if (magnetLink.length() > 255) {
            throw new InvalidMagnetLinkException("Magnet link exceeds the allowed length.");
        }
    }
}

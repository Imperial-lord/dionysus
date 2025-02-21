package com.dionysus.torrent.exception;

public class TorrentAlreadyExistsException extends RuntimeException {
    public TorrentAlreadyExistsException(String message) {
        super(message);
    }
}
package com.dionysus.torrent.exception;

public class InvalidMagnetLinkException extends RuntimeException {
    public InvalidMagnetLinkException(String message) {
        super(message);
    }
}
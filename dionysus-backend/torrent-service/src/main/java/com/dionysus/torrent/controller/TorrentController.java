package com.dionysus.torrent.controller;

import com.dionysus.torrent.entity.TorrentEntity;
import com.dionysus.torrent.exception.InvalidMagnetLinkException;
import com.dionysus.torrent.exception.TorrentAlreadyExistsException;
import com.dionysus.torrent.service.TorrentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/torrent")
@RequiredArgsConstructor
public class TorrentController {

    private final TorrentService torrentService;

    @PostMapping("/download")
    public ResponseEntity<TorrentEntity> downloadTorrent(@RequestParam String magnetLink) {
        return ResponseEntity.ok(torrentService.addTorrent(magnetLink));
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<TorrentEntity> getStatus(@PathVariable Long id) {
        Optional<TorrentEntity> torrent = torrentService.getTorrentById(id);
        return torrent.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTorrent(@PathVariable Long id) {
        torrentService.deleteTorrent(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(TorrentAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleTorrentAlreadyExistsException(TorrentAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidMagnetLinkException.class)
    public ResponseEntity<Map<String, String>> handleInvalidMagnetLinkException(InvalidMagnetLinkException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
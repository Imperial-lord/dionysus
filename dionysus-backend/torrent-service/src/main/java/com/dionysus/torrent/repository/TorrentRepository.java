package com.dionysus.torrent.repository;

import com.dionysus.torrent.entity.TorrentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TorrentRepository extends JpaRepository<TorrentEntity, Long> {
    Optional<TorrentEntity> findByMagnetLink(String magnetLink);
}

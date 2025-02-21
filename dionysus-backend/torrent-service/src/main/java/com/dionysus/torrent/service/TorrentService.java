package com.dionysus.torrent.service;

import com.dionysus.torrent.entity.Status;
import com.dionysus.torrent.entity.TorrentEntity;
import com.dionysus.torrent.exception.TorrentAlreadyExistsException;
import com.dionysus.torrent.repository.TorrentRepository;
import com.dionysus.torrent.util.Validations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TorrentService {

    private static final String DOWNLOAD_PATH = "downloads";
    private static final List<Float> PROGRESS_THRESHOLDS = Arrays.asList(5.0f, 40.0f, 70.0f, 90.0f);
    private final TorrentRepository torrentRepository;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<Long, Object> downloadLocks = new ConcurrentHashMap<>();

    public TorrentEntity addTorrent(String magnetLink) {
        // Perform simple validations on magnet link
        Validations.validateMagnetLink(magnetLink);
        if (torrentRepository.findByMagnetLink(magnetLink).isPresent()) {
            throw new TorrentAlreadyExistsException("A torrent with the same magnet link already exists.");
        }

        TorrentEntity torrent = torrentRepository.save(
                TorrentEntity.builder()
                        .magnetLink(magnetLink)
                        .status(Status.DOWNLOADING)
                        .progress(0.0f)
                        .build()
        );
        startDownload(torrent);
        return torrent;
    }

    public Optional<TorrentEntity> getTorrentById(Long id) {
        return torrentRepository.findById(id);
    }

    public void deleteTorrent(Long id) {
        torrentRepository.deleteById(id);
    }

    private void startDownload(TorrentEntity torrent) {
        executorService.submit(() -> {
            Long torrentId = torrent.getId();
            downloadLocks.putIfAbsent(torrentId, new Object());

            try {
                Process process = new ProcessBuilder(
                        "aria2c", "--dir=" + DOWNLOAD_PATH, "--seed-time=0",
                        "--file-allocation=none", torrent.getMagnetLink()).redirectErrorStream(true).start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    monitorDownloadProgress(torrent, reader);
                }

                process.waitFor();
                finalizeDownload(torrent);
            } catch (IOException | InterruptedException e) {
                handleDownloadError(torrent);
                Thread.currentThread().interrupt();
            } finally {
                downloadLocks.remove(torrentId);
            }
        });
    }

    private void monitorDownloadProgress(TorrentEntity torrent, BufferedReader reader) throws IOException {
        float progress;
        int progressThresholdIndex = 0;
        String line;
        String lastDetectedFile = null; // or folder - we'd ideally get a folder from here.

        while ((line = reader.readLine()) != null) {
            progress = extractProgressPercentage(line);
            if (progressThresholdIndex < PROGRESS_THRESHOLDS.size() && progress > PROGRESS_THRESHOLDS.get(progressThresholdIndex)) {
                log.info("Torrent {} progress: {}%", torrent.getId(), progress);
                updateTorrentProgress(torrent, progress);
                progressThresholdIndex++;
            }
            if (line.contains("Download complete")) {
                lastDetectedFile = extractFileName(line);
            }

            // check if completion is actually reached
            if (line.contains("(OK):download completed.")) {
                if (lastDetectedFile != null) {
                    log.info("Torrent {} download completed.", torrent.getId());
                    updateTorrentCompletion(torrent, lastDetectedFile);
                } else {
                    handleDownloadError(torrent);
                }
                break;
            }
        }
    }

    private void finalizeDownload(TorrentEntity torrent) {
        if (torrent.getFilePath() == null) {
            handleDownloadError(torrent);
        }
    }

    private void handleDownloadError(TorrentEntity torrent) {
        synchronized (downloadLocks.get(torrent.getId())) {
            torrent.setStatus(Status.ERROR);
            torrentRepository.save(torrent);
        }
    }

    private void updateTorrentProgress(TorrentEntity torrent, float progress) {
        synchronized (downloadLocks.get(torrent.getId())) {
            torrent.setProgress(progress);
            torrentRepository.save(torrent);
        }
    }

    private void updateTorrentCompletion(TorrentEntity torrent, String filename) {
        synchronized (downloadLocks.get(torrent.getId())) {
            torrent.setFilePath(DOWNLOAD_PATH + "/" + filename);
            torrent.setStatus(Status.COMPLETED);
            torrent.setProgress(100.0f);
            torrentRepository.save(torrent);
        }
    }

    private String extractFileName(String logLine) {
        String[] parts = logLine.split("/");
        return parts[parts.length - 1].trim();
    }

    private float extractProgressPercentage(String logLine) {
        Matcher matcher = Pattern.compile("(\\d+)%").matcher(logLine);
        return matcher.find() ? Float.parseFloat(matcher.group(1)) : 0.0f;
    }
}

package com.ytVed.ytVedDownloader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class DownloadService {

    @Value("${app.download.dir}")
    private String downloadDir;

    @Value("${app.ytdlp.path}")
    private String ytDlpPath;

    @Value("${app.ffmpeg.path}")
    private String ffmpegPath;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(downloadDir));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create download directory", e);
        }
    }

    public Map<String, String> getVideoInfo(String url) throws Exception {
        String execPath = resolveYtDlpPath();

        List<String> command = List.of(
                execPath,
                "--ffmpeg-location", ffmpegPath,
                "--print", "title",
                "--print", "duration_string",
                "--print", "thumbnail",
                "--print", "uploader",
                "--print", "view_count",
                "--no-download",
                url
        );

        List<String> lines = runProcess(command);

        // Filter out WARNING lines
        List<String> cleanLines = lines.stream()
                .filter(l -> !l.startsWith("WARNING") && !l.startsWith("ERROR"))
                .toList();

        System.out.println("Clean lines: " + cleanLines);

        if (cleanLines.size() < 4) {
            throw new RuntimeException("Could not fetch video info. Please check the URL.");
        }

        Map<String, String> info = new LinkedHashMap<>();
        info.put("title",     cleanLines.get(0));
        info.put("duration",  cleanLines.get(1));
        info.put("thumbnail", cleanLines.get(2));
        info.put("uploader",  cleanLines.get(3));
        info.put("views",     cleanLines.size() > 4 ? formatViews(cleanLines.get(4)) : "N/A");
        return info;
    }

    public File downloadVideo(String url, String format, String quality) throws Exception {
        List<String> command = buildDownloadCommand(url, format, quality);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.directory(new File(downloadDir));

        Process process = pb.start();
        String resolvedFilePath = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp] " + line);
                if (line.startsWith("[download] Destination:")) {
                    resolvedFilePath = line.replace("[download] Destination:", "").trim();
                }
                if (line.contains("[Merger] Merging formats into")) {
                    resolvedFilePath = line.replace("[Merger] Merging formats into", "")
                            .replaceAll("\"", "").trim();
                }
                if (line.contains("[ExtractAudio] Destination:")) {
                    resolvedFilePath = line.replace("[ExtractAudio] Destination:", "").trim();
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("yt-dlp exited with error code: " + exitCode);
        }

        if (resolvedFilePath == null || !new File(resolvedFilePath).exists()) {
            resolvedFilePath = findLatestFile();
        }

        if (resolvedFilePath == null) {
            throw new RuntimeException("Download completed but file could not be located.");
        }

        return new File(resolvedFilePath);
    }

    private List<String> buildDownloadCommand(String url, String format, String quality) {
        String execPath = resolveYtDlpPath();

        List<String> cmd = new ArrayList<>();
        cmd.add(execPath);

        cmd.add("--ffmpeg-location");
        cmd.add(ffmpegPath);

        if ("mp3".equalsIgnoreCase(format)) {
            System.out.println("Building MP3 command...");
            cmd.addAll(List.of(
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", "0"
            ));
        } else {
            System.out.println("Building MP4 command with quality: " + quality);
            String formatFilter = buildVideoFormatFilter(quality);
            System.out.println("Format filter: " + formatFilter);
            cmd.addAll(List.of(
                    "-f", formatFilter,
                    "--merge-output-format", "mp4",
                    "--remux-video", "mp4"
            ));
        }

        cmd.addAll(List.of(
                "--restrict-filenames",
                "-o", downloadDir + File.separator + "%(title)s.%(ext)s",
                url
        ));

        System.out.println("Final command: " + String.join(" ", cmd));
        return cmd;
    }

    private String buildVideoFormatFilter(String quality) {
        return switch (quality) {
            case "1080" -> "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/best[height<=1080]";
            case "720"  -> "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[height<=720]";
            case "480"  -> "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[height<=480]";
            case "360"  -> "bestvideo[height<=360][ext=mp4]+bestaudio[ext=m4a]/best[height<=360]";
            default     -> "bestvideo+bestaudio/best";
        };
    }

    private String resolveYtDlpPath() {
        if (ytDlpPath != null && !ytDlpPath.isBlank()) {
            File f = new File(ytDlpPath);
            if (f.exists()) return ytDlpPath;
        }

        String hardcoded = "C:\\Users\\Mohamed\\AppData\\Local\\Microsoft\\WinGet\\" +
                "Packages\\yt-dlp.yt-dlp_Microsoft.Winget.Source_8wekyb3d8bbwe\\" +
                "yt-dlp.exe";
        if (new File(hardcoded).exists()) return hardcoded;

        return "yt-dlp";
    }

    private List<String> runProcess(List<String> command) throws Exception {
        System.out.println("Running: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp] " + line);
                lines.add(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("yt-dlp failed:\n" + String.join("\n", lines));
        }

        return lines;
    }

    private String findLatestFile() throws IOException {
        return Files.list(Paths.get(downloadDir))
                .filter(Files::isRegularFile)
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .orElse(null);
    }

    private String formatViews(String raw) {
        try {
            long count = Long.parseLong(raw.trim());
            if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
            if (count >= 1_000)     return String.format("%.1fK", count / 1_000.0);
            return String.valueOf(count);
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
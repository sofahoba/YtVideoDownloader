package com.ytVed.ytVedDownloader.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DownloadRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
            regexp = "^(https?://)?(www\\.)?(youtube\\.com/(watch\\?v=|shorts/)|youtu\\.be/).+",
            message = "Invalid YouTube URL"
    )
    private String url;

    @Pattern(regexp = "^(mp4|mp3)$", message = "Format must be mp4 or mp3")
    private String format = "mp4";

    @Pattern(regexp = "^(best|1080|720|480|360)$", message = "Invalid quality option")
    private String quality = "best";
}
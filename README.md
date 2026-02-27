# YouTube Video Downloader

A full-stack web application that allows users to extract information and download YouTube videos in various formats (MP4, MP3) and qualities. 

The project consists of a **Spring Boot (Java)** backend that orchestrates the downloading process using `yt-dlp` and `FFmpeg`, and a lightweight **Vanilla JavaScript** frontend.

##  Features
* **Fetch Video Info:** Retrieves title, duration, thumbnail, uploader, and view count before downloading.
* **Video Downloads:** Download MP4 videos in multiple qualities (1080p, 720p, 480p, 360p).
* **Audio Extraction:** Convert and download YouTube videos directly to MP3.
* **Safe File Handling:** Automatically sanitizes filenames to prevent OS-level file saving errors.

---

## Prerequisites

To run this project locally, you must have the following software installed on your machine:

1. **Java Development Kit (JDK):** Version 17 or higher.
2. **Node.js & npm:** Required to serve the frontend.
3. **yt-dlp:** A command-line audio/video downloader. ([Download here](https://github.com/yt-dlp/yt-dlp))
4. **FFmpeg:** Required by `yt-dlp` to merge video and audio streams and convert files to MP3. ([Download here](https://ffmpeg.org/download.html))

---

##  Backend Setup & Configuration (Spring Boot)

The backend uses `application.properties` to locate the required executable files. You **must** update these paths to match your local system.

Open `src/main/resources/application.properties` and configure the following variables:

```properties
# Port the backend will run on
server.port=8080

spring.servlet.multipart.max-file-size=4GB
spring.servlet.multipart.max-request-size=4GB
spring.mvc.async.request-timeout=300000

app.download.dir=${user.home}/yt-downloads
app.frontend.url=http://192.168.1.7:5500

app.ytdlp.path={ADD THE PATH OF yt-dlp.exe}

app.ffmpeg.path=C:\\ffmpeg\\bin #{PUT YOUR PATH OF {ffmpeg\\bin} AT YOUR COMPUTER}
```
*(Note: Use double backslashes `\\` for Windows paths).*

### Running the Backend

Open a terminal in the root directory of the Spring Boot project and run:

Using Maven:
```bash
mvn spring-boot:run
```
*(Or simply run the main application class from your preferred IDE like IntelliJ or Eclipse).*

The backend will start on `http://localhost:8080`.

---

##  Frontend Setup & Running (Vanilla JS)

The frontend is built with pure HTML, CSS, and Vanilla JS. It requires a simple local server to run properly.

1. Open a new terminal.
2. Navigate to your frontend directory (where your `index.html` is located).
3. Run the following command:

```bash
npx serve . -p 5500
```

The frontend will be accessible at `http://192.168.1.7:5500`.

---

##  API Endpoints Summary

If you wish to interact with the backend directly or via Postman, here are the available endpoints:

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/health` | Checks if the API is up and running. |
| `GET` | `/api/v1/info?url={youtube_url}` | Retrieves video metadata. |
| `POST` | `/api/v1/download` | Initiates download and returns the file. |

**Example `POST /api/v1/download` Request Body:**
```json
{
  "url": "https://www.youtube.com/watch?v=...",
  "format": "mp4",
  "quality": "1080"
}
```
*(Formats available: `mp4`, `mp3`)*

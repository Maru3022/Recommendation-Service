package com.example.recommendationservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final Path storage;

    public MediaController(@Value("${fitflow.media.storage:/data/uploads}") String storage) throws IOException {
        this.storage = Paths.get(storage).toAbsolutePath().normalize();
        Files.createDirectories(this.storage);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 25L * 1024 * 1024) {
            throw new IllegalArgumentException("File must be non-empty and no larger than 25 MB");
        }
        String original = file.getOriginalFilename() == null ? "file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        byte[] content = file.getBytes();
        String verifiedContentType = detectAllowedContentType(original, content);
        String safeName = UUID.randomUUID() + "-" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.write(storage.resolve(safeName), content, StandardOpenOption.CREATE_NEW);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "url", "/api/v1/media/" + safeName,
                "name", original,
                "contentType", verifiedContentType));
    }

    @GetMapping("/{name:.+}")
    public ResponseEntity<Resource> download(@PathVariable String name) throws IOException {
        Path file = storage.resolve(Paths.get(name).getFileName().toString()).normalize();
        if (!file.startsWith(storage) || !Files.exists(file)) return ResponseEntity.notFound().build();
        String contentType = Files.probeContentType(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(new UrlResource(file.toUri()));
    }

    private String detectAllowedContentType(String originalName, byte[] content) {
        String name = originalName.toLowerCase(Locale.ROOT);
        if ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && startsWith(content, 0xff, 0xd8, 0xff)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (name.endsWith(".png") && startsWith(content, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (name.endsWith(".webp") && asciiAt(content, 0, "RIFF") && asciiAt(content, 8, "WEBP")) {
            return "image/webp";
        }
        if (name.endsWith(".mp4") && asciiAt(content, 4, "ftyp")) {
            return "video/mp4";
        }
        if (name.endsWith(".webm") && startsWith(content, 0x1a, 0x45, 0xdf, 0xa3)) {
            return "video/webm";
        }
        if (name.endsWith(".pdf") && asciiAt(content, 0, "%PDF-")) {
            return MediaType.APPLICATION_PDF_VALUE;
        }
        if (name.endsWith(".txt") && !containsNul(content)) {
            return MediaType.TEXT_PLAIN_VALUE;
        }
        if ((name.endsWith(".docx") || name.endsWith(".xlsx")) && startsWith(content, 0x50, 0x4b, 0x03, 0x04)) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        throw new IllegalArgumentException("Unsupported or invalid file type");
    }

    private boolean startsWith(byte[] content, int... prefix) {
        if (content.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((content[i] & 0xff) != prefix[i]) return false;
        }
        return true;
    }

    private boolean asciiAt(byte[] content, int offset, String expected) {
        if (content.length < offset + expected.length()) return false;
        for (int i = 0; i < expected.length(); i++) {
            if (content[offset + i] != (byte) expected.charAt(i)) return false;
        }
        return true;
    }

    private boolean containsNul(byte[] content) {
        for (byte value : content) if (value == 0) return true;
        return false;
    }
}

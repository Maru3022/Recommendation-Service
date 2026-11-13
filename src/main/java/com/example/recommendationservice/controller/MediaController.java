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
        String safeName = UUID.randomUUID() + "-" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.copy(file.getInputStream(), storage.resolve(safeName), StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "url", "/api/v1/media/" + safeName,
                "name", original,
                "contentType", file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType()));
    }

    @GetMapping("/{name:.+}")
    public ResponseEntity<Resource> download(@PathVariable String name) throws IOException {
        Path file = storage.resolve(Paths.get(name).getFileName().toString()).normalize();
        if (!file.startsWith(storage) || !Files.exists(file)) return ResponseEntity.notFound().build();
        String contentType = Files.probeContentType(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .body(new UrlResource(file.toUri()));
    }
}

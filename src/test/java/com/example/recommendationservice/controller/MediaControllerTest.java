package com.example.recommendationservice.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaControllerTest {

    @TempDir
    Path storage;

    @Test
    void uploadRejectsActiveSvgContent() throws Exception {
        MediaController controller = new MediaController(storage.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.svg", "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> controller.upload(file));
    }

    @Test
    void uploadRejectsHtmlDisguisedAsImage() throws Exception {
        MediaController controller = new MediaController(storage.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> controller.upload(file));
    }
}

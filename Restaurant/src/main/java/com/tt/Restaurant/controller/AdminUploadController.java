package com.tt.Restaurant.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/admin/api")
public class AdminUploadController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/upload/category-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadCategoryImage(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(BAD_REQUEST, "Only image files are allowed");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String ext = "";

        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) ext = original.substring(dot);

        String filename = UUID.randomUUID() + ext;

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path categoryDir = root.resolve("categories");
        Files.createDirectories(categoryDir);

        Path target = categoryDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // trả về URL để frontend set vào imageUrl trong Category
        String url = "/uploads/categories/" + filename;
        return Map.of("url", url);
    }
}
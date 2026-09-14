package com.mtcrm.storage;

import com.mtcrm.common.api.MessageResponse;
import com.mtcrm.common.exception.NotFoundException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.mtcrm.common.api.PageResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileService files;

    public FileController(FileService files) { this.files = files; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ObjectStorage.StoredObject upload(@RequestParam("file") MultipartFile file) {
        return files.upload(file);
    }

    @GetMapping
    PageResponse<FileService.FileSummary> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return files.list(page, size);
    }

    @GetMapping("/{id}")
    FileService.FileDetails get(@PathVariable UUID id) { return files.get(id); }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','HR','RECRUITER')")
    MessageResponse delete(@RequestParam @NotBlank String key) {
        files.delete(key);
        return new MessageResponse("File deleted");
    }

    @GetMapping("/content/{tenantId}/{filename:.+}")
    ResponseEntity<Resource> content(@PathVariable String tenantId, @PathVariable String filename) {
        var path = files.localContent(tenantId, filename);
        if (!java.nio.file.Files.isRegularFile(path)) throw new NotFoundException("File not found");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(new FileSystemResource(path));
    }
}

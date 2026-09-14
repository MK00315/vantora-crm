package com.mtcrm.storage;

import com.mtcrm.common.exception.NotFoundException;
import com.mtcrm.config.AppProperties;
import com.mtcrm.tenant.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3ObjectStorage implements ObjectStorage {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ObjectStorage(S3Client client, S3Presigner presigner, AppProperties properties) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = properties.storage().s3().bucket();
    }

    @Override
    public StoredObject store(MultipartFile file, String key) {
        LocalObjectStorage.validate(file);
        if (!key.startsWith(TenantContext.require() + "/")) throw new NotFoundException("File not found");
        String original = StorageKeyFactory.originalFilename(file);
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(file.getContentType()).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException ex) { throw new IllegalStateException("Could not upload file", ex); }
        return new StoredObject(key, downloadUrl(key),
                original, file.getContentType(), file.getSize());
    }

    @Override
    public void delete(String key) {
        if (!key.startsWith(TenantContext.require() + "/")) throw new NotFoundException("File not found");
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public String downloadUrl(String key) {
        if (!key.startsWith(TenantContext.require() + "/")) throw new NotFoundException("File not found");
        var download = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build()).build());
        return download.url().toString();
    }
}

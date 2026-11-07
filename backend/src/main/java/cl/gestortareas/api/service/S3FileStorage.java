package cl.gestortareas.api.service;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Component
@ConditionalOnProperty(name = "app.storage.s3.enabled", havingValue = "true")
public class S3FileStorage {
    private final S3Client client;
    private final String bucket;

    public S3FileStorage(@Value("${app.storage.s3.bucket:}") String bucket,
                         @Value("${app.storage.s3.region:us-east-1}") String region,
                         @Value("${app.storage.s3.endpoint:}") String endpoint,
                         @Value("${app.storage.s3.path-style:false}") boolean pathStyle) {
        Assert.hasText(bucket, "Configura app.storage.s3.bucket para habilitar el almacenamiento externo");
        this.bucket = bucket;
        var builder = S3Client.builder().region(Region.of(region)).forcePathStyle(pathStyle)
            .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(15))
                .apiCallAttemptTimeout(Duration.ofSeconds(5)));
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        client = builder.build();
    }

    public void put(String key, String contentType, byte[] data) {
        client.putObject(request -> request.bucket(bucket).key(key).contentType(contentType),
            RequestBody.fromBytes(data));
    }

    public byte[] read(String key) {
        return client.getObjectAsBytes(request -> request.bucket(bucket).key(key)).asByteArray();
    }

    public void delete(String key) {
        client.deleteObject(request -> request.bucket(bucket).key(key));
    }

    @PreDestroy
    void close() {
        client.close();
    }
}
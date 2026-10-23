package bd.cityv1.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class CloudinaryStorageService {

    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final Pattern OWNED_IMAGE = Pattern.compile(
            "v\\d+/(cityfix/(?:before|after|avatars)/[0-9a-f-]{36})\\.[a-zA-Z0-9]+$");

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public CloudinaryStorageService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this(cloudName, apiKey, apiSecret, createRestTemplate());
    }

    CloudinaryStorageService(String cloudName, String apiKey, String apiSecret,
                            RestTemplate restTemplate) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return new RestTemplate(factory);
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        requireConfiguration();
        if (!"before".equals(folder) && !"after".equals(folder) && !"avatars".equals(folder)) {
            throw new IllegalArgumentException("Unknown image folder.");
        }

        // The image goes straight to Cloudinary; no local file or database blob is created.
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return "evidence";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("public_id", "cityfix/" + folder + "/" + UUID.randomUUID());
        body.add("overwrite", "false");

        HttpHeaders headers = authenticatedHeaders(MediaType.MULTIPART_FORM_DATA);
        Map<String, Object> result = post("upload", body, headers);
        Object value = result.get("secure_url");
        if (!(value instanceof String url) || ownedPublicId(url) == null) {
            throw new IOException("Image storage returned an invalid image URL.");
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteQuietly(url);
                    }
                }
            });
        }
        return url;
    }

    public void deleteAfterCommit(String publicUrl) {
        if (ownedPublicId(publicUrl) == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(publicUrl);
                }
            });
        } else {
            deleteQuietly(publicUrl);
        }
    }

    void delete(String publicUrl) throws IOException {
        String publicId = ownedPublicId(publicUrl);
        // Older Supabase URLs remain readable and are never sent to Cloudinary for deletion.
        if (publicId == null) return;
        requireConfiguration();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("public_id", publicId);
        body.add("invalidate", "true");
        Map<String, Object> result = post("destroy", body,
                authenticatedHeaders(MediaType.APPLICATION_FORM_URLENCODED));
        if (!"ok".equals(result.get("result")) && !"not found".equals(result.get("result"))) {
            throw new IOException("Image storage could not delete the image.");
        }
    }

    private void deleteQuietly(String url) {
        try {
            delete(url);
        } catch (IOException ex) {
            log.warn("Cloudinary image cleanup failed; the image may need manual removal.");
        }
    }

    private Map<String, Object> post(String action, MultiValueMap<String, Object> body,
                                     HttpHeaders headers) throws IOException {
        try {
            Map<String, Object> result = restTemplate.exchange(
                    "https://api.cloudinary.com/v1_1/" + cloudName + "/image/" + action,
                    HttpMethod.POST, new HttpEntity<>(body, headers), RESPONSE_TYPE).getBody();
            if (result == null) throw new IOException("Image storage returned an empty response.");
            return result;
        } catch (RestClientException ex) {
            throw new IOException("Image storage is unavailable. Please try again.", ex);
        }
    }

    private HttpHeaders authenticatedHeaders(MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(apiKey, apiSecret);
        headers.setContentType(contentType);
        return headers;
    }

    private String ownedPublicId(String url) {
        String prefix = "https://res.cloudinary.com/" + cloudName + "/image/upload/";
        if (url == null || !url.startsWith(prefix)) return null;
        Matcher matcher = OWNED_IMAGE.matcher(url.substring(prefix.length()));
        return matcher.matches() ? matcher.group(1) : null;
    }

    private void requireConfiguration() throws IOException {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()
                || !cloudName.matches("[a-zA-Z0-9_-]+")) {
            throw new IOException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, "
                    + "CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET.");
        }
    }
}

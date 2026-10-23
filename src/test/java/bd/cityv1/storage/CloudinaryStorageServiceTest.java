package bd.cityv1.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudinaryStorageServiceTest {

    private static final String PUBLIC_ID = "cityfix/before/12345678-1234-1234-1234-123456789abc";
    private static final String IMAGE_URL = "https://res.cloudinary.com/test-cloud/image/upload/v123/"
            + PUBLIC_ID + ".jpg";
    private MockRestServiceServer server;
    private CloudinaryStorageService storage;

    @BeforeEach
    void setUp() {
        RestTemplate template = new RestTemplate();
        server = MockRestServiceServer.bindTo(template).build();
        storage = new CloudinaryStorageService("test-cloud", "test-key", "test-secret", template);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        server.verify();
    }

    @Test
    void uploadsMultipartBytesAndReturnsOnlyTheHttpsUrl() throws Exception {
        expectUpload();
        assertEquals(IMAGE_URL, storage.upload(image(), "before"));
    }

    @Test
    void uploadFailureReturnsAnIOExceptionWithoutReturningAnImagePath() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/upload"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThrows(IOException.class, () -> storage.upload(image(), "before"));
    }

    @Test
    void rejectsMissingConfigurationBeforeSendingARequest() {
        CloudinaryStorageService unconfigured = new CloudinaryStorageService("", "", "", new RestTemplate());
        assertTrue(assertThrows(IOException.class,
                () -> unconfigured.upload(image(), "before")).getMessage().contains("CLOUDINARY"));
    }

    @Test
    void rejectsUnexpectedUploadUrls() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/upload"))
                .andRespond(withSuccess("{\"secure_url\":\"https://other.example/image.jpg\"}",
                        MediaType.APPLICATION_JSON));
        assertThrows(IOException.class, () -> storage.upload(image(), "before"));
    }

    @Test
    void doesNotDeleteLegacyOrOtherAccountsImages() throws Exception {
        storage.delete("https://legacy.supabase.co/storage/v1/object/public/complaints/before/photo.jpg");
        storage.delete(IMAGE_URL.replace("test-cloud", "another-cloud"));
        storage.delete(null);
        storage.delete(IMAGE_URL.replace("cityfix/before", "unrelated/before"));
    }

    @Test
    void deletesByPublicIdWithCdnInvalidation() throws Exception {
        expectDelete();
        storage.delete(IMAGE_URL);
    }

    @Test
    void deletesNewUploadIfTheDatabaseTransactionRollsBack() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        expectUpload();
        expectDelete();
        storage.upload(image(), "before");
        TransactionSynchronizationManager.getSynchronizations().forEach(
                callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    @Test
    void keepsNewUploadWhenTheDatabaseTransactionCommits() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        expectUpload();
        storage.upload(image(), "before");
        TransactionSynchronizationManager.getSynchronizations().forEach(
                callback -> callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
    }

    @Test
    void removesReplacedImageOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        storage.deleteAfterCommit(IMAGE_URL);
        // Register the expected request only now: deletion must not happen before commit.
        expectDelete();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void keepsPreviousImageWhenReplacementTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        storage.deleteAfterCommit(IMAGE_URL);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private void expectUpload() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString("test-key:test-secret".getBytes(StandardCharsets.ISO_8859_1))))
                .andExpect(request -> assertTrue(request.getHeaders().getContentType()
                        .isCompatibleWith(MediaType.MULTIPART_FORM_DATA)))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("name=\"file\""),
                        org.hamcrest.Matchers.containsString("image-bytes"),
                        org.hamcrest.Matchers.containsString("cityfix/before/"),
                        org.hamcrest.Matchers.containsString("name=\"overwrite\""))))
                .andRespond(withSuccess("{\"secure_url\":\"" + IMAGE_URL + "\"}",
                        MediaType.APPLICATION_JSON));
    }

    private void expectDelete() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/destroy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(new org.springframework.util.LinkedMultiValueMap<>() {{
                    add("public_id", PUBLIC_ID);
                    add("invalidate", "true");
                }}))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8));
    }
}

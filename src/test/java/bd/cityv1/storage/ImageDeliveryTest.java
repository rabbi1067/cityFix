package bd.cityv1.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageDeliveryTest {

    private final ImageDelivery images = new ImageDelivery();

    @Test
    void optimizesCloudinaryImagesWithoutChangingTheirPublicIds() {
        assertEquals("https://res.cloudinary.com/demo/image/upload/f_auto,q_auto,c_limit,w_480/v42/cityfix/before/proof.jpg",
                images.url("https://res.cloudinary.com/demo/image/upload/v42/cityfix/before/proof.jpg", 480));
    }

    @Test
    void preservesLegacyAndLocalImageUrls() {
        String legacy = "https://demo.supabase.co/storage/v1/object/public/complaints/proof.png";
        assertEquals(legacy, images.url(legacy, 1200));
        assertEquals("/profile1.jpg", images.url("/profile1.jpg", 256));
        assertNull(images.url(null, 480));
    }

    @Test
    void doesNotApplyTheTransformationTwice() {
        String transformed = images.url("https://res.cloudinary.com/demo/image/upload/v42/proof.jpg", 480);
        assertEquals(transformed, images.url(transformed, 480));
    }
}

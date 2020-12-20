package me.postaddict.instagram.scraper;

import me.postaddict.instagram.scraper.client.InstaClient;
import me.postaddict.instagram.scraper.client.InstaClientFactory;
import me.postaddict.instagram.scraper.client.types.Instagram;
import org.junit.BeforeClass;
import org.junit.Test;


public class RandomInstaTest {

    private static Instagram instagram;

    @BeforeClass
    public static void setUp() {
        InstaClient instaClient = new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        instagram = new Instagram(instaClient);
    }

    @Test
    public void testGetRandomClient() {
        new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
    }

}

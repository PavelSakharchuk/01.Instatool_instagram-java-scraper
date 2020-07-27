package me.postaddict.instagram.scraper;

import me.postaddict.instagram.scraper.client.InstaClient;
import me.postaddict.instagram.scraper.client.InstaClientFactory;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.exception.InstagramAuthException;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.interceptor.FakeBrowserInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.BeforeClass;
import org.junit.Test;


public class RandomInstaTest {

    private static Instagram instagram;

    @BeforeClass
    public static void setUp() {
        InstaClient instaClient = new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        instagram = new Instagram(instaClient);
    }

    @Test(expected = InstagramAuthException.class)
    public void testLoginWithInvalidCredentials() throws Exception {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new FakeBrowserInterceptor(InstaClientFactory.UserAgent.randomUserAgent().getValue()))
                .addInterceptor(new ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        Instagram instagramClient = new Instagram(new InstaClient(httpClient));
        instagramClient.basePage();
        instagramClient.login("1", "2");
    }

    @Test
    public void testGetRandomClient() {
        InstaClient instaClient = new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        instaClient = new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        instaClient = new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        instaClient = new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
        instaClient = new InstaClientFactory(InstaClient.InstaClientType.randomClientType()).getClient();
    }

}

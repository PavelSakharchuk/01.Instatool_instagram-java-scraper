package me.postaddict.instagram.scraper.client;

import me.postaddict.instagram.scraper.ErrorType;
import me.postaddict.instagram.scraper.Instagram;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.exception.InstagramException;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.interceptor.FakeBrowserInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class InstaClientFactory {
    private OkHttpClient httpClient;
    private Instagram instaClient;
    private InstaClientType intaClientType;

    public InstaClientFactory(InstaClientType intaClientType) {
        this.intaClientType = intaClientType;
    }

    public Instagram getRandomClient() {
        this.intaClientType = InstaClientType.randomClientType();
        return getClient();
    }

    public Instagram getClient() {
        UserAgent userAgent = UserAgent.randomUserAgent();
        // TODO: 29.05.2020: Add logger
        System.out.println(String.format("User Agent: [%s] %s", userAgent, userAgent.userAgentValue));
        // TODO: 29.05.2020: Add logger
        System.out.println("Instagram Client Type: " + intaClientType);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // TODO: 08.05.2020: Move to config
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new FakeBrowserInterceptor(userAgent.userAgentValue))
                // TODO: 08.05.2020: Move to config
                .connectTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new ErrorInterceptor());

        switch (intaClientType) {
            case STATELESS:
                break;
            case ANONYMOUS:
            case AUTHENTICATED:
                builder.cookieJar(new DefaultCookieJar(new CookieHashSet()));
                break;
            default:
                String message = String.format("Instagram client is not found:%s", intaClientType);
                throw new InstagramException(message, ErrorType.UNKNOWN_ERROR);
        }

        httpClient = builder.build();
        instaClient = new Instagram(httpClient);
        instaClient = getBasePage();
        return instaClient;
    }

    private Credentials getCredentials() {
        try {
            return new Credentials();
        } catch (IOException e) {
            String message = String.format("Can not create credentials:%n%s", e);
            throw new InstagramException(message, ErrorType.UNKNOWN_ERROR);
        }
    }

    private Instagram getBasePage() {
        try {
            instaClient.basePage();

            if (intaClientType == InstaClientType.AUTHENTICATED) {
                Credentials credentials = getCredentials();
                instaClient.login(credentials.getLogin(), credentials.getEncPassword());
                instaClient.basePage();
            }

        } catch (IOException e) {
            String message = String.format("Can not get base page data:%n%s", e);
            throw new InstagramException(message, ErrorType.UNKNOWN_ERROR);
        }
        return instaClient;
    }

    public enum InstaClientType {
        /**
         * Client without cookies
         */
        STATELESS,
        /**
         * Client with cookies, but without login
         */
        ANONYMOUS,
        /**
         * Client with login
         */
        AUTHENTICATED;

        private static final List<InstaClientType> TYPE_LIST =
                Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = TYPE_LIST.size();
        private static final Random RANDOM = new Random();

        public static InstaClientType randomClientType() {
            return TYPE_LIST.get(RANDOM.nextInt(SIZE));
        }
    }

    public enum UserAgent {
        WIN10_CHROME("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36"),
        WIN10_FIREFOX("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0"),
        OSX_CHROME("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36"),
        OSX_SAFARI("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/602.4.8 (KHTML, like Gecko) Version/10.0.3 Safari/602.4.8"),
        LINUX_FIREFOX("Mozilla/5.0 (X11; Linux x86_64; rv:68.0) Gecko/20100101 Firefox/68.0"),
        WIN10_OPERA("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36 OPR/67.0.3575.137"),
        WIN10_EDGE("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18362"),
        WIN10_IE11("Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko");

        private static final List<UserAgent> USER_AGENT_LIST =
                Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = USER_AGENT_LIST.size();
        private static final Random RANDOM = new Random();
        String userAgentValue;

        UserAgent(String userAgentValue) {
            this.userAgentValue = userAgentValue;
        }

        public static UserAgent randomUserAgent() {
            return USER_AGENT_LIST.get(RANDOM.nextInt(SIZE));
        }
    }
}

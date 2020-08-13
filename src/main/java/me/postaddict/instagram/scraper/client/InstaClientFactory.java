package me.postaddict.instagram.scraper.client;

import me.postaddict.instagram.scraper.ErrorType;
import me.postaddict.instagram.scraper.Instagram;
import me.postaddict.instagram.scraper.Logger;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


public class InstaClientFactory {
    private static final Logger LOGGER = Logger.getInstance();
    private static final Credentials CREDENTIALS = Credentials.getInstance();

    private final InstaClient.InstaClientType instaClientType;
    private OkHttpClient httpClient;

    private final InstaClient instaClient;
    private Instagram instagram;


    public InstaClientFactory(InstaClient.InstaClientType instaClientType) {
        this.instaClientType = instaClientType;
        this.instaClient = new InstaClient(this.httpClient);
        this.instaClient.setInstaClientType(instaClientType);
        this.instagram = new Instagram(instaClient);
    }

    public InstaClient getClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // TODO: 08.05.2020: Move to config
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

        LOGGER.info(String.format("Initial '%s' Instagram Client...", this.instaClient.getInstaClientType()));
        UserAgent userAgent = UserAgent.randomUserAgent();
        LOGGER.info(String.format("User Agent: [%s] %s", userAgent, userAgent.userAgentValue));

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new FakeBrowserInterceptor(userAgent.userAgentValue))
                // TODO: 08.05.2020: Move to config
                .connectTimeout(120, TimeUnit.SECONDS)
                // TODO: 08.05.2020: Move to config
                .readTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(new ErrorInterceptor());

        switch (this.instaClientType) {
            case STATELESS:
                break;
            case ANONYMOUS:
            case AUTHENTICATED:
                builder.cookieJar(new DefaultCookieJar(new CookieHashSet()));
                break;
            default:
                String message = String.format("Instagram client is not found:%s", instaClientType);
                throw new InstagramException(message, ErrorType.UNKNOWN_ERROR);
        }

        httpClient = builder.build();
        instaClient.setHttpClient(httpClient);
        instagram = getBasePage();
        return instaClient;
    }

    private Instagram getBasePage() {
        try {
            instagram.basePage();

            if (instaClientType == InstaClient.InstaClientType.AUTHENTICATED) {
                login();
            }

        } catch (IOException e) {
            String message = String.format("Can not get base page data:%n%s", e);
            throw new InstagramException(message, ErrorType.UNKNOWN_ERROR);
        }
        return instagram;
    }

    private void login() throws IOException {

        User user = CREDENTIALS.getUser();
        instaClient.setCredentialUser(user);

        user.setReloginNumber(user.getReloginNumber() + 1);
        LOGGER.info(String.format("Login [%s]: %s", user.getReloginNumber(), user));

        try {
            // TODO: p.sakharchuk: 06.08.2020: Add Random interval (60 - 300 sec) if rateLimitedDate is before current time more than 5 min
            // TODO: p.sakharchuk: 06.08.2020: Move to property
            final long TIMEOUT_BEFORE_LOGIN_MIN_SEC = 10;
            final long TIMEOUT_BEFORE_LOGIN_MAX_SEC = 30;
            long timeout = ThreadLocalRandom.current().nextLong(TIMEOUT_BEFORE_LOGIN_MIN_SEC, TIMEOUT_BEFORE_LOGIN_MAX_SEC + 1);

            LOGGER.info(String.format("Waiting: %s sec.%n.....", timeout));
            Thread.sleep(timeout * 1000);
            instagram.login(user);

            // TODO: p.sakharchuk: 30.07.2020: Need to check: Is it possible extra
            instagram.basePage();
        } catch (InterruptedException e) {
            String message = String.format("%s can not be logged.", user);
            Thread.currentThread().interrupt();
            throw new InstagramException(message, ErrorType.UNKNOWN_ERROR);
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

        public String getValue() {
            return userAgentValue;
        }

        public static UserAgent randomUserAgent() {
            return USER_AGENT_LIST.get(RANDOM.nextInt(SIZE));
        }
    }
}

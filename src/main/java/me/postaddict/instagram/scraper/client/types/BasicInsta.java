package me.postaddict.instagram.scraper.client.types;

import me.postaddict.instagram.scraper.client.Endpoint;
import me.postaddict.instagram.scraper.interceptor.ErrorType;
import me.postaddict.instagram.scraper.utils.Logger;
import me.postaddict.instagram.scraper.client.InstaClient;
import me.postaddict.instagram.scraper.client.InstaClientFactory;
import me.postaddict.instagram.scraper.client.user.User;
import me.postaddict.instagram.scraper.exception.InstagramException;
import me.postaddict.instagram.scraper.mapper.Mapper;
import me.postaddict.instagram.scraper.mapper.ModelMapper;
import me.postaddict.instagram.scraper.request.DefaultDelayHandler;
import me.postaddict.instagram.scraper.request.DelayHandler;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public abstract class BasicInsta {
    private static final Logger LOGGER = Logger.getInstance();
    protected final DelayHandler delayHandler = new DefaultDelayHandler();
    protected final Mapper mapper = new ModelMapper();
    protected InstaClient instaClient;


    public BasicInsta(InstaClient instaClient) {
        this.instaClient = instaClient;
    }


    protected void getCSRFToken(String body) throws IOException {
        // TODO: p.saharchuk: 19.07.2020: Get JSON from '<script type="text/javascript">window._sharedData ='
        //  and pars it
        String csrf_token = getToken("\"csrf_token\":\"", 32, IOUtils.toInputStream(body, StandardCharsets.UTF_8));
        instaClient.setCsrfToken(csrf_token);
    }

    private String getCSRFToken() {
        LOGGER.debug("Get CSRF Token from cookies");
        for (Cookie cookie : instaClient.getHttpClient().cookieJar().loadForRequest(HttpUrl.parse(Endpoint.BASE_URL))) {
            if ("csrftoken".equals(cookie.name())) {
                return cookie.value();
            }
        }
        return instaClient.getCsrfToken();
    }

    protected void getRolloutHash(String body) throws IOException {
        try {
            // TODO: p.saharchuk: 19.07.2020: Get JSON from '<script type="text/javascript">window._sharedData ='
            //  and pars it
            String rollout_hash = getToken("\"rollout_hash\":\"", 12, IOUtils.toInputStream(body, StandardCharsets.UTF_8));
            instaClient.setRolloutHash(rollout_hash);
        } catch (IOException e) {
            // TODO: p.saharchuk: 19.07.2020: Check it
            instaClient.setRolloutHash("1");
        }
    }

    private String getToken(String seek, int length, InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));

        String line;
        while ((line = in.readLine()) != null) {
            int index = line.indexOf(seek);
            if (index != -1) {
                return line.substring(index + seek.length(), index + seek.length() + length);
            }
        }
        throw new NullPointerException("Couldn't find " + seek);
    }

    protected Request withCsrfToken(Request request) {
        String rollout_hash = instaClient.getRolloutHash();
        return request.newBuilder()
                .addHeader("X-CSRFToken", getCSRFToken())
                .addHeader("X-Instagram-AJAX", (rollout_hash.isEmpty() ? "1" : rollout_hash))
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("X-IG-App-ID", "936619743392459")
                .build();
    }

    public Response executeHttpRequest(Request request) throws IOException {
        String urlLog = decodeUrl(request.url());

        User user = this.instaClient.getCredentialUser();
        if(user != null){
            user.setRequestsNumber(user.getRequestsNumber() + 1);
            urlLog = String.format("[%s:%s]: ", user.getRequestsNumber(), user.getLogin()) + urlLog;
        }

        LOGGER.debug("Request >>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        LOGGER.info(urlLog);
        LOGGER.debug(String.format("headers:%n%s", request.headers()));

        Response response = null;
        // TODO: p.saharchuk: 27.07.2020: Move to properties and refactoring
        final int RETRY_LIMIT = 50;
        final int RETRY_BASE_TIMEOUT_SEC = 300;
        int retry = 0;
        do {
            try {
                response = instaClient.getHttpClient().newCall(request).execute();
            } catch (InstagramException e) {
                retry++;
                LOGGER.warn(String.format("'%s'[%s] Exception for %s", e.getErrorType(), retry, instaClient.getCredentialUser()));
                e.printStackTrace();
                if (e.getErrorType().equals(ErrorType.RATE_LIMITED)) {
                    InstaClient.InstaClientType instaClientType = this.instaClient.getInstaClientType();

                    user.setRateLimitedDate(LocalDateTime.now());
                    user.setRequestsNumber(0);

                    this.instaClient = new InstaClientFactory(instaClientType).getClient();
                }
                if (e.getErrorType().equals(ErrorType.UNKNOWN_ERROR)) {
                    try {
                        long timeout = (long) 1000 * retry * RETRY_BASE_TIMEOUT_SEC;
                        LOGGER.warn(String.format("Waiting: %s sec.%n.....", timeout));
                        Thread.sleep(timeout);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } while (retry < RETRY_LIMIT && response == null);

        LOGGER.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        delayHandler.onEachRequest();
        return response;
    }

    /**
     * Decodes a URL encoded string using `UTF-8`
     */
    private static String decodeUrl(HttpUrl uri) {
        String decodedUrl = String.format("URL can't decoder: %s", uri);
        String url = uri.toString();
        try {
            decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return decodedUrl;
    }
}

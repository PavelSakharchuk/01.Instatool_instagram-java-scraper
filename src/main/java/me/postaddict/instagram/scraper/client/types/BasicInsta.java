package me.postaddict.instagram.scraper.client.types;

import com.google.common.base.Splitter;
import me.postaddict.instagram.scraper.Constants;
import me.postaddict.instagram.scraper.client.Endpoint;
import me.postaddict.instagram.scraper.client.InstaClient;
import me.postaddict.instagram.scraper.client.InstaClientFactory;
import me.postaddict.instagram.scraper.client.user.User;
import me.postaddict.instagram.scraper.exception.InstagramException;
import me.postaddict.instagram.scraper.interceptor.ErrorType;
import me.postaddict.instagram.scraper.mapper.Mapper;
import me.postaddict.instagram.scraper.mapper.ModelMapper;
import me.postaddict.instagram.scraper.request.DefaultDelayHandler;
import me.postaddict.instagram.scraper.request.DelayHandler;
import me.postaddict.instagram.scraper.utils.Logger;
import me.postaddict.instagram.scraper.utils.password.WebEncryption;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;


public abstract class BasicInsta {
    private static final Logger LOGGER = Logger.getInstance();
    protected final DelayHandler delayHandler = new DefaultDelayHandler();
    protected final Mapper mapper = new ModelMapper();
    protected InstaClient instaClient;
    protected WebEncryption webEncryption = new WebEncryption();


    public BasicInsta(InstaClient instaClient) {
        this.instaClient = instaClient;
    }


    protected void getCSRFToken(String body) throws XPathExpressionException {
        String csrfToken = getToken("csrf_token", body);
        instaClient.setCsrfToken(csrfToken);
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

    protected void getRolloutHash(String body) {
        try {
            String rolloutHash = getToken("rollout_hash", body);
            instaClient.setRolloutHash(rolloutHash);
        } catch (XPathExpressionException e) {
//             TODO: p.saharchuk: 19.07.2020: Check it
            instaClient.setRolloutHash("1");
        }
    }

    protected void getWebEncryption(String body) {
        try {
            String keyId = getToken("key_id", body);
            String publicKey = getToken("public_key", body);
            String version = getToken("version", body);

            webEncryption.setKeyId(Integer.parseInt(keyId));
            webEncryption.setPublicKey(publicKey);
            webEncryption.setVersion(Integer.parseInt(version));
        } catch (XPathExpressionException e) {
            webEncryption.setKeyId(Constants.WebEncryption.DEFAULT_KEY_ID);
            webEncryption.setPublicKey(Constants.WebEncryption.DEFAULT_PUBLIC_KEY);
            webEncryption.setVersion(Constants.WebEncryption.DEFAULT_VERSION);
        }
    }

    private String getToken(String tokenName, String body) throws XPathExpressionException {
        final String SCRIPT_TAG = "script";
        final String PARAMETERS_SEPARATOR = ",";
        final String KEY_SEPARATOR = "=";
        final String CONFIG_KEY = "window._sharedData";

        // Get javaScript string with 'csrf_token' from html.
        Document doc = Jsoup.parse(body);
        String scriptData = doc.getElementsByTag(SCRIPT_TAG).stream()
                .filter(script -> script.data().contains(tokenName))
                .findFirst().get().data();

        // Convert javaScript to key/ value map
        Map<String, String> scriptDataMap = Splitter
                .on(PARAMETERS_SEPARATOR)
                .trimResults()
                .omitEmptyStrings()
                .withKeyValueSeparator(KEY_SEPARATOR)
                .split(scriptData);

        // Get 'window._sharedData' value (JSON)
        String scriptJson = scriptDataMap.get(CONFIG_KEY);
        // Convert JSON to key/ value Node
        Node jsonDom = new ModelMapper().getDomModel(IOUtils.toInputStream(scriptJson, Charset.defaultCharset()));
        // Get 'csrf_token' value.
        // Notes: // is short for '/descendant-or-self::node()/' (xPath)
        return XPathFactory.newInstance().newXPath().evaluate("//" + tokenName, jsonDom);
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
        if (user != null) {
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

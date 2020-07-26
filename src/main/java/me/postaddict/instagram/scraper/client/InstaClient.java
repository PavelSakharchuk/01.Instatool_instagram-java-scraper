package me.postaddict.instagram.scraper.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


@Data
@AllArgsConstructor
public class InstaClient {

    private OkHttpClient httpClient;
    /**
     * csrf_token
     */
    private String csrfToken;
    /**
     * rollout_hash
     */
    private String rolloutHash;


    public InstaClient(OkHttpClient httpClient) {
        this(httpClient, "", "");
    }


    public enum InstaClientType {
        /** Client without cookies */
        STATELESS,
        /** Client with cookies, but without login */
        ANONYMOUS,
        /** Client with login */
        AUTHENTICATED;

        private static final List<InstaClientType> TYPE_LIST =
                Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = TYPE_LIST.size();
        private static final Random RANDOM = new Random();

        public static InstaClientType randomClientType() {
            return TYPE_LIST.get(RANDOM.nextInt(SIZE));
        }
    }
}



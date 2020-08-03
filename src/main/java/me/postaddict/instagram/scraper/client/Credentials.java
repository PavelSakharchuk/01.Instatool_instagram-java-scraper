package me.postaddict.instagram.scraper.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public final class Credentials {
    private static final ThreadLocal<Credentials> instancePull = ThreadLocal.withInitial(() -> null);

    private static final String PATH = "credentials.json";
    private List<User> users;


    public Credentials() {
        InputStream is = null;
        try {
            try {
                is = getClass().getClassLoader().getResourceAsStream(PATH);
                if (is == null) {
                    throw new RuntimeException("Can't find credentials file.");
                }
                users = new ObjectMapper().registerModule(new JavaTimeModule())
                        .readValue(is, new TypeReference<List<User>>() {});
                users.forEach(user -> user.setRateLimitedDate(LocalDateTime.now()));
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't read credentials file.");
        }
    }

    /**
     * Implementation of the Singleton pattern
     *
     * @return Logger instance
     */
    public static synchronized Credentials getInstance() {
        return Optional.ofNullable(instancePull.get()).orElseGet(() -> {
            instancePull.set(new Credentials());
            return instancePull.get();
        });
    }

    public User getUser() {
        // TODO: p.sakharchuk: 02.08.2020: properties
        final int RATE_LIMITED_MINUTES = 30;

        List<User> filteredUsers = users.stream()
                .filter(user -> user.getRateLimitedDate().isBefore(LocalDateTime.now().minusMinutes(RATE_LIMITED_MINUTES)))
                .collect(Collectors.toList());

        if (filteredUsers.isEmpty()) {
            filteredUsers = users.stream()
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            Collections.shuffle(filteredUsers);
        }
        return filteredUsers.get(0);
    }
}

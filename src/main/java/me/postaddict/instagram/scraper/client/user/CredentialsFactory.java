package me.postaddict.instagram.scraper.client.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


// TODO: p.saharchuk: 27.12.2020: Singleton with enum
public final class CredentialsFactory {
    private static final ThreadLocal<CredentialsFactory> instancePull = ThreadLocal.withInitial(() -> null);
    private final String PATH = "credentials.yml";
    private final List<User> users;

    public CredentialsFactory() {
        InputStream is = null;
        try {
            try {
                is = getClass().getClassLoader().getResourceAsStream(PATH);
                if (is == null) {
                    throw new RuntimeException("Can't find credentials file.");
                }
                Credentials credentials = new ObjectMapper(new YAMLFactory())
                        .readValue(is, Credentials.class);
                this.users = credentials.getUsers();
                users.forEach(user -> {
                    user.setRateLimitedDate(LocalDateTime.now().minusDays(1));
                });
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
    public static synchronized CredentialsFactory getInstance() {
        return Optional.ofNullable(instancePull.get()).orElseGet(() -> {
            instancePull.set(new CredentialsFactory());
            return instancePull.get();
        });
    }

    public User getUser() {
        // TODO: p.sakharchuk: 02.08.2020: properties
        final int RATE_LIMITED_MINUTES = 5;

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

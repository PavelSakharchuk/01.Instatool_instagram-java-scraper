package me.postaddict.instagram.scraper.client;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.Entity;
import java.time.LocalDateTime;

@Entity
@Data
public class User implements Comparable<User> {
    private String login;
    private String phone;
    private String password;
    private String encPassword;
    private LocalDateTime rateLimitedDate;

    // Need for initialisation in Credentials.class
    public User() {
    }

    public User(String login, String encPassword) {
        this.login = login;
        this.encPassword = encPassword;
        this.phone = null;
        this.password = null;
    }

    /**
     * Compare Users by rateLimitedDate. Need for [list].stream.sorted method
     *
     * @param user - user for compare
     * @return  0 -  if both the date-times represent the same time instance of the day.
     *          1 [true] -  if this User has rateLimitedDate after user.getRateLimitedDate()
     *          -1 [false] - if this User has rateLimitedDate before user.getRateLimitedDate()
     */
    @Override
    public int compareTo(User user) {
        return this.rateLimitedDate.compareTo(user.getRateLimitedDate());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("login", login)
                .append("password", password)
                .append("rateLimitedDate", rateLimitedDate)
                .toString();
    }
}

package me.postaddict.instagram.scraper.client.user;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
public class Credentials {
    private List<User> users;
}

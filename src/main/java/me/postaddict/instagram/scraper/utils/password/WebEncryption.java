package me.postaddict.instagram.scraper.utils.password;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
// TODO: p.saharchuk: 27.12.2020: Singleton
public class WebEncryption {
    private int keyId;
    private String publicKey;
    private int version;
}

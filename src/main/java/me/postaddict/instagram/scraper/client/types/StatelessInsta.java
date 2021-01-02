package me.postaddict.instagram.scraper.client.types;

import me.postaddict.instagram.scraper.client.InstaClient;
import me.postaddict.instagram.scraper.model.Account;
import me.postaddict.instagram.scraper.model.Media;
import me.postaddict.instagram.scraper.model.PageInfo;
import me.postaddict.instagram.scraper.model.PageObject;
import me.postaddict.instagram.scraper.model.Tag;

import java.io.IOException;


public abstract class StatelessInsta extends BasicInsta {
    public StatelessInsta(InstaClient instaClient) {
        super(instaClient);
    }

    public abstract void basePage();

    public abstract Tag getMediasByTag(String tag) throws IOException;

    public abstract Tag getMediasByTag(String tag, int pageCount) throws IOException;

    public abstract Account getAccountByUsername(String username) throws IOException;

    /**
     * @deprecated send tree request, but should be only one
     */
    @Deprecated
    public abstract Account getAccountById(long id) throws IOException;

    public abstract PageObject<Media> getMediaByUserId(long userId) throws IOException;

    public abstract PageObject<Media> getMediaByUserId(long userId, long mediaListSize) throws IOException;

    /**
     * @deprecated use getMediaByUserId
     */
    @Deprecated
    public abstract PageObject<Media> getMedias(String username, int pageCount) throws IOException;

    /**
     * @deprecated use getMediaByUserId, because it is private method by 'pageCursor'
     */
    @Deprecated
    public abstract PageObject<Media> getMedias(long userId, int pageCount, PageInfo pageCursor) throws IOException;

    public abstract Media getMediaByUrl(String url) throws IOException;

    public abstract Media getMediaByCode(String code) throws IOException;
}

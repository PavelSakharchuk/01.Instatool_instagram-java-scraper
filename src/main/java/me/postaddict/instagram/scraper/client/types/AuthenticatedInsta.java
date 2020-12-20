package me.postaddict.instagram.scraper.client.types;

import me.postaddict.instagram.scraper.client.InstaClient;
import me.postaddict.instagram.scraper.client.user.User;
import me.postaddict.instagram.scraper.model.Account;
import me.postaddict.instagram.scraper.model.ActionResponse;
import me.postaddict.instagram.scraper.model.ActivityFeed;
import me.postaddict.instagram.scraper.model.Comment;
import me.postaddict.instagram.scraper.model.PageObject;

import java.io.IOException;

public abstract class AuthenticatedInsta extends AnonymousInsta {

    public AuthenticatedInsta(InstaClient instaClient) {
        super(instaClient);
    }


    public abstract void login(User user) throws IOException;

    public abstract void likeMediaByCode(String code) throws IOException;

    public abstract void unlikeMediaByCode(String code) throws IOException;

    public abstract void followAccountByUsername(String username) throws IOException;

    public abstract void unfollowAccountByUsername(String username) throws IOException;

    public abstract void followAccount(long userId) throws IOException;

    public abstract void unfollowAccount(long userId) throws IOException;

    public abstract ActionResponse<Comment> addMediaComment(String code, String commentText) throws IOException;

    public abstract void deleteMediaComment(String code, String commentId) throws IOException;

    public abstract PageObject<Account> getMediaLikes(String shortcode, int pageCount) throws IOException;

    public abstract PageObject<Account> getFollows(long userId, int pageCount) throws IOException;

    public abstract PageObject<Account> getFollowers(long userId, int pageCount) throws IOException;

    public abstract ActivityFeed getActivityFeed() throws IOException;

    public abstract Long getLoginUserId();
}

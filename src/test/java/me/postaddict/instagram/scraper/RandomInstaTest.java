package me.postaddict.instagram.scraper;

import me.postaddict.instagram.scraper.client.InstaClientFactory;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.exception.InstagramAuthException;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.interceptor.FakeBrowserInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgents;
import me.postaddict.instagram.scraper.model.Account;
import me.postaddict.instagram.scraper.model.ActionResponse;
import me.postaddict.instagram.scraper.model.ActivityFeed;
import me.postaddict.instagram.scraper.model.Comment;
import me.postaddict.instagram.scraper.model.Location;
import me.postaddict.instagram.scraper.model.Media;
import me.postaddict.instagram.scraper.model.PageObject;
import me.postaddict.instagram.scraper.model.Tag;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static me.postaddict.instagram.scraper.ContentCheck.checkAccount;
import static me.postaddict.instagram.scraper.ContentCheck.checkComment;
import static me.postaddict.instagram.scraper.ContentCheck.checkMedia;
import static me.postaddict.instagram.scraper.ContentCheck.checkTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RandomInstaTest {

    private static Instagram client;

    @BeforeClass
    public static void setUp() {
        client = new InstaClientFactory(InstaClientFactory.InstaClientType.randomClientType()).getClient();
    }

    @Test(expected = InstagramAuthException.class)
    public void testLoginWithInvalidCredentials() throws Exception {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new FakeBrowserInterceptor(UserAgents.OSX_CHROME))
                .addInterceptor(new ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        Instagram instagramClient = new Instagram(httpClient);
        instagramClient.basePage();
        instagramClient.login("1", "2");
    }

    @Test
    public void testGetRandomClient() throws Exception {
        client = new InstaClientFactory(InstaClientFactory.InstaClientType.randomClientType()).getClient();
        client = new InstaClientFactory(InstaClientFactory.InstaClientType.randomClientType()).getClient();
        client = new InstaClientFactory(InstaClientFactory.InstaClientType.randomClientType()).getClient();
        client = new InstaClientFactory(InstaClientFactory.InstaClientType.randomClientType()).getClient();
        client = new InstaClientFactory(InstaClientFactory.InstaClientType.randomClientType()).getClient();
    }

    @Test
    public void testGetAccountByUsername() throws Exception {
        Account account = client.getAccountByUsername("kevin");
        assertEquals("kevin", account.getUsername());
        assertTrue(checkAccount(account));
        System.out.println(account);
    }

    @Test
    public void testGetTagByName() throws Exception {
        Tag tag = client.getMediasByTag("corgi");
        assertEquals("corgi", tag.getName());
        assertTrue(checkTag(tag));
        System.out.println(tag);
    }

    @Test
    public void testGetMedias() throws Exception {
        PageObject<Media> medias = client.getMedias("kevin", 2);
        Collection<Media> mediaList = medias.getNodes();
        assertEquals(2 * 12, mediaList.size());
        for (Media media : mediaList) {
            assertTrue(checkMedia(media));
        }
        System.out.println(mediaList);
    }

    @Test
    public void testGetMediaByUrl() throws Exception {
        Media media = client.getMediaByUrl("https://www.instagram.com/p/BHaRdodBouH");
        assertEquals("kevin", media.getOwner().getUsername());
        assertTrue(checkMedia(media));
        System.out.println(media);
    }

    @Test
    public void testGetMediaByCode() throws Exception {
        Media media = client.getMediaByCode("BHaRdodBouH");
        assertEquals("kevin", media.getOwner().getUsername());
        assertTrue(checkMedia(media));
        System.out.println(media);
    }

    @Test
    public void testGetLocationMediasById() throws Exception {
        Location location = client.getLocationMediasById("17326249", 1);
        Collection<Media> list = location.getMediaRating().getMedia().getNodes();
        assertEquals(12, list.size());
        for (Media media : list) {
            assertTrue(checkMedia(media));
        }
        System.out.println(list);
    }

    @Test
    public void testGetMediasByTag() throws Exception {
        Tag tag = client.getMediasByTag("Moscow", 2);
        Collection<Media> list = tag.getMediaRating().getMedia().getNodes();
        assertEquals(2 * 12, list.size());
        for (Media media : list) {
            assertTrue(checkMedia(media));
        }
        System.out.println(list);
    }

    @Test
    public void testGetTopMediasByTag() throws Exception {
        Tag tag = client.getMediasByTag("Sheremetyevo", 1);
        Collection<Media> list = tag.getMediaRating().getTopPosts();
        assertEquals(9, list.size());
        for (Media media : list) {
            assertTrue(checkMedia(media));
        }
        System.out.println(list);
    }

    @Test
    public void testGetCommentsByMediaCode() throws Exception {
        PageObject<Comment> comments = client.getCommentsByMediaCode("BHaRdodBouH", 2);
        Collection<Comment> list = comments.getNodes();
        assertEquals(2 * 12, list.size());
        for (Comment comment : list) {
            assertTrue(checkComment(comment));
        }
        System.out.println(list);
    }

    @Test
    public void testGetIdFromCode() throws Exception {
        String code = MediaUtil.getCodeFromId("1270593720437182847");
        assertEquals("BGiDkHAgBF_", code);
        code = MediaUtil.getCodeFromId("1270593720437182847_3");
        assertEquals("BGiDkHAgBF_", code);
    }

    @Test
    public void testGetCodeFromId() throws Exception {
        String id = MediaUtil.getIdFromCode("BGiDkHAgBF_");
        assertEquals("1270593720437182847", id);
    }

    @Test
    public void testPreviewComments() throws Exception {
        Media media = client.getMediaByCode("");
        System.out.println(media);
        if (media.getCommentCount() > 0) {
            assertTrue(media.getCommentPreview().getNodes().size() > 0);
            for (Comment comment : media.getCommentPreview().getNodes()) {
                assertTrue(checkComment(comment));
            }
        } else {
            assertFalse(media.getCommentPreview().getNodes().size() > 0);
        }
    }

    @Test
    public void testLikeMediaByCode() throws Exception {
        client.likeMediaByCode("PASTE_HERE_MEDIA_CODE");
    }

    @Test
    public void testUnlikeMediaByCode() throws Exception {
        client.unlikeMediaByCode("PASTE_HERE_MEDIA_CODE");
    }

    @Test
    public void testFollowAccountByUsername() throws Exception {
        client.followAccountByUsername("PASTE_HERE_USERNAME");
    }

    @Test
    public void testUnfollowAccountByUsername() throws Exception {
        client.unfollowAccountByUsername("PASTE_HERE_USERNAME");
    }

    @Test
    public void testAddMediaComment() throws Exception {
        ActionResponse<Comment> comment = client.addMediaComment("PASTE_HERE_MEDIA_CODE", "PASTE_COMMENT_TEXT");
        System.out.println(comment);
    }

    @Test
    public void testDeleteMediaComment() throws Exception {
        client.deleteMediaComment("PASTE_HERE_MEDIA_CODE", "PASTE_COMMENT_ID");
    }

    @Test
    public void testGetMediaLikes() throws Exception {
        PageObject<Account> likes = client.getMediaLikes("BaKLiFugkQa", 2);
        assertThat(likes).isNotNull();
        assertThat(likes.getNodes().size()).isEqualTo(400);
    }

    @Test
    public void testGetAllMediaLikes() throws Exception {
        PageObject<Account> likes = client.getMediaLikes("BcfiQOhBSLc", Integer.MAX_VALUE);
        assertThat(likes).isNotNull();
        assertThat(likes.getNodes().size()).isGreaterThan(700);
    }

    @Test
    public void testGetActivityFeed() throws Exception {
        ActivityFeed activityFeed = client.getActivityFeed();
        assertThat(activityFeed).isNotNull();
        assertThat(activityFeed.getCount()).isGreaterThan(3);
        assertThat(activityFeed.getActivities().size()).isGreaterThan(3);
    }

    @Test
    public void testFollows() throws Exception {
        Account account = client.getAccountByUsername("kevin");
        PageObject<Account> follows = client.getFollows(account.getId(), 2);
        assertTrue(follows.getNodes().size() > 390);
    }

    @Test
    public void testFollowers() throws Exception {
        Account account = client.getAccountByUsername("kevin");
        PageObject<Account> followers = client.getFollowers(account.getId(), 1);
        assertEquals(200, followers.getNodes().size());
    }

}

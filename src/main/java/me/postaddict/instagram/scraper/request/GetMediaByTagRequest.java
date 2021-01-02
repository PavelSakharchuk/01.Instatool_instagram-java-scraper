package me.postaddict.instagram.scraper.request;

import me.postaddict.instagram.scraper.client.Endpoint;
import me.postaddict.instagram.scraper.client.InstaClient;
import me.postaddict.instagram.scraper.mapper.Mapper;
import me.postaddict.instagram.scraper.model.PageInfo;
import me.postaddict.instagram.scraper.model.Tag;
import me.postaddict.instagram.scraper.request.parameters.TagName;
import okhttp3.Request;

import java.io.InputStream;

public class GetMediaByTagRequest extends PaginatedRequest<Tag, TagName> {

    public GetMediaByTagRequest(InstaClient instaClient, Mapper mapper, DelayHandler delayHandler) {
        super(instaClient, mapper, delayHandler);
    }

    @Override
    protected Request requestInstagram(TagName requestParameters, PageInfo pageInfo) {
        return new Request.Builder()
                .url(Endpoint.getMediasJsonByTagLink(requestParameters.getTag(), pageInfo.getEndCursor()))
                .header(Endpoint.REFERER, Endpoint.BASE_URL + "/")
                .build();
    }

    @Override
    protected void updateResult(Tag result, Tag current) {
        if(isResultMediaEmpty(result) || isCurrentMediaEmpty(current)){
            return;
        }
        result.getMediaRating().getMedia().getNodes().addAll(current.getMediaRating().getMedia().getNodes());
        if(result.getMediaRating().getMedia().getPageInfo()==null){
            return;
        }
        result.getMediaRating().getMedia().setPageInfo(current.getMediaRating().getMedia().getPageInfo());
    }

    private boolean isResultMediaEmpty(Tag result) {
        return result == null || result.getMediaRating() == null || result.getMediaRating().getMedia() == null
                || result.getMediaRating().getMedia().getNodes() == null;
    }

    private boolean isCurrentMediaEmpty(Tag current) {
        return isResultMediaEmpty(current);
    }

    @Override
    protected PageInfo getPageInfo(Tag current) {
        return current.getMediaRating().getMedia().getPageInfo();
    }

    @Override
    protected Tag mapResponse(InputStream jsonStream) {
        return getMapper().mapTag(jsonStream);
    }
}

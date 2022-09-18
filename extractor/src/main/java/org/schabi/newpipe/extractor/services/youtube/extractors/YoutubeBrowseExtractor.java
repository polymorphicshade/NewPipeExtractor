package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonWriter;

import org.schabi.newpipe.extractor.IInfoItemFilter;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MetaInfo;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.localization.TimeAgoParser;
import org.schabi.newpipe.extractor.search.SearchExtractor;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getKey;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static org.schabi.newpipe.extractor.utils.Utils.UTF_8;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

public class YoutubeBrowseExtractor extends SearchExtractor {
    private JsonObject initialData;

    public YoutubeBrowseExtractor(final StreamingService service,
                                  final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws
            IOException,
            ExtractionException {
        // @formatter:off
        final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(getExtractorLocalization(),
                        getExtractorContentCountry())
                        .value("browseId", "FEwhat_to_watch")
                        .done())
                .getBytes(UTF_8);
        // @formatter:on

        initialData = getJsonPostResponse("browse", body, getExtractorLocalization());
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        final String apiKey;

        try {
            apiKey = getKey();
        } catch (final Exception e) {
            throw new ParsingException("Error extracting Youtube API key");
        }

        return YOUTUBEI_V1_URL + "browse?key=" + apiKey + DISABLE_PRETTY_PRINT_PARAMETER;
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage(final IInfoItemFilter<InfoItem> filter)
            throws IOException, ExtractionException {
        final MultiInfoItemsCollector collector =
                new MultiInfoItemsCollector(getServiceId(), filter);
        final TimeAgoParser timeAgoParser = getTimeAgoParser();
        final JsonArray sections = initialData.getObject("contents")
                .getObject("twoColumnBrowseResultsRenderer").getArray("tabs").getObject(0)
                .getObject("tabRenderer").getObject("content").getObject("richGridRenderer")
                .getArray("contents");

        Page nextPage = null;

        for (final Object section : sections) {
            if (((JsonObject) section).has("richItemRenderer")) {
                final JsonObject videoInfo = ((JsonObject) section)
                        .getObject("richItemRenderer").getObject("content")
                        .getObject("videoRenderer");
                if (videoInfo.isEmpty()) {
                    continue;
                }
                collector.commit(new YoutubeStreamInfoItemExtractor(videoInfo, timeAgoParser));
            } else if (((JsonObject) section).has("continuationItemRenderer")) {
                nextPage = getNextPage(((JsonObject) section)
                        .getObject("continuationItemRenderer"));
            }
        }

        return new InfoItemsPage<>(collector, nextPage);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page,
                                           final IInfoItemFilter<InfoItem> filter)
            throws IOException,
            ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final Localization localization = getExtractorLocalization();
        final MultiInfoItemsCollector collector =
                new MultiInfoItemsCollector(getServiceId(), filter);

        // @formatter:off
        final byte[] json = JsonWriter.string(prepareDesktopJsonBuilder(localization,
                        getExtractorContentCountry())
                        .value("continuation", page.getId())
                        .done())
                .getBytes(UTF_8);
        // @formatter:on

        final String responseBody = getValidJsonResponseBody(getDownloader().post(
                page.getUrl(), new HashMap<>(), json));

        final JsonObject ajaxJson;
        try {
            ajaxJson = JsonParser.object().from(responseBody);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse JSON", e);
        }

        final JsonArray continuationItems = ajaxJson.getArray("onResponseReceivedActions")
                .getObject(0).getObject("appendContinuationItemsAction")
                .getArray("continuationItems");

        getStreams(collector, continuationItems);

        JsonObject continuationItemRenderer = null;

        for (final Object obj : continuationItems) {
            if (((JsonObject) obj).has("continuationItemRenderer")) {
                continuationItemRenderer = ((JsonObject) obj).getObject("continuationItemRenderer");
                break;
            }
        }

        return new InfoItemsPage<>(collector, getNextPage(continuationItemRenderer));
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() throws ParsingException {
        return "";
    }

    @Override
    public boolean isCorrectedSearch() throws ParsingException {
        return false;
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() throws ParsingException {
        return Collections.emptyList();
    }

    private void getStreams(final MultiInfoItemsCollector collector,
                            final JsonArray continuationItems) {
        final TimeAgoParser timeAgoParser = getTimeAgoParser();

        for (final Object continuationItem : continuationItems) {
            final JsonObject videoInfo = ((JsonObject) continuationItem)
                    .getObject("richItemRenderer").getObject("content")
                    .getObject("videoRenderer");
            if (videoInfo.isEmpty()) {
                continue;
            }
            collector.commit(new YoutubeStreamInfoItemExtractor(videoInfo, timeAgoParser));
        }
    }

    private Page getNextPage(final JsonObject continuationItemRenderer) throws IOException,
            ExtractionException {
        if (isNullOrEmpty(continuationItemRenderer)) {
            return null;
        }

        final String token = continuationItemRenderer.getObject("continuationEndpoint")
                .getObject("continuationCommand").getString("token");

        final String url = YOUTUBEI_V1_URL + "browse?key=" + getKey()
                + DISABLE_PRETTY_PRINT_PARAMETER;

        return new Page(url, token);
    }
}

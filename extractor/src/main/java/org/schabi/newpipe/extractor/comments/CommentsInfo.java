package org.schabi.newpipe.extractor.comments;

import org.schabi.newpipe.extractor.IInfoItemFilter;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.utils.ExtractorHelper;

import java.io.IOException;

public final class CommentsInfo extends ListInfo<CommentsInfoItem> {

    private CommentsInfo(
            final int serviceId,
            final ListLinkHandler listUrlIdHandler,
            final String name) {
        super(serviceId, listUrlIdHandler, name);
    }

    public static CommentsInfo getInfo(final String url,
                                       final IInfoItemFilter<CommentsInfoItem> filter)
            throws IOException, ExtractionException {
        return getInfo(NewPipe.getServiceByUrl(url), url, filter);
    }

    public static CommentsInfo getInfo(final StreamingService service, final String url,
                                       final IInfoItemFilter<CommentsInfoItem> filter)
            throws ExtractionException, IOException {
        return getInfo(service.getCommentsExtractor(url), filter);
    }

    public static CommentsInfo getInfo(final CommentsExtractor commentsExtractor,
                                       final IInfoItemFilter<CommentsInfoItem> filter)
            throws IOException, ExtractionException {
        // for services which do not have a comments extractor
        if (commentsExtractor == null) {
            return null;
        }

        commentsExtractor.fetchPage();

        final String name = commentsExtractor.getName();
        final int serviceId = commentsExtractor.getServiceId();
        final ListLinkHandler listUrlIdHandler = commentsExtractor.getLinkHandler();

        final CommentsInfo commentsInfo = new CommentsInfo(serviceId, listUrlIdHandler, name);
        commentsInfo.setCommentsExtractor(commentsExtractor);
        final InfoItemsPage<CommentsInfoItem> initialCommentsPage =
                ExtractorHelper.getItemsPageOrLogError(commentsInfo, commentsExtractor, filter);
        commentsInfo.setCommentsDisabled(commentsExtractor.isCommentsDisabled());
        commentsInfo.setRelatedItems(initialCommentsPage.getItems());
        commentsInfo.setNextPage(initialCommentsPage.getNextPage());

        return commentsInfo;
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final CommentsInfo commentsInfo,
            final Page page,
            final IInfoItemFilter<CommentsInfoItem> filter)
            throws ExtractionException, IOException {
        return getMoreItems(NewPipe.getService(commentsInfo.getServiceId()), commentsInfo.getUrl(),
                page, filter);
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final StreamingService service,
            final CommentsInfo commentsInfo,
            final Page page,
            final IInfoItemFilter<CommentsInfoItem> filter)
            throws IOException, ExtractionException {
        return getMoreItems(service, commentsInfo.getUrl(), page, filter);
    }

    public static InfoItemsPage<CommentsInfoItem> getMoreItems(
            final StreamingService service,
            final String url,
            final Page page,
            final IInfoItemFilter<CommentsInfoItem> filter)
            throws IOException, ExtractionException {
        return service.getCommentsExtractor(url).getPage(page, filter);
    }

    private transient CommentsExtractor commentsExtractor;
    private boolean commentsDisabled = false;

    public CommentsExtractor getCommentsExtractor() {
        return commentsExtractor;
    }

    public void setCommentsExtractor(final CommentsExtractor commentsExtractor) {
        this.commentsExtractor = commentsExtractor;
    }

    /**
     * @apiNote Warning: This method is experimental and may get removed in a future release.
     * @return {@code true} if the comments are disabled otherwise {@code false} (default)
     * @see CommentsExtractor#isCommentsDisabled()
     */
    public boolean isCommentsDisabled() {
        return commentsDisabled;
    }

    /**
     * @apiNote Warning: This method is experimental and may get removed in a future release.
     * @param commentsDisabled {@code true} if the comments are disabled otherwise {@code false}
     */
    public void setCommentsDisabled(final boolean commentsDisabled) {
        this.commentsDisabled = commentsDisabled;
    }
}

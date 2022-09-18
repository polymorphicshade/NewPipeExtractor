package org.schabi.newpipe.extractor.utils;

import org.schabi.newpipe.extractor.IInfoItemFilter;
import org.schabi.newpipe.extractor.Info;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.InfoItemsCollector;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Collections;
import java.util.List;

public final class ExtractorHelper {
    private ExtractorHelper() {
    }

    public static <T extends InfoItem> InfoItemsPage<T> getItemsPageOrLogError(
            final Info info, final ListExtractor<T> extractor, final IInfoItemFilter<T> filter) {
        try {
            final InfoItemsPage<T> page = extractor.getInitialPage(filter);
            info.addAllErrors(page.getErrors());

            return page;
        } catch (final Exception e) {
            info.addError(e);
            return InfoItemsPage.emptyPage();
        }
    }


    public static List<InfoItem> getRelatedItemsOrLogError(
            final StreamInfo info,
            final StreamExtractor extractor,
            final IInfoItemFilter<StreamInfoItem> filter) {
        try {
            final InfoItemsCollector<? extends InfoItem, ?> collector =
                    extractor.getRelatedItems(filter);
            if (collector == null) {
                return Collections.emptyList();
            }
            info.addAllErrors(collector.getErrors());

            //noinspection unchecked
            return (List<InfoItem>) collector.getItems();
        } catch (final Exception e) {
            info.addError(e);
            return Collections.emptyList();
        }
    }

    /**
     * @deprecated Use {@link #getRelatedItemsOrLogError(StreamInfo, StreamExtractor, IInfoItemFilter)}
     */
    @Deprecated
    public static List<InfoItem> getRelatedVideosOrLogError(
            final StreamInfo info,
            final StreamExtractor extractor,
            final IInfoItemFilter<StreamInfoItem> filter) {
        return getRelatedItemsOrLogError(info, extractor, filter);
    }

}

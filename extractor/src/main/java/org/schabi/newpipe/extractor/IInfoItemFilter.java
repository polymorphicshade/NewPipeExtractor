package org.schabi.newpipe.extractor;

public interface IInfoItemFilter<R extends InfoItem> {
    boolean isAllowed(R infoItem);
}

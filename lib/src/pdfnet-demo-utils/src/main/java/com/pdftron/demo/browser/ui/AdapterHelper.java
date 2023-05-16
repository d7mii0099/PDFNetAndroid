package com.pdftron.demo.browser.ui;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.pdftron.demo.utils.ThumbnailWorker;

import java.util.List;

interface AdapterHelper extends ThumbnailWorker.ThumbnailWorkerListener {
    void cancelAllThumbRequests(boolean removePreviewHandler);
    void cancelAllThumbRequests();
    void cleanupResources();
    void setItems(List<MultiItemEntity> items);
    void setLastOpenedFilePosition(int lastOpenedFilePosition);
    void evictFromMemoryCache(String uuid);
}

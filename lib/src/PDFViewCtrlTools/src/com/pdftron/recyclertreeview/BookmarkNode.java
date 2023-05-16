package com.pdftron.recyclertreeview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.Destination;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import static com.pdftron.pdf.Action.e_GoTo;

public class BookmarkNode implements LayoutItemType {
    public static final String PLACEHOLDER_TAG = "PLACEHOLDER";

    private final PDFDoc mPdfDoc;
    private Bookmark mBookmark;
    private String mTitle;
    private int mPageNumber;
    private int mFontStyle = 0; // normal
    public boolean mIsOpen;
    public boolean mIsSelected;

    public BookmarkNode(@Nullable Bookmark bookmark) {
        this(null, bookmark);
    }

    public BookmarkNode(@Nullable PDFDoc pdfDoc, @Nullable Bookmark bookmark) {
        mPdfDoc = pdfDoc;
        mBookmark = bookmark;

        if (bookmark == null || pdfDoc == null) {
            mTitle = PLACEHOLDER_TAG;// this should never be seen by the user
        } else {
            boolean shouldUnlockRead = false;
            try {
                mPdfDoc.lockRead();
                shouldUnlockRead = true;

                mTitle = bookmark.getTitle();
                Action action = bookmark.getAction();
                if (action != null && action.getType() == e_GoTo) {
                    mPageNumber = action.getDest().getPage().getIndex();
                }
                mIsOpen = bookmark.isOpen();
                mFontStyle = bookmark.getFlags();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(mPdfDoc);
                }
            }
        }
    }

    /**
     * Commit moving bookmark
     *
     * @param newParentNode when null, bookmark is added as a root bookmark,
     *                      otherwise it will be added as a child
     */
    public BookmarkNode commitMoveToNewParent(@Nullable BookmarkNode newParentNode) {
        if (mPdfDoc != null && mBookmark != null) {
            boolean shouldUnlock = false;
            try {
                mPdfDoc.lock();
                shouldUnlock = true;

                mBookmark.unlink();
                if (newParentNode != null && newParentNode.getBookmark() != null) {
                    newParentNode.getBookmark().addChild(mBookmark);
                } else {
                    mPdfDoc.addRootBookmark(mBookmark);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mPdfDoc);
                }
            }
        }
        return this;
    }

    public BookmarkNode commitMoveToPrev(@NonNull BookmarkNode nextNode) {
        if (mPdfDoc != null && mBookmark != null && nextNode.getBookmark() != null) {
            boolean shouldUnlock = false;
            try {
                mPdfDoc.lock();
                shouldUnlock = true;

                mBookmark.unlink();
                nextNode.getBookmark().addPrev(mBookmark);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mPdfDoc);
                }
            }
        }
        return this;
    }

    public BookmarkNode commitMoveToNext(@NonNull BookmarkNode prevNode) {
        if (mPdfDoc != null && mBookmark != null && prevNode.getBookmark() != null) {
            boolean shouldUnlock = false;
            try {
                mPdfDoc.lock();
                shouldUnlock = true;

                mBookmark.unlink();
                prevNode.getBookmark().addNext(mBookmark);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mPdfDoc);
                }
            }
        }
        return this;
    }

    public BookmarkNode commitDelete() {
        if (mPdfDoc != null && mBookmark != null) {
            boolean shouldUnlock = false;
            try {
                mPdfDoc.lock();
                shouldUnlock = true;

                mBookmark.delete();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mPdfDoc);
                }
            }
        }
        return this;
    }

    /**
     * Commit adding bookmark
     *
     * @param parentBookmark when null, bookmark is added as a root bookmark,
     *                       otherwise it will be added as a child
     */
    public BookmarkNode commitAdd(@Nullable BookmarkNode parentBookmark) {
        if (mPdfDoc != null) {
            boolean shouldUnlock = false;
            try {
                mPdfDoc.lock();
                shouldUnlock = true;

                Bookmark newBookmark = Bookmark.create(mPdfDoc, this.mTitle);
                this.mBookmark = newBookmark;
                newBookmark.setAction(Action.createGoto(Destination.createFit(mPdfDoc.getPage(this.mPageNumber))));
                if (parentBookmark != null && parentBookmark.getBookmark() != null) {
                    parentBookmark.getBookmark().addChild(newBookmark);
                } else {
                    mPdfDoc.addRootBookmark(newBookmark);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mPdfDoc);
                }
            }
        }
        return this;
    }

    public BookmarkNode commitEditEntry() {
        if (mPdfDoc != null && mBookmark != null) {
            boolean shouldUnlock = false;
            try {
                mPdfDoc.lock();
                shouldUnlock = true;

                mBookmark.setTitle(this.mTitle);
                mBookmark.setAction(Action.createGoto(Destination.createFit(mPdfDoc.getPage(this.mPageNumber))));
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mPdfDoc);
                }
            }
        }
        return this;
    }

    public BookmarkNode commitOpen() {
        if (mPdfDoc != null && mBookmark != null) {
            boolean shouldUnlock = false;
            try {
                mPdfDoc.lock();
                shouldUnlock = true;

                mBookmark.setOpen(this.mIsOpen);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mPdfDoc);
                }
            }
        }
        return this;
    }

    public Bookmark getBookmark() {
        return mBookmark;
    }

    public BookmarkNode setPageNumber(int pageNumber) {
        this.mPageNumber = pageNumber;
        return this;
    }

    public int getPageNumber() {
        return this.mPageNumber;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public BookmarkNode setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    public boolean isOpen() {
        return this.mIsOpen;
    }

    public BookmarkNode setOpen(boolean open) {
        this.mIsOpen = open;
        return this;
    }

    public int getFontStyle() {
        return mFontStyle;
    }

    @Override
    public int getLayoutId() {
        return R.layout.tree_view_list_item;
    }
}

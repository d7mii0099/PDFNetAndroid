package com.pdftron.demo.browser.ui;
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseViewHolder;
import com.pdftron.demo.R;
import com.pdftron.demo.browser.model.FileItem;
import com.pdftron.demo.browser.model.FolderItem;
import com.pdftron.pdf.utils.Utils;

public class StickyHeader extends FrameLayout implements View.OnClickListener {
    private static final String TAG = StickyHeader.class.getSimpleName();
    private final ScrollListener scrollListener = new ScrollListener();
    View header;
    TextView title;
    AppCompatImageView foldingBtn;
    private RecyclerView mRecyclerView;
    private AllFilesListAdapter mAdapter;
    private View view;
    private int headerPos = 0;
    private boolean isDisabled = false;
    private FileBrowserTheme mTheme;
    private String mBackupFolderPath;
    private boolean mIsBackupFolder;

    public StickyHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public StickyHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        mBackupFolderPath = context.getExternalFilesDir(null).toString();
        mTheme = FileBrowserTheme.fromContext(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.sticky_header, this);
        header = view.findViewById(R.id.header_view);
        title = view.findViewById(R.id.title);
        foldingBtn = view.findViewById(R.id.folding_btn);

        mRecyclerView = null;
        mAdapter = null;
        header.setOnClickListener(this);
        header.setBackgroundColor(mTheme.headerBackgroundColor);
        title.setTextColor(mTheme.headerTextColor);
        foldingBtn.setColorFilter(mTheme.headerChevronColor);
        if (Utils.isLollipop()) {
            header.setElevation(3);
        }
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mAdapter = (AllFilesListAdapter) mRecyclerView.getAdapter();

        mRecyclerView.addOnScrollListener(scrollListener);
    }

    public void disable() {
        isDisabled = true;
        header.setVisibility(View.GONE);
    }

    public void enable(int position) {
        isDisabled = false;
        if (locateHeaderPos(position)) {
            updateStickyHeader();
        }
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    private boolean locateHeaderPos(int firstChildPos) {
        int viewType = mAdapter.getItemViewType(firstChildPos);
        switch (viewType) {
            case AllFilesListAdapter.VIEW_TYPE_HEADER:
                headerPos = firstChildPos;
                break;
            case AllFilesListAdapter.VIEW_TYPE_CONTENT:
                headerPos = mAdapter.findHeaderPositionFromChild(firstChildPos);
                break;
        }
        return true;
    }

    private void updateStickyHeader() {
        if (mAdapter == null || headerPos < 0) {
            return;
        }
        if (!(mAdapter.getItem(headerPos) instanceof FolderItem)) {
            return;
        }
        FolderItem fileHeader = (FolderItem) mAdapter.getItem(headerPos);
        if (fileHeader == null) {
            return;
        }
        setTitle(fileHeader.filePath);
        View firstChild = mRecyclerView.getChildAt(0);
        int firstChildAdapterPos = mRecyclerView.getChildAdapterPosition(firstChild);
        if (fileHeader.isCollapsed() || (firstChildAdapterPos == headerPos && firstChild.getBottom() == header.getLayoutParams().height)) {
            header.setVisibility(View.GONE);
        } else {
            View nextChild = mRecyclerView.getChildAt(1);
            header.setVisibility(View.VISIBLE);
            if (nextChild != null) {
                RecyclerView.ViewHolder vh = mRecyclerView.getChildViewHolder(nextChild);
                if (vh instanceof BaseViewHolder && firstChild.getBottom() < header.getLayoutParams().height) {
                    header.setTranslationY(firstChild.getBottom() - header.getLayoutParams().height);
                } else {
                    header.setTranslationY(0);
                }
            } else {
                header.setTranslationY(0);
            }
            foldingBtn.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
        }
    }

    @Override
    public void onClick(View v) {
        if (isDisabled) {
            return;
        }
        if (mAdapter != null) {
            mRecyclerView.scrollToPosition(headerPos);
            mAdapter.clickHeader(getContext(), headerPos);
            updateStickyHeader();
        }
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (mAdapter == null || mRecyclerView == null || isDisabled) {
                header.setVisibility(View.GONE);
                return;
            }
            View firstChild = mRecyclerView.getChildAt(0);
            if (firstChild == null) {
                header.setVisibility(View.GONE);
                return;
            }
            int firstChildPos = mRecyclerView.getChildAdapterPosition(firstChild);
            if (firstChildPos == RecyclerView.NO_POSITION) {
                header.setVisibility(View.GONE);
                return;
            }
            int viewType = mAdapter.getItemViewType(firstChildPos);
            header.setVisibility(View.VISIBLE);
            switch (viewType) {
                case AllFilesListAdapter.VIEW_TYPE_HEADER:
                    FolderItem fileHeader = (FolderItem) mAdapter.getItem(firstChildPos);
                    if (fileHeader == null) {
                        return;
                    }

                    if (fileHeader.isCollapsed()) {
                        header.setVisibility(View.GONE);
                        return;
                    }
                    setTitle(fileHeader.filePath);
                    if (fileHeader.isCollapsed()) {
                        foldingBtn.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
                    } else {
                        foldingBtn.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
                    }
                    header.setTranslationY(0);
                    if (firstChild.getBottom() == header.getLayoutParams().height) {
                        header.setVisibility(View.GONE);
                    }
                    headerPos = firstChildPos;
                    break;
                case AllFilesListAdapter.VIEW_TYPE_CONTENT:
                    if (firstChildPos == mAdapter.getItemCount() - 1) {
                        return;
                    }
                    FileItem contentInfo = (FileItem) mAdapter.getItem(firstChildPos);
                    if (contentInfo == null) {
                        return;
                    }

                    int nextChildType = mAdapter.getItemViewType(firstChildPos + 1);

                    String fileParent = contentInfo.fileParent;
                    if (nextChildType == AllFilesListAdapter.VIEW_TYPE_HEADER) {    // last content
                        if (dy >= 0) { // scrolling up
                            if (dy == 0 && !title.getText().equals(fileParent)) {
                                title.setText(fileParent);
                                foldingBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_arrow_up_black_24dp));
                                headerPos = mAdapter.findHeaderPositionFromChild(firstChildPos);
                            }
                            if (firstChild.getBottom() <= header.getLayoutParams().height) {
                                header.setTranslationY(firstChild.getBottom() - header.getLayoutParams().height);
                            } else {
                                header.setTranslationY(0);
                            }
                        } else {   // scrolling down
                            int startingPoint = -header.getLayoutParams().height;
                            if (!title.getText().equals(fileParent)) {
                                header.setTranslationY(startingPoint + header.getTranslationY());
                                title.setText(fileParent);
                                foldingBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_arrow_up_black_24dp));
                                headerPos = mAdapter.findHeaderPositionFromChild(firstChildPos);
                            }

                            if (header.getTranslationY() < startingPoint) {
                                header.setTranslationY(startingPoint);
                            } else if (header.getTranslationY() < 0) {
                                header.setTranslationY(header.getTranslationY() - dy);
                            }
                        }
                    } else {
                        if (!title.getText().equals(fileParent)) {
                            title.setText(fileParent);
                            foldingBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_arrow_up_black_24dp));
                            headerPos = mAdapter.findHeaderPositionFromChild(firstChildPos);
                        }
                        if (header.getTranslationY() != 0) {
                            header.setTranslationY(0);
                        }
                    }

                    if (header.getTranslationY() > 0) {
                        header.setTranslationY(0);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void setBackupFolder(boolean backupFolder) {
        mIsBackupFolder = backupFolder;
    }

    private void setTitle(String titleValue) {
        if (mIsBackupFolder && mBackupFolderPath != null) {
            title.setText(titleValue.replace(mBackupFolderPath, ""));
        } else {
            title.setText(titleValue);
        }
    }
}
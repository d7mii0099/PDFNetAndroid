package com.pdftron.recyclertreeview;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.OutlineDialogFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.BookmarkManager;

import java.util.ArrayList;

public class EditOutlineMoveDialog extends DialogFragment {

    private RelativeLayout mNavigation;
    private TextView mNavigationText;
    protected int mDialogTitle;
    private final ArrayList<Bookmark> mBookmarks;
    private OutlineDialogFragment.Theme mTheme;
    private final PDFViewCtrl mPdfViewCtrl;
    private OutlineMoveAdapter mOutlineMoveAdapter;
    private final EditOutlineMoveClickListener mListener;
    private Bookmark mCurrentBookmark;
    private final Bookmark mBookmarkToMove;
    protected RelativeLayout mEmptyView;

    public interface EditOutlineMoveClickListener {
        // success boolean to close dialog
        boolean moveBookmarkSelected(Bookmark selectedBookmark);
    }

    public EditOutlineMoveDialog(int title, ArrayList<Bookmark> bookmarks, PDFViewCtrl pdfViewCtrl, EditOutlineMoveClickListener listener, Bookmark bookmarkToMove) {
        this.mBookmarks = bookmarks;
        this.mDialogTitle = title;
        this.mPdfViewCtrl = pdfViewCtrl;
        this.mListener = listener;
        this.mBookmarkToMove = bookmarkToMove;
        this.mBookmarks.remove(mBookmarkToMove);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mTheme = OutlineDialogFragment.Theme.fromContext(requireContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.edit_pdf_outline_move_to_entry);
        View viewInflated = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_outline_move, (ViewGroup) getView(), false);

        mEmptyView = viewInflated.findViewById(R.id.empty_list_message_layout);
        TextView emptyListBodyText = viewInflated.findViewById(R.id.empty_list_body);
        emptyListBodyText.setTextColor(mTheme.textColor);
        TextView emptyListSecondaryText = viewInflated.findViewById(R.id.empty_list_secondary_text);
        emptyListSecondaryText.setTextColor(mTheme.secondaryTextColor);

        RecyclerView moveRecyclerView = viewInflated.findViewById(R.id.edit_outline_move_recyclerview);
        mOutlineMoveAdapter = new OutlineMoveAdapter(mBookmarks);

        mNavigation = viewInflated.findViewById(R.id.edit_outline_move_navigation);
        mNavigationText = mNavigation.findViewById(R.id.edit_outline_move_navigation_title);
        ImageView navigationIcon = mNavigation.findViewById(R.id.edit_outline_move_navigation_back);
        mNavigation.setVisibility(View.GONE);

        mNavigation.setBackgroundColor(mTheme.headerBackgroundColor);
        mNavigationText.setTextColor(mTheme.headerTextColor);
        navigationIcon.setColorFilter(mTheme.headerTextColor, PorterDuff.Mode.SRC_IN);

        moveRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        moveRecyclerView.setAdapter(mOutlineMoveAdapter);
        builder.setView(viewInflated);

        builder.setPositiveButton(getString(R.string.action_move), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean isSuccessfulMove = mListener.moveBookmarkSelected(mCurrentBookmark);
                if (isSuccessfulMove) {
                    dialog.cancel();
                }
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                try {
                    if (mCurrentBookmark != null) {
                        if (mCurrentBookmark.getIndent() > 0) {
                            mPdfViewCtrl.docLockRead(new PDFViewCtrl.LockRunnable() {
                                @Override
                                public void run() throws Exception {
                                    mNavigationText.setText(mCurrentBookmark.getTitle());
                                }
                            });
                            mNavigation.setVisibility(View.VISIBLE);
                        } else { // the file has incorrect outline indent
                            mCurrentBookmark = null;
                        }
                    }
                } catch (Exception e) {
                    mCurrentBookmark = null;
                }
                mNavigation.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        navigateToParentBookmark();
                    }
                });
            }
        });
        return alertDialog;
    }

    private void navigateToParentBookmark() {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return;
        }

        try {
            mPdfViewCtrl.docLockRead(new PDFViewCtrl.LockRunnable() {
                @Override
                public void run() throws Exception {
                    ArrayList<Bookmark> temp = null;
                    if (mCurrentBookmark != null && mCurrentBookmark.getIndent() > 0) {
                        mCurrentBookmark = mCurrentBookmark.getParent();
                        temp = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), mCurrentBookmark.getFirstChild());
                        mNavigationText.setText(mCurrentBookmark.getTitle());
                        if (mCurrentBookmark.getIndent() <= 0) {
                            mNavigation.setVisibility(View.GONE);
                        }
                    } else {
                        temp = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), null);
                        mCurrentBookmark = null;
                        mNavigation.setVisibility(View.GONE);
                    }

                    mBookmarks.clear();
                    temp.remove(mBookmarkToMove);

                    mBookmarks.addAll(temp);
                    setMessageVisibility();
                    mOutlineMoveAdapter.notifyDataSetChanged();
                }
            });
        } catch (Exception e) {
            mCurrentBookmark = null;
        }
    }

    private void setMessageVisibility() {
        if (mBookmarks.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private class OutlineMoveAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final ArrayList<Bookmark> mBookmarks;

        OutlineMoveAdapter(ArrayList<Bookmark> objects) {
            mBookmarks = objects;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(
                    R.layout.controls_fragment_outline_listview_item, parent, false);
            return new OutlineMoveAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            OutlineMoveAdapter.ViewHolder viewHolder = (OutlineMoveAdapter.ViewHolder) holder;
            viewHolder.bookmarkArrow.setOnClickListener(navClickListener(position));
            viewHolder.bookmarkText.setOnClickListener(navClickListener(position));

            Bookmark bookmark = mBookmarks.get(position);
            try {
                mPdfViewCtrl.docLockRead(new PDFViewCtrl.LockRunnable() {
                    @Override
                    public void run() throws Exception {
                        viewHolder.bookmarkText.setText(bookmark.getTitle());
                    }
                });
                viewHolder.bookmarkArrow.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {

            }
        }

        private View.OnClickListener navClickListener(int position) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCurrentBookmark = mBookmarks.get(position);
                    if (mCurrentBookmark == null) {
                        return;
                    }

                    if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null) {
                        try {
                            mPdfViewCtrl.docLockRead(new PDFViewCtrl.LockRunnable() {
                                @Override
                                public void run() throws Exception {
                                    ArrayList<Bookmark> temp = new ArrayList<>();
                                    if (mCurrentBookmark.hasChildren()) {
                                        temp = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), mCurrentBookmark.getFirstChild());
                                    } else {
                                        temp = new ArrayList<>();
                                    }

                                    mBookmarks.clear();
                                    temp.remove(mBookmarkToMove);
                                    mBookmarks.addAll(temp);
                                    setMessageVisibility();
                                    notifyDataSetChanged();
                                    mNavigation.setVisibility(View.VISIBLE);
                                    mNavigationText.setText(mCurrentBookmark.getTitle());
                                }
                            });
                        } catch (Exception ignored) {

                        }
                    }
                }
            };
        }

        @Override
        public int getItemCount() {
            if (mBookmarks != null) {
                return mBookmarks.size();
            } else {
                return 0;
            }
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            TextView bookmarkText;
            ImageView bookmarkArrow;

            ViewHolder(View itemView) {
                super(itemView);

                bookmarkText = itemView.findViewById(R.id.control_outline_listview_item_textview);
                bookmarkArrow = itemView.findViewById(R.id.control_outline_listview_item_imageview);

                bookmarkText.setTextColor(mTheme.textColor);
                bookmarkArrow.setColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_IN);
            }
        }
    }
}

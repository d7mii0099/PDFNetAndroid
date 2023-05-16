package com.pdftron.recyclertreeview;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.OutlineDialogFragment;
import com.pdftron.pdf.utils.BookmarkManager;

import java.util.ArrayList;
import java.util.List;

public class OutlineTreeViewAdapter<T extends LayoutItemType> extends TreeViewAdapter<T> {

    private OnBookmarkTreeNodeListener mOnTreeNodeListener;
    private boolean mEditingOutline = false;
    // theme
    private OutlineDialogFragment.Theme mTheme;

    public void setOnTreeNodeListener(OnBookmarkTreeNodeListener onTreeNodeListener) {
        this.mOnTreeNodeListener = onTreeNodeListener;
    }

    public OutlineTreeViewAdapter(List<TreeNode<T>> nodes, List<? extends TreeViewBinder> viewBinders, PDFViewCtrl pdfViewCtrl, float scale) {
        super(nodes, viewBinders, pdfViewCtrl, scale);
    }

    @Override
    public int getItemViewType(int position) {
        return mDisplayNodes.get(position).getContent().getLayoutId();
    }

    public void enableEditOutline() {
        mEditingOutline = true;
        notifyDataSetChanged();
    }

    public void disableEditOutline() {
        mEditingOutline = false;
        notifyDataSetChanged();
    }

    public boolean isEditingOutline() {
        return mEditingOutline;
    }

    public void setTheme(OutlineDialogFragment.Theme theme) {
        mTheme = theme;
    }

    @Override
    public void setNodeTreeNode(PDFViewCtrl pdfViewCtrl, TreeNode<T> selectedNode, boolean isSearchMode) {
        setBookMarkTreeNode(pdfViewCtrl, (TreeNode<BookmarkNode>) selectedNode, isSearchMode);
    }

    public static void setBookMarkTreeNode(PDFViewCtrl pdfViewCtrl, TreeNode<BookmarkNode> selectedNode, boolean isSearchMode) {
        List<TreeNode<BookmarkNode>> childNodeList = selectedNode.getChildList();
        if (childNodeList.size() > 0) {
            BookmarkNode child = childNodeList.get(0).getContent();
            if (child.getTitle().equals(BookmarkNode.PLACEHOLDER_TAG)) { //if placeholder then get children for first time
                BookmarkNode selectedBookmark = selectedNode.getContent();
                List<TreeNode<BookmarkNode>> treeNodes = buildBookmarkTreeNodeList(pdfViewCtrl, selectedBookmark.getBookmark(), isSearchMode);
                selectedNode.setChildList(treeNodes);
            }
        }
    }

    public static List<TreeNode<BookmarkNode>> buildBookmarkTreeNodeList(PDFViewCtrl pdfViewCtrl, Bookmark bookmark, boolean isSearchMode) {
        List<TreeNode<BookmarkNode>> treeNodeList = new ArrayList<>();
        if (pdfViewCtrl != null) {
            boolean shouldUnlockRead = false;
            try {
                pdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                List<Bookmark> bookmarkList = BookmarkManager.getBookmarkList(pdfViewCtrl.getDoc(), bookmark.getFirstChild());
                for (Bookmark child : bookmarkList) {
                    BookmarkNode bookmarkNode = new BookmarkNode(pdfViewCtrl.getDoc(), child);
                    TreeNode<BookmarkNode> temp = new TreeNode<>(bookmarkNode);
                    if (child.hasChildren()) {
                        if (child.isOpen() && !isSearchMode) {
                            List<TreeNode<BookmarkNode>> childNodes = buildBookmarkTreeNodeList(pdfViewCtrl, child, isSearchMode);
                            temp.setChildList(childNodes);
                            temp.expand();
                        } else {
                            addPlaceHolderNode(temp);
                        }
                    }
                    treeNodeList.add(temp);
                }
            } catch (Exception ignored) {

            } finally {
                if (shouldUnlockRead) {
                    pdfViewCtrl.docUnlockRead();
                }
            }
        }

        return treeNodeList;
    }

    public static void addPlaceHolderNode(TreeNode<BookmarkNode> treeNode) {
        BookmarkNode childBookMarkNode = new BookmarkNode(null);
        TreeNode<BookmarkNode> childNode = new TreeNode<>(childBookMarkNode);
        treeNode.addChild(childNode); // add one for now and get all if they expand the list
    }

    @Nullable
    public TreeNode<T> findNode(@NonNull Bookmark bookmark) {
        for (TreeNode<T> node : mDisplayNodes) {
            if (bookmark.equals(((BookmarkNode) node.getContent()).getBookmark())) {
                return node;
            }
        }
        return null;
    }

    public void onItemDrop(RecyclerView.ViewHolder holder, int fromPosition, int toPosition) {
        // commit changes
        toPosition = validateToPositionSize(toPosition);

        if (mDragStartNode != null) {
            T draggedBookmarkNode = mDragStartNode.getContent();
            // edge case (move to first)
            if (isFirstInLevel(toPosition) && mDisplayNodes.size() > toPosition) {
                // find next item and add as prev
                int nextItem = toPosition;
                if ((toPosition + 1) < mDisplayNodes.size()) {
                    nextItem = toPosition + 1;
                }
                TreeNode<T> nextNode = mDisplayNodes.get(nextItem);
                BookmarkNode nextBookmarkNode = (BookmarkNode) nextNode.getContent();
                ((BookmarkNode) draggedBookmarkNode).commitMoveToPrev(nextBookmarkNode);
            } else {
                // find previous item and add as next
                TreeNode<T> prevNode = mDisplayNodes.get(toPosition - 1);
                BookmarkNode prevBookmarkNode = (BookmarkNode) prevNode.getContent();
                ((BookmarkNode) draggedBookmarkNode).commitMoveToNext(prevBookmarkNode);
            }
        }

        if (mDraggedItemExpanded) {
            TreeNode<T> draggedNode = mDisplayNodes.get(toPosition);
            draggedNode.expand();
            int positionStart = getExpandableStartPosition(draggedNode);
            notifyItemRangeInserted(positionStart, addChildNodes(draggedNode, positionStart));
        }
        mDraggedItemExpanded = false;
        if (toPosition > mDragStartPosition) {//down
            notifyItemMovedDown(toPosition, mDragStartPosition);
        } else {
            notifyItemMovedUp(toPosition, mDragStartPosition);
        }
        mDragStartPosition = -1;
    }

    public void expandDisplayNodesNodes(boolean isSearchMode) {
        List<TreeNode<T>> tempList = new ArrayList<>(mDisplayNodes);
        for (TreeNode<T> currentNode : tempList) {
            int positionStart = getExpandableStartPosition(currentNode);
            if (!currentNode.isExpand()) {
                // update state on bookmark
                ((BookmarkNode) currentNode.getContent()).setOpen(true).commitOpen();
                // ui
                setBookMarkTreeNode(mPdfViewCtrl, (TreeNode<BookmarkNode>) currentNode, isSearchMode);
                notifyItemRangeInserted(positionStart, addChildNodes(currentNode, positionStart));
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            Bundle b = (Bundle) payloads.get(0);
            for (String key : b.keySet()) {
                switch (key) {
                    case KEY_IS_EXPAND:
                        if (mOnTreeNodeListener != null)
                            mOnTreeNodeListener.onToggle(b.getBoolean(key), holder);
                        break;
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        setArrowMargins(holder, position);
        TreeNode<BookmarkNode> selectedNode = (TreeNode<BookmarkNode>) mDisplayNodes.get(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position >= mDisplayNodes.size() || position < 0) {
                    return;
                }
                if (mOnTreeNodeListener != null) {
                    mOnTreeNodeListener.onClick(selectedNode, holder);
                }
            }
        });
        for (TreeViewBinder viewBinder : mViewBinders) {
            if (viewBinder.getLayoutId() == mDisplayNodes.get(position).getContent().getLayoutId()) {
                try {
                    viewBinder.bindView(holder, position, mDisplayNodes.get(position));
                } catch (PDFNetException e) {
                    e.printStackTrace();
                }
            }
        }
        if (holder instanceof BookmarkNodeBinder.ViewHolder) {
            TreeNode<BookmarkNode> treeNode = (TreeNode<BookmarkNode>) mDisplayNodes.get(position);
            if (mEditingOutline) {
                ((BookmarkNodeBinder.ViewHolder) holder).getCheckBox().setVisibility(View.VISIBLE);
                ((BookmarkNodeBinder.ViewHolder) holder).getIvDrag().setVisibility(mSelectionCount == 0 ? View.VISIBLE : View.INVISIBLE);
                ((BookmarkNodeBinder.ViewHolder) holder).getTvPageNumber().setVisibility(View.GONE);

                // check box state
                ((BookmarkNodeBinder.ViewHolder) holder).getCheckBox().setChecked(treeNode.getContent().mIsSelected);
            } else {
                ((BookmarkNodeBinder.ViewHolder) holder).getCheckBox().setVisibility(View.GONE);
                ((BookmarkNodeBinder.ViewHolder) holder).getIvDrag().setVisibility(View.INVISIBLE);
                ((BookmarkNodeBinder.ViewHolder) holder).getTvPageNumber().setVisibility(View.VISIBLE);
            }
            if (mIsSearchMode) {
                ((BookmarkNodeBinder.ViewHolder) holder).getIvArrow().setVisibility(View.INVISIBLE);
            }

            if (mTheme != null) {
                ((BookmarkNodeBinder.ViewHolder) holder).getIvDrag().setColorFilter(mTheme.secondaryTextColor);
                ((BookmarkNodeBinder.ViewHolder) holder).getIvArrow().setColorFilter(mTheme.iconColor);
                ((BookmarkNodeBinder.ViewHolder) holder).getTvName().setTextColor(mTheme.textColor);
                int backgroundColour = treeNode.getContent().mIsSelected ? mTheme.selectedBackgroundColor : mTheme.backgroundColor;
                holder.itemView.setBackgroundColor(backgroundColour);
            }

            setFontStyle(((BookmarkNodeBinder.ViewHolder) holder).getTvName(), treeNode.getContent().getFontStyle());
        }
    }

    private void setFontStyle(TextView textView, Integer fontStyle) {
        switch (fontStyle) {
            case 0:
                textView.setTypeface(Typeface.create(textView.getTypeface(), Typeface.NORMAL));
                break;
            case 1:
                textView.setTypeface(Typeface.create(textView.getTypeface(), Typeface.ITALIC));
                break;
            case 2:
                textView.setTypeface(Typeface.create(textView.getTypeface(), Typeface.BOLD));
                break;
            case 3:
                textView.setTypeface(Typeface.create(textView.getTypeface(), Typeface.BOLD_ITALIC));
                break;
        }
    }

    @Override
    protected void setArrowMargins(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BookmarkNodeBinder.ViewHolder) {
            float padding_16dp = (16 * mScale + 0.5f);
            View arrow = ((BookmarkNodeBinder.ViewHolder) holder).getIvArrow();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) arrow.getLayoutParams();
            params.setMargins((int) (mDisplayNodes.get(position).getHeight() * padding_16dp), 3, 3, 3);
            arrow.setLayoutParams(params);
        }
    }

    @Override
    protected void collapseAllDisplayNodes() {
        // Back up the nodes are displaying.
        List<TreeNode<T>> temp = backupDisplayNodes();
        //find all root nodes.
        List<TreeNode<T>> roots = new ArrayList<>();
        for (TreeNode<T> displayNode : mDisplayNodes) {
            if (displayNode.isRoot()) {
                roots.add(displayNode);
            }
            ((TreeNode<BookmarkNode>) displayNode).getContent().setOpen(false).commitOpen();
        }
        //Close all root nodes.
        for (TreeNode<T> root : roots) {
            if (root.isExpand())
                removeChildNodes(root);
        }
        notifyDiff(temp);
    }

    @Override
    protected void removeFromParent(TreeNode<T> selected, TreeNode<T> parent) {
        parent.getChildList().remove(selected);
        int parentPosition = getSelectedPosition(parent);
        // collapse parent if all children have been removed to avoid items being added to this parent after an item is dragged below it (drag drop logic adds dragged item next next expanded item above it if its in range(child count))
        if (parent.getChildList() == null || parent.getChildList().isEmpty()) {
            parent.collapse();
            ((BookmarkNode) parent.getContent()).setOpen(false).commitOpen();
        }
        notifyItemChanged(parentPosition);
        selected.setParent(null);
    }

    public interface OnBookmarkTreeNodeListener {
        /**
         * called when TreeNodes were clicked.
         *
         * @return weather consume the click event.
         */
        boolean onClick(TreeNode<BookmarkNode> node, RecyclerView.ViewHolder holder);

        /**
         * called when TreeNodes were toggle.
         *
         * @param isExpand the status of TreeNodes after being toggled.
         */
        void onToggle(boolean isExpand, RecyclerView.ViewHolder holder);
    }
}

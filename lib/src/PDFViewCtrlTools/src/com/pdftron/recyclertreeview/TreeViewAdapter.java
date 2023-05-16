package com.pdftron.recyclertreeview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.PDFViewCtrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * https://github.com/TellH/RecyclerTreeView
 * Modified by PDFTron
 */

public abstract class TreeViewAdapter<T extends LayoutItemType> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    protected static final String KEY_IS_EXPAND = "IS_EXPAND";
    protected List<? extends TreeViewBinder> mViewBinders;
    protected ArrayList<TreeNode<T>> mDisplayNodes;
    private boolean mToCollapseChild;
    protected boolean mDraggedItemExpanded = false;
    protected float mScale;
    protected PDFViewCtrl mPdfViewCtrl;
    protected int mSelectionCount = 0;
    protected int mDragStartPosition = -1;
    protected boolean mIsSearchMode = false;

    // drag and drop
    protected TreeNode<T> mDragStartNode;

    public TreeViewAdapter(List<TreeNode<T>> nodes, List<? extends TreeViewBinder> viewBinders, PDFViewCtrl pdfViewCtrl, float scale) {
        mDisplayNodes = new ArrayList<>();
        if (nodes != null) {
            findDisplayNodes(nodes);
        }
        this.mViewBinders = viewBinders;
        mPdfViewCtrl = pdfViewCtrl;
        this.mScale = scale;
    }

    public void setItems(List<TreeNode<T>> nodes) {
        mDisplayNodes.clear();
        if (nodes != null)
            findDisplayNodes(nodes);

        notifyDataSetChanged();
    }

    protected void findDisplayNodes(List<TreeNode<T>> nodes) {
        for (TreeNode<T> node : nodes) {
            mDisplayNodes.add(node);
            if (!node.isLeaf() && node.isExpand())
                findDisplayNodes(node.getChildList());
        }
    }

    @Override
    public long getItemId(int position) {
        return mDisplayNodes.get(position).hashCode();
    }

    @Override
    public abstract int getItemViewType(int position);

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);

        RecyclerView.ViewHolder viewHolder = null;
        if (mViewBinders.size() == 1)
            viewHolder = mViewBinders.get(0).provideViewHolder(v);
        for (TreeViewBinder viewBinder : mViewBinders) {
            if (viewBinder.getLayoutId() == viewType)
                viewHolder = viewBinder.provideViewHolder(v);
        }
        viewHolder = mViewBinders.get(0).provideViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    }

    public void setSelectionCount(int count) {
        mSelectionCount = count;
    }

    public int getExpandableStartPosition(TreeNode<T> selectedNode) {
        return mDisplayNodes.indexOf(selectedNode) + 1;
    }

    public int getSelectedPosition(TreeNode<T> selectedNode) {
        return mDisplayNodes.indexOf(selectedNode);
    }

    public void setIsSearchMode(boolean isSearchMode) {
        mIsSearchMode = isSearchMode;
    }

    public void expandAllNodes(TreeNode<T> node) {
        TreeNode<T> selectedNode = node;
        if (selectedNode.isLeaf())
            return;
        // This TreeNode was locked to click.
        if (selectedNode.isLocked()) return;
        boolean isExpand = selectedNode.isExpand();
        int positionStart = mDisplayNodes.indexOf(selectedNode) + 1;
        if (!isExpand) {
            notifyItemRangeInserted(positionStart, addChildNodes(selectedNode, positionStart));
        } else {
            notifyItemRangeRemoved(positionStart, removeChildNodes(selectedNode, true));
        }
    }

    private void expandNode(RecyclerView.ViewHolder holder) {
        TreeNode<T> selectedNode = mDisplayNodes.get(holder.getLayoutPosition());
        int positionStart = mDisplayNodes.indexOf(selectedNode) + 1;
        notifyItemRangeInserted(positionStart, addChildNodes(selectedNode, positionStart));
    }

    public void addToRoot(TreeNode<T> newTreeNode) {
        mDisplayNodes.add(newTreeNode);
        notifyItemInserted(mDisplayNodes.size());
    }

    public int removeNode(TreeNode<T> node) {
        if (node.getParent() != null) {
            node.getParent().getChildList().remove(node);
        }
        if (node.isLeaf()) {
            mDisplayNodes.remove(node);
            return 1;
        }
        // first remove all children of target node
        List<TreeNode<T>> childList = node.getChildList();
        int removeChildCount = childList.size();
        mDisplayNodes.removeAll(childList);
        for (TreeNode<T> child : childList) {
            if (child.isExpand()) {
                child.toggle();
                removeChildCount += removeNode(child);
            }
        }
        // then remove target node
        mDisplayNodes.remove(node);
        removeChildCount += 1;
        if (node.isExpand()) {
            node.toggle();
        }
        return removeChildCount;
    }

    public void moveChildNode(@Nullable TreeNode<T> newParent, @NonNull TreeNode<T> node, boolean isSearchMode) {
        removeNode(node);
        if (newParent != null) {
            addChildNode(newParent, node, isSearchMode);
        } else {
            node.setParent(null);
            addToRoot(node);
        }
        notifyDataSetChanged();
    }

    public void addChildNode(TreeNode<T> parent, TreeNode<T> newNode, boolean isSearchMode) {
        parent.addChild(newNode);
        int selectedPos = getSelectedPosition(parent);
        if (!parent.isExpand()) {
            int startIndex = getExpandableStartPosition(parent);
            setNodeTreeNode(mPdfViewCtrl, parent, isSearchMode);
            parent.expand();
            notifyItemChanged(selectedPos);
            notifyItemRangeInserted(startIndex, addChildNodes(parent, startIndex));
        } else {
            int childPos = selectedPos + parent.getChildList().size();
            mDisplayNodes.add(childPos, newNode);
            notifyItemInserted(childPos);
        }
    }

    public abstract void setNodeTreeNode(PDFViewCtrl pdfViewCtrl, TreeNode<T> selectedNode, boolean isSearchMode);

    public int addChildNodes(TreeNode<T> pNode, int startIndex) {
        List<TreeNode<T>> childList = pNode.getChildList();
        int addChildCount = 0;
        for (TreeNode<T> treeNode : childList) {
            mDisplayNodes.add(startIndex + addChildCount++, treeNode);
            if (treeNode.isExpand()) {
                addChildCount += addChildNodes(treeNode, startIndex + addChildCount);
            }
        }
        if (!pNode.isExpand())
            pNode.toggle();
        return addChildCount;
    }

    protected int removeChildNodes(TreeNode<T> pNode) {
        return removeChildNodes(pNode, true);
    }

    public int removeChildNodes(TreeNode<T> pNode, boolean shouldToggle) {
        if (pNode.isLeaf())
            return 0;
        List<TreeNode<T>> childList = pNode.getChildList();
        int removeChildCount = childList.size();
        mDisplayNodes.removeAll(childList);
        for (TreeNode<T> child : childList) {
            if (child.isExpand()) {
                if (mToCollapseChild)
                    child.toggle();
                removeChildCount += removeChildNodes(child, false);
            }
        }
        if (shouldToggle)
            pNode.toggle();
        return removeChildCount;
    }

    @Override
    public int getItemCount() {
        return mDisplayNodes == null ? 0 : mDisplayNodes.size();
    }

    public void ifCollapseChildWhileCollapseParent(boolean toCollapseChild) {
        this.mToCollapseChild = toCollapseChild;
    }

    public void onItemDrag(TreeNode<T> treeNode, int position) {
        mDragStartNode = treeNode;
    }

    protected int validateToPositionSize(int toPosition) {
        if (toPosition >= mDisplayNodes.size()) {
            return mDisplayNodes.size() - 1;
        }
        return toPosition;
    }

    public abstract void onItemDrop(RecyclerView.ViewHolder holder, int fromPosition, int toPosition);

    protected void notifyItemMovedUp(int toPosition, int fromPosition) {
        int startPos = 0;
        int endPos = fromPosition + 1;
        if ((toPosition - 1) >= 0) {
            startPos = (toPosition - 1);
        }
        if ((fromPosition + 1) >= mDisplayNodes.size()) {
            endPos = mDisplayNodes.size();
        }
        int itemCount = endPos - startPos;
        notifyItemRangeChanged(startPos, itemCount);
    }

    protected void notifyItemMovedDown(int toPosition, int fromPosition) {
        int startPos = 0;
        int endPos = toPosition + 1;
        if ((fromPosition - 1) >= 0) {
            startPos = (fromPosition - 1);
        }
        if (endPos >= mDisplayNodes.size()) {
            endPos = mDisplayNodes.size();
        }
        int itemCount = endPos - startPos;
        notifyItemRangeChanged(startPos, itemCount);
    }

    protected boolean isFirstInLevel(int toPosition) {
        if (toPosition == 0) {
            return true;
        }
        TreeNode<T> node = mDisplayNodes.get(toPosition);
        TreeNode<T> parentNode = node.getParent();
        if (parentNode != null && parentNode.getChildList() != null && parentNode.getChildList().size() > 0) {
            TreeNode<T> first = parentNode.getChildList().get(0);
            return first == node;
        }
        return false;
    }

    public void refresh(List<TreeNode<T>> treeNodes) {
        mDisplayNodes.clear();
        findDisplayNodes(treeNodes);
        notifyDataSetChanged();
    }

    public Iterator<TreeNode<T>> getDisplayNodesIterator() {
        return mDisplayNodes.iterator();
    }

    protected void notifyDiff(final List<TreeNode<T>> temp) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return temp.size();
            }

            @Override
            public int getNewListSize() {
                return mDisplayNodes.size();
            }

            // judge if the same items
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return TreeViewAdapter.this.areItemsTheSame(temp.get(oldItemPosition), mDisplayNodes.get(newItemPosition));
            }

            // if they are the same items, whether the contents has bean changed.
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return TreeViewAdapter.this.areContentsTheSame(temp.get(oldItemPosition), mDisplayNodes.get(newItemPosition));
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                return TreeViewAdapter.this.getChangePayload(temp.get(oldItemPosition), mDisplayNodes.get(newItemPosition));
            }
        });
        diffResult.dispatchUpdatesTo(this);
    }

    private Object getChangePayload(TreeNode<T> oldNode, TreeNode<T> newNode) {
        Bundle diffBundle = new Bundle();
        if (newNode.isExpand() != oldNode.isExpand()) {
            diffBundle.putBoolean(KEY_IS_EXPAND, newNode.isExpand());
        }
        if (diffBundle.size() == 0)
            return null;
        return diffBundle;
    }

    // For DiffUtil, if they are the same items, whether the contents has bean changed.
    private boolean areContentsTheSame(TreeNode<T> oldNode, TreeNode<T> newNode) {
        return oldNode.getContent() != null && oldNode.getContent().equals(newNode.getContent())
                && oldNode.isExpand() == newNode.isExpand();
    }

    // judge if the same item for DiffUtil
    private boolean areItemsTheSame(TreeNode<T> oldNode, TreeNode<T> newNode) {
        return oldNode.getContent() != null && oldNode.getContent().equals(newNode.getContent());
    }

    /**
     * collapse all root nodes.
     */
    protected abstract void collapseAllDisplayNodes();

    public void expandNodes(List<TreeNode<T>> rootNodeList) {
        for (TreeNode<T> currentNode : rootNodeList) {
            expandAllNodes(currentNode);
            expandNodes(currentNode.getChildList());
        }
    }

    public abstract void expandDisplayNodesNodes(boolean isSearchMode);

    public void expandAll(List<TreeNode<T>> rootNode) {
        TreeNode<T> selectedNode = rootNode.get(1);
        if (selectedNode.isLocked()) return;
        boolean isExpand = selectedNode.isExpand();
        int positionStart = mDisplayNodes.indexOf(selectedNode) + 1;
        if (!isExpand) {
            notifyItemRangeInserted(positionStart, addChildNodes(selectedNode, positionStart));
        }
    }

    @NonNull
    protected List<TreeNode<T>> backupDisplayNodes() {
        List<TreeNode<T>> temp = new ArrayList<>();
        for (TreeNode<T> displayNode : mDisplayNodes) {
            try {
                temp.add(displayNode.clone());
            } catch (CloneNotSupportedException e) {
                temp.add(displayNode);
            }
        }
        return temp;
    }

    private int indexCheck(int position) {
        if (position < 0) {
            return 0;
        } else if (position > mDisplayNodes.size()) {
            return mDisplayNodes.size();
        }
        return position;
    }

    public void itemMoved(RecyclerView.ViewHolder holder, int fromPosition, int toPosition) {
        fromPosition = indexCheck(fromPosition);
        toPosition = indexCheck(toPosition);

        TreeNode<T> selected = mDisplayNodes.get(fromPosition);
        boolean isExpand = selected.isExpand();
        if (mDragStartPosition == -1) {
            mDragStartPosition = fromPosition;
        }
        if (isExpand) {
            mDraggedItemExpanded = true;
            collapseNode(selected);
        }

        toPosition = validateToPositionSize(toPosition);

        boolean isMovingDown = (toPosition > fromPosition);
        checkIfInsideExpandedNode(selected, selected.getParent(), toPosition, isMovingDown);
        setArrowMargins(holder, toPosition);

        Collections.swap(mDisplayNodes, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    private void checkIfInsideExpandedNode(TreeNode<T> selected, TreeNode<T> parent, int toPosition, boolean isMovingDown) {
        if (parent != null) {
            removeFromParent(selected, parent);
        }
        if (toPosition > 0) { //if moved to position 0, nothing to move into so remove parent if any
            for (int nodePosition = toPosition; nodePosition >= 0; nodePosition--) {
                if (nodePosition >= mDisplayNodes.size()) {
                    continue;
                }
                TreeNode<T> node = mDisplayNodes.get(nodePosition);
                if (node.isExpand()) { // only care if its expanded
                    int range;
                    int moveTo;
                    if (isMovingDown) {
                        range = ((getParentChildRange(node) + nodePosition) + 1);//range plus one to see if the item is below parent
                        moveTo = toPosition + 1;
                    } else {
                        range = getParentChildRange(node) + nodePosition;
                        moveTo = toPosition;
                    }
                    if (moveTo > nodePosition && toPosition <= range) {// check if inside range of expanded node and set expanded node as new parent
                        int addChildToPos = isMovingDown ? (toPosition - nodePosition) : (toPosition - nodePosition) - 1;
                        node.addChildAtPos(addChildToPos, selected);
                        selected.setParent(node);
                        break;
                    }
                }
            }
        }
    }

    private int getParentChildRange(TreeNode<T> parent) {
        int range = 0;
        List<TreeNode<T>> parentsChildList = parent.getChildList();
        for (TreeNode<T> treeNode : parentsChildList) {
            if (treeNode.isExpand()) {
                range += (getParentChildRange(treeNode) + 1); //get child count and add this child as one position
            } else {
                range++;
            }
        }
        return range;
    }

    protected abstract void removeFromParent(TreeNode<T> selected, TreeNode<T> parent);

    protected abstract void setArrowMargins(RecyclerView.ViewHolder holder, int position);

    public void findAndCollapseNode(int position) {
        TreeNode<T> selected = mDisplayNodes.get(position);
        collapseNode(selected);
    }

    public void findAndExpandNode(int position) {
        TreeNode<T> selected = mDisplayNodes.get(position);
        ArrayList<TreeNode<T>> list = new ArrayList<>();
        list.add(selected);
        expandAll(list);
    }

    public void collapseNode(TreeNode<T> pNode) {
        List<TreeNode<T>> temp = backupDisplayNodes();
        removeChildNodes(pNode);
        notifyDiff(temp);
    }

    public void collapseBrotherNode(TreeNode<T> pNode) {
        List<TreeNode<T>> temp = backupDisplayNodes();
        if (pNode.isRoot()) {
            List<TreeNode<T>> roots = new ArrayList<>();
            for (TreeNode<T> displayNode : mDisplayNodes) {
                if (displayNode.isRoot())
                    roots.add(displayNode);
            }
            //Close all root nodes.
            for (TreeNode<T> root : roots) {
                if (root.isExpand() && !root.equals(pNode))
                    removeChildNodes(root);
            }
        } else {
            TreeNode<T> parent = pNode.getParent();
            if (parent == null)
                return;
            List<TreeNode<T>> childList = parent.getChildList();
            for (TreeNode<T> node : childList) {
                if (node.equals(pNode) || !node.isExpand())
                    continue;
                removeChildNodes(node);
            }
        }
        notifyDiff(temp);
    }
}
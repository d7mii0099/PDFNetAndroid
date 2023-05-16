package com.pdftron.recyclertreeview;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.widget.CompoundButtonCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.dialog.pdflayer.PdfLayer;
import com.pdftron.pdf.dialog.pdflayer.PdfLayerDialogFragment;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class PdfLayerTreeViewAdapter<T extends LayoutItemType> extends TreeViewAdapter<T> {

    private OnPdfLayerTreeNodeListener mOnTreeNodeListener;
    private PdfLayerDialogFragment.Theme mTheme;

    public void setOnTreeNodeListener(OnPdfLayerTreeNodeListener onTreeNodeListener) {
        this.mOnTreeNodeListener = onTreeNodeListener;
    }

    public PdfLayerTreeViewAdapter(List<TreeNode<T>> treeNodes, List<? extends TreeViewBinder> viewBinders, PDFViewCtrl pdfViewCtrl, float scale) {
        super(treeNodes, viewBinders, pdfViewCtrl, scale);
    }

    @Override
    public int getItemViewType(int position) {
        return mDisplayNodes.get(position).getContent().getLayoutId();
    }

    public void setTheme(PdfLayerDialogFragment.Theme theme) {
        mTheme = theme;
    }

    @Override
    public void setNodeTreeNode(PDFViewCtrl pdfViewCtrl, TreeNode<T> selectedNode, boolean isSearchMode) {
    }

    @Override
    public void onItemDrop(RecyclerView.ViewHolder holder, int fromPosition, int toPosition) {
    }

    public static List<TreeNode<PdfLayerNode>> buildPdfLayerTreeNodeList(PDFViewCtrl pdfViewCtrl, PdfLayer pdfLayer) {
        List<TreeNode<PdfLayerNode>> treeNodeList = new ArrayList<>();
        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            for (PdfLayer child : pdfLayer.getChildren()) {
                PdfLayerNode pdfLayerNode = new PdfLayerNode(child);
                TreeNode<PdfLayerNode> temp = new TreeNode<>(pdfLayerNode);
                if (child.hasChildren()) {
                    List<TreeNode<PdfLayerNode>> childNodes = buildPdfLayerTreeNodeList(pdfViewCtrl, child);
                    temp.setChildList(childNodes);
                    temp.expand();
                }
                treeNodeList.add(temp);
            }
        } catch (Exception ignored) {

        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
        return treeNodeList;
    }

    public void expandDisplayNodesNodes(boolean isSearchMode) {
        List<TreeNode<T>> tempList = new ArrayList<>(mDisplayNodes);
        for (TreeNode<T> currentNode : tempList) {
            int positionStart = getExpandableStartPosition(currentNode);
            if (!currentNode.isExpand()) {
                // ui
                notifyItemRangeInserted(positionStart, addChildNodes(currentNode, positionStart));
            }
        }
    }

    @Override
    protected void removeFromParent(TreeNode<T> selected, TreeNode<T> parent) {
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
        if (holder instanceof PdfLayerNodeBinder.ViewHolder) {
            CheckBox checkBox = ((PdfLayerNodeBinder.ViewHolder) holder).getCheckBox();
            ImageView imageViewArrow = ((PdfLayerNodeBinder.ViewHolder) holder).getIvArrow();
            ImageView imageViewLock = ((PdfLayerNodeBinder.ViewHolder) holder).getIvLock();
            TextView textViewName = ((PdfLayerNodeBinder.ViewHolder) holder).getTvName();
            View pdfLayerDivider = ((PdfLayerNodeBinder.ViewHolder) holder).getPdfLayerDivider();

            setupPdfLayerItemView(holder, position, checkBox, imageViewArrow, imageViewLock, textViewName, pdfLayerDivider);
        }
    }

    protected void setupPdfLayerItemView(RecyclerView.ViewHolder holder, int position,
            CheckBox checkBox, ImageView imageViewArrow, ImageView imageViewLock,
            TextView textViewName, View pdfLayerDivider) {

        setArrowMargins(holder, position);
        TreeNode<PdfLayerNode> treeNode = (TreeNode<PdfLayerNode>) mDisplayNodes.get(position);
        PdfLayer pdfLayer = treeNode.getContent().getPdfLayer();
        TreeNode<PdfLayerNode> selectedNode = (TreeNode<PdfLayerNode>) mDisplayNodes.get(position);
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
            try {
                viewBinder.bindView(holder, position, mDisplayNodes.get(position));
            } catch (PDFNetException e) {
                e.printStackTrace();
            }
        }

        checkBox.setVisibility(pdfLayer.isLocked() || pdfLayer.isChecked() == null ? View.GONE : View.VISIBLE);
        imageViewLock.setVisibility(pdfLayer.isLocked() ? View.VISIBLE : View.GONE);
        if (!pdfLayer.isLocked() && pdfLayer.isChecked() != null) {
            treeNode.getContent().mIsSelected = pdfLayer.isChecked();
            checkBox.setChecked(treeNode.getContent().mIsSelected);
        }

        boolean isParentEnabled = true;
        if (pdfLayer.getParent() != null) {
            isParentEnabled = (pdfLayer.getParent().isChecked() == null || pdfLayer.getParent().isChecked())
                    && pdfLayer.getParent().isEnabled();
        }
        CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(isParentEnabled ? mTheme.checkBoxColor : mTheme.disabledCheckBoxColor));
        checkBox.setEnabled(isParentEnabled);
        pdfLayer.setEnabled(isParentEnabled);
        textViewName.setTextColor(isParentEnabled ? mTheme.textColor : mTheme.disabledTextColor);

        pdfLayerDivider.setVisibility(pdfLayer.hasChildren() ? View.VISIBLE : View.GONE);

        if (mTheme != null) {
            imageViewArrow.setColorFilter(mTheme.secondaryTextColor);
            pdfLayerDivider.setBackgroundColor(mTheme.disabledTextColor);
        }
    }

    @Override
    protected void setArrowMargins(RecyclerView.ViewHolder holder, int position) {
        ImageView imageViewArrow = ((PdfLayerNodeBinder.ViewHolder) holder).getIvArrow();
        setArrowMarginsWithLevel(imageViewArrow, position);
    }

    protected void setArrowMarginsWithLevel(ImageView imageViewArrow, int position) {
        int nodeLevel = ((TreeNode<PdfLayerNode>) mDisplayNodes.get(position)).getContent().getPdfLayer().getLevel();
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(imageViewArrow.getLayoutParams());
        int startMargin = (int) (mScale * (50 + (14 * nodeLevel)));
        marginParams.setMargins(startMargin, (int) (mScale * 7), marginParams.rightMargin, marginParams.bottomMargin);
        if (Utils.isJellyBeanMR1()) {
            marginParams.setMarginStart(startMargin);
        }
        imageViewArrow.setLayoutParams(new RelativeLayout.LayoutParams(marginParams));
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
        }
        //Close all root nodes.
        for (TreeNode<T> root : roots) {
            if (root.isExpand())
                removeChildNodes(root);
        }
        notifyDiff(temp);
    }

    public interface OnPdfLayerTreeNodeListener {
        /**
         * called when TreeNodes were clicked.
         *
         * @return weather consume the click event.
         */
        boolean onClick(TreeNode<PdfLayerNode> node, RecyclerView.ViewHolder holder);

        /**
         * called when TreeNodes were toggle.
         *
         * @param isExpand the status of TreeNodes after being toggled.
         */
        void onToggle(boolean isExpand, RecyclerView.ViewHolder holder);
    }
}

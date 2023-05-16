package com.pdftron.recyclertreeview;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.tools.R;

public class PdfLayerNodeBinder extends TreeViewBinder<PdfLayerNodeBinder.ViewHolder> {

    public interface PdfLayerNodeClickListener {
        void onExpandNode(TreeNode<PdfLayerNode> treeNode, int position);

        void onNodeCheckBoxSelected(TreeNode<PdfLayerNode> treeNode, RecyclerView.ViewHolder viewHolder);
    }

    @NonNull
    private final PdfLayerNodeClickListener mPdfLayerNodeClickListener;

    public PdfLayerNodeBinder(@NonNull PdfLayerNodeClickListener listener) {
        mPdfLayerNodeClickListener = listener;
    }

    @Override
    public ViewHolder provideViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void bindView(ViewHolder holder, int position, TreeNode<?> treeNode) {
        PdfLayerNode pdfLayerNode = (PdfLayerNode) treeNode.getContent();
        int rotateDegree = treeNode.isExpand() ? 180 : 90;

        holder.ivArrow.setRotation(rotateDegree);
        holder.tvName.setText(pdfLayerNode.getTitle());
        if (treeNode.isLeaf()) {
            holder.ivArrow.setVisibility(View.INVISIBLE);
        } else {
            holder.ivArrow.setVisibility(View.VISIBLE);
            holder.ivArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPdfLayerNodeClickListener.onExpandNode((TreeNode<PdfLayerNode>) treeNode, position);
                    int rotate = treeNode.isExpand() ? 180 : 90;
                    holder.ivArrow.setRotation(rotate);
                }
            });
        }
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPdfLayerNodeClickListener.onNodeCheckBoxSelected((TreeNode<PdfLayerNode>) treeNode, holder);
            }
        });
        if (pdfLayerNode.getPdfLayer().isChecked() != null) {
            holder.checkBox.setChecked(pdfLayerNode.getPdfLayer().isChecked());
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.pdf_layer_tree_view_list_item;
    }

    public static class ViewHolder extends TreeViewBinder.ViewHolder {
        private final CheckBox checkBox;
        private final ImageView ivArrow;
        private final ImageView ivLock;
        private final TextView tvName;
        private final View pdfLayerDivider;

        public ViewHolder(View rootView) {
            super(rootView);
            this.ivArrow = rootView.findViewById(R.id.tree_view_arrow);
            this.ivLock = rootView.findViewById(R.id.tree_view_lock);
            this.tvName = rootView.findViewById(R.id.tree_view_name);
            this.checkBox = rootView.findViewById(R.id.tree_view_selected);
            this.pdfLayerDivider = rootView.findViewById(R.id.pdf_layer_divider);
        }

        public ImageView getIvArrow() {
            return ivArrow;
        }

        public ImageView getIvLock() {
            return ivLock;
        }

        public TextView getTvName() {
            return tvName;
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public View getPdfLayerDivider() {
            return pdfLayerDivider;
        }
    }
}

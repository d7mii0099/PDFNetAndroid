package com.pdftron.recyclertreeview;

import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.tools.R;

public class BookmarkNodeBinder extends TreeViewBinder<BookmarkNodeBinder.ViewHolder> {

    public interface BookmarkNodeClickListener {
        void onExpandNode(TreeNode<BookmarkNode> treeNode, int position);

        void onNodeCheckBoxSelected(TreeNode<BookmarkNode> treeNode, RecyclerView.ViewHolder viewHolder);

        void onStartDrag(TreeNode<BookmarkNode> treeNode, int position, RecyclerView.ViewHolder viewHolder);
    }

    @NonNull
    private final BookmarkNodeClickListener mBookmarkNodeClickListener;

    public BookmarkNodeBinder(@NonNull BookmarkNodeClickListener listener) {
        mBookmarkNodeClickListener = listener;
    }

    @Override
    public ViewHolder provideViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void bindView(ViewHolder holder, int position, TreeNode<?> treeNode) {
        BookmarkNode bookmarkNode = (BookmarkNode) treeNode.getContent();
        int rotateDegree = treeNode.isExpand() ? 180 : 90;

        holder.ivArrow.setRotation(rotateDegree);
        holder.tvName.setText(bookmarkNode.getTitle());
        holder.tvPageNumber.setText(String.valueOf(bookmarkNode.getPageNumber()));
        if (treeNode.isLeaf()) {
            holder.ivArrow.setVisibility(View.INVISIBLE);
        } else {
            holder.ivArrow.setVisibility(View.VISIBLE);
            holder.ivArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBookmarkNodeClickListener.onExpandNode((TreeNode<BookmarkNode>) treeNode, position);
                    int rotate = treeNode.isExpand() ? 180 : 90;
                    holder.ivArrow.setRotation(rotate);
                }
            });
        }
        holder.ivDrag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mBookmarkNodeClickListener.onStartDrag((TreeNode<BookmarkNode>) treeNode, position, holder);
                return true;
            }
        });
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBookmarkNodeClickListener.onNodeCheckBoxSelected((TreeNode<BookmarkNode>) treeNode, holder);
            }
        });
        holder.checkBox.setChecked(bookmarkNode.mIsSelected);
    }

    @Override
    public int getLayoutId() {
        return R.layout.tree_view_list_item;
    }

    public static class ViewHolder extends TreeViewBinder.ViewHolder {
        private final CheckBox checkBox;
        private final ImageView ivArrow;
        private final ImageView ivDrag;
        private final TextView tvName;
        private final TextView tvPageNumber;

        public ViewHolder(View rootView) {
            super(rootView);
            this.ivArrow = rootView.findViewById(R.id.tree_view_arrow);
            this.ivDrag = rootView.findViewById(R.id.tree_view_drag);
            this.tvName = rootView.findViewById(R.id.tree_view_name);
            this.tvPageNumber = rootView.findViewById(R.id.tree_view_page_number);
            this.checkBox = rootView.findViewById(R.id.tree_view_selected);
        }

        public ImageView getIvArrow() {
            return ivArrow;
        }

        public ImageView getIvDrag() {
            return ivDrag;
        }

        public TextView getTvName() {
            return tvName;
        }

        public TextView getTvPageNumber() {
            return tvPageNumber;
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }
    }
}

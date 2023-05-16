package com.pdftron.pdf.dialog.toolbarswitcher.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcel;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.dialog.base.BaseBottomDialogFragment;
import com.pdftron.pdf.dialog.toolbarswitcher.ToolbarSwitcherViewModel;
import com.pdftron.pdf.dialog.toolbarswitcher.model.ToolbarSwitcherItem;
import com.pdftron.pdf.dialog.toolbarswitcher.model.ToolbarSwitcherState;
import com.pdftron.pdf.interfaces.builder.SkeletalFragmentBuilder;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;

public class ToolbarSwitcherDialog extends BaseBottomDialogFragment {

    public static final String TAG = ToolbarSwitcherDialog.class.getName();

    protected RecyclerView mRecyclerView;
    protected ToolbarSwitcherViewModel mViewModel;
    protected ToolbarSwitcherDialogTheme mTheme;

    @Override
    protected Dialog onCreateDialogImpl(@NonNull Context context) {
        return new Dialog(context, R.style.ToolbarSwitcherDialogStyle);
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @Override
    protected int getContentLayoutResource() {
        return R.layout.controls_toolbar_switcher_content;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mTheme = ToolbarSwitcherDialogTheme.fromContext(root.getContext());
        mRecyclerView = root.findViewById(R.id.toolbar_switcher_list);
        mRecyclerView.setBackgroundColor(mTheme.backgroundColor);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));

        TypedValue outValue = new TypedValue();
        root.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);

        mViewModel = ViewModelProviders.of(getTargetFragment()).get(ToolbarSwitcherViewModel.class);

        final ToolbarSwitcherAdapter adapter = new ToolbarSwitcherAdapter(mTheme, mViewModel, this, getContext());
        mRecyclerView.setAdapter(adapter);
        mViewModel.observeToolbarSwitcherState(getTargetFragment(), new Observer<ToolbarSwitcherState>() {
            @Override
            public void onChanged(ToolbarSwitcherState toolbarSwitcherState) {
                if (toolbarSwitcherState != null) {
                    adapter.setToolbarSwitcherState(toolbarSwitcherState);
                }
            }
        });

        return root;
    }

    protected static class ToolbarSwitcherAdapter extends RecyclerView.Adapter<ToolbarSwitcherViewHolder> {

        @Nullable
        protected ToolbarSwitcherState mToolbarSwitcherState;

        protected ToolbarSwitcherDialog mdialog;
        protected ToolbarSwitcherViewModel mViewModel;
        protected ToolbarSwitcherDialogTheme mTheme;
        protected Context mContext;

        protected ToolbarSwitcherAdapter(ToolbarSwitcherDialogTheme theme, ToolbarSwitcherViewModel viewModel, ToolbarSwitcherDialog dialog, Context context){
            mdialog = dialog;
            mViewModel = viewModel;
            mTheme = theme;
            mContext = context;
        }

        public void setToolbarSwitcherState(ToolbarSwitcherState toolbarSwitcherState) {
            mToolbarSwitcherState = toolbarSwitcherState;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ToolbarSwitcherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_toolbar_switcher, parent, false);
            return new ToolbarSwitcherViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ToolbarSwitcherViewHolder holder, int position) {
            if (mToolbarSwitcherState != null) {
                ToolbarSwitcherItem item = mToolbarSwitcherState.get(position);
                holder.icon.setImageResource(item.getIcon());
                holder.icon.setColorFilter(mTheme.iconColor);
                holder.name.setText(item.getToolbarName(mContext));
                holder.name.setTextColor(mTheme.textColor);
                if (item.isVisible()) {
                    holder.itemView.setVisibility(View.VISIBLE);
                    // Resize item to origin size defined in item_toolbar_switcher.xml
                    holder.itemView.setLayoutParams(
                            new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT)
                    );
                } else {
                    holder.itemView.setVisibility(View.GONE);
                    // Resize item to zero, to remove blank space
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                }
                if (item.isSelected) {
                    holder.itemView.setBackgroundColor(mTheme.selectedItemBackgroundColor);
                    holder.icon.setColorFilter(mTheme.selectedIconColor);
                } else {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    holder.icon.setColorFilter(mTheme.iconColor);
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                            ToolbarSwitcherItem switcherItem = mToolbarSwitcherState.get(holder.getAdapterPosition());
                            String tag = switcherItem.getTag();
                            mViewModel.selectToolbar(tag);
                            AnalyticsHandlerAdapter.getInstance().sendEvent(
                                    AnalyticsHandlerAdapter.EVENT_TOOLBAR_SWITCHER,
                                    AnalyticsParam.annotationToolbarSwitcherParam(switcherItem)
                            );
                        }
                        mdialog.dismiss();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mToolbarSwitcherState == null ? 0 : mToolbarSwitcherState.size();
        }
    }

    protected static class ToolbarSwitcherViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView icon;
        TextView name;

        public ToolbarSwitcherViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
        }
    }

    public static class Builder extends SkeletalFragmentBuilder<ToolbarSwitcherDialog> {

        private boolean anchorInScreen = false;
        private boolean hasAnchor = false;
        private float left;
        private float top;
        private float right;
        private float bottom;

        public Builder() {
            super();
        }

        @Override
        public ToolbarSwitcherDialog build(@NonNull Context context) {
            return build(context, ToolbarSwitcherDialog.class);
        }

        @Override
        public Bundle createBundle(@NonNull Context context) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ARGS_KEY_ANCHOR_SCREEN, anchorInScreen);
            if (hasAnchor) {
                Bundle rect = new Bundle();
                rect.putInt("left", (int) left);
                rect.putInt("top", (int) top);
                rect.putInt("right", (int) right);
                rect.putInt("bottom", (int) bottom);
                bundle.putBundle(ARGS_KEY_ANCHOR, rect);
            }
            return bundle;
        }

        @Override
        public void checkArgs(@NonNull Context context) {
        }

        /**
         * Sets anchor rectangle in window location for tablet mode.
         * The annotation style dialog will be displayed around the anchor rectangle.
         * <br/>
         * You can get window location of a view as follows:
         * <pre>
         * int[] pos = new int[2];
         * view.getLocationInWindow(pos);
         * RectF rect = new RectF(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight());
         * builder.setAnchor(rect);
         * </pre>
         * <p>
         * where {@code view} is an instance of View, and {@code builder} is an instance of {@link Builder}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchor(RectF anchor) {
            hasAnchor = true;
            left = anchor.left;
            top = anchor.top;
            right = anchor.right;
            bottom = anchor.bottom;
            return this;
        }

        /**
         * Sets anchor rectangle in window location for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         * This is equivalent to call {@link #setAnchor(RectF)}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchor(Rect anchor) {
            hasAnchor = true;
            left = anchor.left;
            top = anchor.top;
            right = anchor.right;
            bottom = anchor.bottom;
            return this;
        }

        /**
         * Sets anchor view for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         *
         * @param view The anchor view
         * @return The builder
         */
        public Builder setAnchorView(View view) {
            int[] pos = new int[2];
            view.getLocationInWindow(pos);
            return setAnchor(new Rect(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight()));
        }

        /**
         * Sets anchor rectangle in screen position for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         * <br/>
         * You can get screen location of a view as follows:
         * <pre>
         * int[] pos = new int[2];
         * view.getLocationInScreen(pos);
         * RectF rect = new RectF(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight());
         * builder.setAnchorInScreen(rect);
         * </pre>
         * <p>
         * where {@code view} is an instance of View, and {@code builder} is an instance of {@link Builder}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchorInScreen(Rect anchor) {
            setAnchor(anchor);
            anchorInScreen = true;
            return this;
        }

        // Parcelable methods

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte(this.anchorInScreen ? (byte) 1 : (byte) 0);
            dest.writeByte(this.hasAnchor ? (byte) 1 : (byte) 0);
            dest.writeFloat(this.left);
            dest.writeFloat(this.top);
            dest.writeFloat(this.right);
            dest.writeFloat(this.bottom);
        }

        protected Builder(Parcel in) {
            this.anchorInScreen = in.readByte() != 0;
            this.hasAnchor = in.readByte() != 0;
            this.left = in.readFloat();
            this.top = in.readFloat();
            this.right = in.readFloat();
            this.bottom = in.readFloat();
        }

        public static final Creator<Builder> CREATOR = new Creator<Builder>() {
            @Override
            public Builder createFromParcel(Parcel source) {
                return new Builder(source);
            }

            @Override
            public Builder[] newArray(int size) {
                return new Builder[size];
            }
        };
    }
}

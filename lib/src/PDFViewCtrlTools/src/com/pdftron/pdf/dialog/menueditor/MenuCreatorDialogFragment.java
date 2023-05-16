package com.pdftron.pdf.dialog.menueditor;

import android.content.ClipData;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemContent;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.TouchAwareRecyclerView;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import java.util.ArrayList;
import java.util.UUID;

import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

public class MenuCreatorDialogFragment extends CustomSizeDialogFragment implements
        View.OnDragListener,
        MenuCreatorAdapter.OnItemActionListener {

    public static final String TAG = MenuCreatorDialogFragment.class.getName();

    private static final int SPAN_COUNT_PINNED = 5;
    private static final int SPAN_COUNT_ALL = 5;

    private TouchAwareRecyclerView mPinnedList;
    private RecyclerView mAllList;

    private TextView mPinnedLabel;
    private FrameLayout mTrashArea;

    private MenuCreatorAdapter mPinnedListAdapter;
    private MenuCreatorAdapter mAllListAdapter;

    private MenuEditorViewModel mViewModel;

    private int mDraggingPos = RecyclerView.NO_POSITION;
    private Theme mTheme;

    public static MenuCreatorDialogFragment newInstance() {
        return new MenuCreatorDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_creator_dialog, container);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menu_creator_title);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mPinnedList = view.findViewById(R.id.pinned_container);
        mAllList = view.findViewById(R.id.all_container);
        mPinnedLabel = view.findViewById(R.id.pinned_label);
        mTrashArea = view.findViewById(R.id.trash_area);

        // Setup colors
        mTheme = Theme.fromContext(view.getContext());

        View background = view.findViewById(R.id.background);
        background.setBackgroundColor(mTheme.backgroundColor);

        // Colors for pinned area
        View pinnedLayout = view.findViewById(R.id.pinned_layout);
        pinnedLayout.setBackgroundColor(mTheme.pinnedBackgroundColor);
        mPinnedLabel.setTextColor(mTheme.textColor);
        Utils.updateDashedLineColor(mPinnedList, mTheme.dottedLineColor);

        // Colors for trash area
        Utils.updateDashedLineColor(mTrashArea, mTheme.dottedLineActiveColor);
        ImageView trashIcon = view.findViewById(R.id.trash_icon);
        trashIcon.setColorFilter(mTheme.iconColor);

        // Setup listeners
        mTrashArea.setOnDragListener(this);

        // layout manager
        mPinnedList.setLayoutManager(new GridLayoutManager(getContext(), SPAN_COUNT_PINNED));
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(), SPAN_COUNT_ALL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAllListAdapter.getItemViewType(position)) {
                    case MenuCreatorAdapter.VIEW_TYPE_HEADER:
                        return layoutManager.getSpanCount();
                    case MenuCreatorAdapter.VIEW_TYPE_CONTENT:
                    default:
                        return 1;
                }
            }
        });
        mAllList.setLayoutManager(layoutManager);

        // adapter
        mPinnedListAdapter = new MenuCreatorAdapter();
        mAllListAdapter = new MenuCreatorAdapter();

        mPinnedList.setAdapter(mPinnedListAdapter);
        mAllList.setAdapter(mAllListAdapter);

        // pinned
        ItemClickHelper clickHelperPinned = new ItemClickHelper();
        clickHelperPinned.attachToRecyclerView(mPinnedList);
        SimpleItemTouchHelperCallback touchHelperCallbackPinned = new SimpleItemTouchHelperCallback(mPinnedListAdapter, SPAN_COUNT_PINNED, false, false);
        touchHelperCallbackPinned.setAllowDragAmongSections(true);
        final ItemTouchHelper touchHelperPinned = new ItemTouchHelper(touchHelperCallbackPinned);
        touchHelperPinned.attachToRecyclerView(mPinnedList);
        clickHelperPinned.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                showRemoveMenu(view, position);
            }
        });
        clickHelperPinned.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, final View view, final int position, long id) {
                mPinnedList.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mPinnedListAdapter.isHeader(position)) {
                            RecyclerView.ViewHolder holder = mPinnedList.findViewHolderForAdapterPosition(position);
                            if (holder != null) {
                                mPinnedListAdapter.setDragging(true);
                                mPinnedListAdapter.notifyHeadersChanged();

                                // hide all item list and show trash area
                                mAllList.setVisibility(View.GONE);
                                mTrashArea.setVisibility(View.VISIBLE);
                                mPinnedLabel.setText(R.string.menu_editor_delete_title);

                                touchHelperPinned.startDrag(holder);
                                mDraggingPos = position;
                            }
                        }
                    }
                });
                return true;
            }
        });
        mPinnedList.setOnDragListener(this);
        mPinnedListAdapter.setOnDragListener(this);
        mPinnedListAdapter.setOnItemActionListener(this);

        // all
        ItemClickHelper clickHelperAll = new ItemClickHelper();
        clickHelperAll.attachToRecyclerView(mAllList);
        clickHelperAll.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, final View view, final int position, long id) {
                mAllList.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mAllListAdapter.isHeader(position)) {
                            RecyclerView.ViewHolder holder = mAllList.findViewHolderForAdapterPosition(position);
                            if (holder != null) {
                                ClipData dragData = ClipData.newPlainText("position", String.valueOf(position));
                                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                                ViewCompat.startDragAndDrop(view, dragData, shadowBuilder, view, 0);
                                Utils.updateDashedLineColor(mPinnedList, mTheme.dottedLineActiveColor);
                            }
                        }
                    }
                });
                return true;
            }
        });
        clickHelperAll.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                showAddMenu(view, position);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Fragment parentFragment = getParentFragment();
        if (parentFragment == null) {
            throw new RuntimeException("This fragment should run as a child fragment of a containing parent fragment.");
        }

        mViewModel = ViewModelProviders.of(parentFragment).get(MenuEditorViewModel.class);
        mPinnedListAdapter.setViewModel(mViewModel);
        mAllListAdapter.setViewModel(mViewModel);

        mViewModel.getAllItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<MenuEditorItem>>() {
            @Override
            public void onChanged(ArrayList<MenuEditorItem> menuEditorItems) {
                // emit once
                if (mAllListAdapter != null) {
                    mAllListAdapter.setData(menuEditorItems);
                }
                mViewModel.getAllItemsLiveData().removeObserver(this);
            }
        });

        mViewModel.getPinnedItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<MenuEditorItem>>() {
            @Override
            public void onChanged(ArrayList<MenuEditorItem> menuEditorItems) {
                // emit once
                if (mPinnedListAdapter != null) {
                    mPinnedListAdapter.setData(menuEditorItems);
                }
                mViewModel.getPinnedItemsLiveData().removeObserver(this);
            }
        });
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DROP) {
            ClipData data = event.getClipData();
            if (data.getItemCount() > 0) {
                ClipData.Item item = data.getItemAt(0);
                CharSequence positionStr = item.getText();
                if (positionStr != null) {
                    try {
                        int position = Integer.parseInt(positionStr.toString());

                        if (mPinnedListAdapter.isDragging()) {
                            // dragging item in pinned list
                            // check if it is deletion
                            if (v.getId() == R.id.trash_area) {
                                mPinnedListAdapter.removeAt(position);
                                mPinnedListAdapter.notifyItemRemoved(position);
                            }
                        } else {
                            // dragging item in all items list
                            // assume append to the end
                            int targetPosition = mPinnedListAdapter.getItemCount();
                            if (v.getId() != R.id.pinned_container) {
                                // an item is dragged to pinned area touching existing item
                                targetPosition = (int) v.getTag();
                            }
                            if (addItemToPinned(position, targetPosition)) {
                                mPinnedListAdapter.notifyDataSetChanged();
                            }
                        }
                    } catch (Exception ex) {
                        AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    }
                }
            }
        } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            onItemDrop();
        }
        return true;
    }

    private void showAddMenu(final View v, final int position) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.inflate(R.menu.menu_editor_add_item);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.add_to_list) {
                    int targetPosition = mPinnedListAdapter.getItemCount();
                    if (addItemToPinned(position, targetPosition)) {
                        mPinnedListAdapter.notifyItemInserted(targetPosition);
                    }
                }
                return true;
            }
        });

        if (popup.getMenu() instanceof MenuBuilder) {
            MenuPopupHelper menuHelper = new MenuPopupHelper(v.getContext(), (MenuBuilder) popup.getMenu(), v);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        } else {
            popup.show();
        }
    }

    private void showRemoveMenu(final View v, final int position) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.inflate(R.menu.menu_editor_remove_item);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.remove_from_list) {
                    mPinnedListAdapter.removeAt(position);
                    mPinnedListAdapter.notifyItemRemoved(position);
                    mPinnedListAdapter.notifyPinnedItemsChanged();
                }
                return true;
            }
        });

        if (popup.getMenu() instanceof MenuBuilder) {
            MenuPopupHelper menuHelper = new MenuPopupHelper(v.getContext(), (MenuBuilder) popup.getMenu(), v);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        } else {
            popup.show();
        }
    }

    private boolean addItemToPinned(int sourcePosition, int targetPosition) {
        MenuEditorItem menuItem = mAllListAdapter.getItem(sourcePosition);
        if (menuItem instanceof MenuEditorItemContent) {
            MenuEditorItemContent contentItem = (MenuEditorItemContent) menuItem;
            if (contentItem.getTitle() != null && contentItem.getDrawable() != null) {
                mPinnedListAdapter.insert(new MenuEditorItemContent(
                        generateNewUniqueId(),
                        contentItem.getToolbarButtonType(),
                        contentItem.getTitle(),
                        DrawableCompat.wrap(contentItem.getDrawable()).mutate()
                ), targetPosition);
                mPinnedList.scrollToPosition(targetPosition);
                return true;
            }
        }
        return false;
    }

    private int generateNewUniqueId() {
        ArrayList<MenuEditorItem> menuEditorItems = mPinnedListAdapter.mMenuEditorItems;
        int idSample = UUID.randomUUID().hashCode();
        boolean hasResult = true;
        for (MenuEditorItem menuEditorItem : menuEditorItems) {
            if (menuEditorItem instanceof MenuEditorItemContent) {
                if (((MenuEditorItemContent) menuEditorItem).getId() == idSample) {
                    hasResult = false;
                    break;
                }
            }
        }
        if (hasResult) {
            return idSample;
        } else {
            return generateNewUniqueId();
        }
    }

    @Override
    public void onItemMove(int toPosition) {
        if (mDraggingPos != RecyclerView.NO_POSITION) {
            mDraggingPos = toPosition;
        }
    }

    @Override
    public void onItemDrop() {
        if (mAllList == null || mPinnedList == null || mTrashArea == null) {
            return;
        }

        if (mDraggingPos != RecyclerView.NO_POSITION &&
                isViewInBounds(mTrashArea,
                        Math.round(mPinnedList.getTouchX()),
                        Math.round(mPinnedList.getTouchY()))) {
            mPinnedListAdapter.removeAt(mDraggingPos);
            mPinnedListAdapter.notifyDataSetChanged();
            mDraggingPos = RecyclerView.NO_POSITION;
        }

        // show all item list and hide trash area
        mAllList.setVisibility(View.VISIBLE);
        mTrashArea.setVisibility(View.GONE);
        mPinnedLabel.setText(R.string.menu_editor_add_title);
        Utils.updateDashedLineColor(mPinnedList, mTheme.dottedLineColor);
    }

    private final Rect outRect = new Rect();
    private final int[] location = new int[2];

    private boolean isViewInBounds(View view, int x, int y) {
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }
}

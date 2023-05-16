package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnnotationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int STATE_UNKNOWN = 0;
    private static final int STATE_SECTIONED_CELL = 1;
    private static final int STATE_REGULAR_CELL = 2;

    private ArrayList<AnnotationDialogFragment.AnnotationInfo> mAnnotation;
    private int[] mCellStates;
    private final boolean mIsReadOnly;

    private static final int CONTEXT_MENU_DELETE_ITEM = 0;
    private static final int CONTEXT_MENU_DELETE_ITEM_ON_PAGE = 1;
    private static final int CONTEXT_MENU_DELETE_ALL = 2;

    private final RecyclerView mRecyclerView;
    private final PDFViewCtrl mPdfViewCtrl;
    private NavigationListDialogFragment.AnalyticsEventListener mAnalyticsEventListener;
    private RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        public void onChanged() {
            mCellStates = mAnnotation == null ? null : new int[mAnnotation.size()];
        }
    };

    public AnnotationsAdapter(ArrayList<AnnotationDialogFragment.AnnotationInfo> objects, boolean isReadOnly, RecyclerView recyclerView, PDFViewCtrl pdfViewCtrl, NavigationListDialogFragment.AnalyticsEventListener analyticsEventListener) {
        mAnnotation = objects;
        mIsReadOnly = isReadOnly;
        mRecyclerView = recyclerView;
        mPdfViewCtrl = pdfViewCtrl;
        mAnalyticsEventListener = analyticsEventListener;
        mCellStates = new int[objects.size()];
        registerAdapterDataObserver(observer);
    }

    public void addAll(List<AnnotationDialogFragment.AnnotationInfo> annotationInfos) {
        mAnnotation.addAll(annotationInfos);
        notifyDataSetChanged();
    }

    public void removeAll(List<AnnotationDialogFragment.AnnotationInfo> annotationInfos) {
        mAnnotation.removeAll(annotationInfos);
        notifyDataSetChanged();
    }

    public void replaceAll(List<AnnotationDialogFragment.AnnotationInfo> annotationInfos) {
        mAnnotation.removeAll(annotationInfos);
        mAnnotation.addAll(annotationInfos);
        notifyDataSetChanged();
    }

    public AnnotationDialogFragment.AnnotationInfo getItem(int position) {
        if (mAnnotation != null && position >= 0 && position < mAnnotation.size()) {
            return mAnnotation.get(position);
        }
        return null;
    }

    public ArrayList<AnnotationDialogFragment.AnnotationInfo> getItemsOnPage(int pageNum) {
        ArrayList<AnnotationDialogFragment.AnnotationInfo> list = new ArrayList<>();
        if (mAnnotation != null) {
            for (AnnotationDialogFragment.AnnotationInfo info : mAnnotation) {
                if (info.getPageNum() == pageNum) {
                    list.add(info);
                }
            }
            return list;
        }
        return null;
    }

    public ArrayList<AnnotationDialogFragment.AnnotationInfo> getItems() {
        return new ArrayList<>(mAnnotation);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.controls_fragment_annotation_listview_item, parent, false);
        return new ViewHolder(view);
    }

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Context context = holder.itemView.getContext();
        if (context == null) {
            return;
        }

        boolean needSeparator = false;
        AnnotationDialogFragment.AnnotationInfo annotationInfo = mAnnotation.get(position);
        if (position < mCellStates.length) {
            switch (mCellStates[position]) {
                case STATE_SECTIONED_CELL:
                    needSeparator = true;
                    break;
                case STATE_REGULAR_CELL:
                    needSeparator = false;
                    break;
                case STATE_UNKNOWN:
                default:
                    if (position == 0) {
                        needSeparator = true;
                    } else {
                        AnnotationDialogFragment.AnnotationInfo previousAnnotation = mAnnotation.get(position - 1);
                        if (annotationInfo.getPageNum() != previousAnnotation.getPageNum()) {
                            needSeparator = true;
                        }
                    }

                    // Cache the result
                    mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
                    break;
            }
        }

        ViewHolder viewHolder = (ViewHolder) holder;

        if (needSeparator) {
            viewHolder.separator.setText(String.format(holder.itemView.getContext().getString(R.string.controls_annotation_dialog_page), annotationInfo.getPageNum()));
            viewHolder.separator.setVisibility(View.VISIBLE);
        } else {
            viewHolder.separator.setVisibility(View.GONE);
        }
        String content = annotationInfo.getContent();
        if (Utils.isNullOrEmpty(content)) {
            viewHolder.line1.setVisibility(View.GONE);
        } else {
            viewHolder.line1.setText(annotationInfo.getContent());
            viewHolder.line1.setVisibility(View.VISIBLE);
        }

        // Set icon based on the annotation type
        viewHolder.icon.setImageResource(AnnotUtils.getAnnotImageResId(annotationInfo.getType()));

        StringBuilder descBuilder = new StringBuilder();
        if (PdfViewCtrlSettingsManager.getAnnotListShowAuthor(context)) {
            String author = annotationInfo.getAuthor();
            if (!author.isEmpty()) {
                descBuilder.append(author).append(", ");
            }
        }
        descBuilder.append(annotationInfo.getDate());
        viewHolder.line2.setText(descBuilder.toString());

        // set author and date
        Annot annot = annotationInfo.getAnnotation();

        int color = AnnotUtils.getAnnotColor(annot);
        if (color == -1) {
            color = Color.BLACK;
        }
        viewHolder.icon.setColorFilter(color);
        viewHolder.icon.setAlpha(AnnotUtils.getAnnotOpacity(annot));
    }

    @Override
    public int getItemCount() {
        if (mAnnotation != null) {
            return mAnnotation.size();
        } else {
            return 0;
        }
    }

    public void clear() {
        mAnnotation.clear();
    }

    public boolean remove(AnnotationDialogFragment.AnnotationInfo annotInfo) {
        return mAnnotation.remove(annotInfo);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        public TextView separator;
        public TextView line1;
        public TextView line2;
        public ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            separator = itemView.findViewById(R.id.textview_annotation_recyclerview_item_separator);
            icon = itemView.findViewById(R.id.imageview_annotation_recyclerview_item);
            line1 = itemView.findViewById(R.id.textview_annotation_recyclerview_item);
            line2 = itemView.findViewById(R.id.textview_desc_recyclerview_item);
            if (!mIsReadOnly) {
                itemView.setOnCreateContextMenuListener(this);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
            final int position = mRecyclerView.getChildAdapterPosition(view);
            AnnotationDialogFragment.AnnotationInfo item = getItem(position);
            Context context = view.getContext();
            if (item != null) {
                String title = String.format(context.getString(R.string.controls_annotation_dialog_page), item.getPageNum());
                String author = item.getAuthor();
                if (!Utils.isNullOrEmpty(author)) {
                    title = title + " " + context.getString(R.string.controls_annotation_dialog_author) + " " + author;
                }
                menu.setHeaderTitle(title);
            }
            String[] menuItems = context.getResources().getStringArray(R.array.annotation_dialog_context_menu);
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ITEM, CONTEXT_MENU_DELETE_ITEM, menuItems[CONTEXT_MENU_DELETE_ITEM]);
            String deleteOnPage = menuItems[CONTEXT_MENU_DELETE_ITEM_ON_PAGE];
            if (item != null) {
                deleteOnPage = deleteOnPage + " " + item.getPageNum();
            }
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ITEM_ON_PAGE, CONTEXT_MENU_DELETE_ITEM_ON_PAGE, deleteOnPage);
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ALL, CONTEXT_MENU_DELETE_ALL, menuItems[CONTEXT_MENU_DELETE_ALL]);
            MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    onContextMenuItemClicked(item, position);
                    return true;
                }
            };
            menu.getItem(CONTEXT_MENU_DELETE_ITEM).setOnMenuItemClickListener(listener);
            menu.getItem(CONTEXT_MENU_DELETE_ITEM_ON_PAGE).setOnMenuItemClickListener(listener);
            menu.getItem(CONTEXT_MENU_DELETE_ALL).setOnMenuItemClickListener(listener);
        }

        void onContextMenuItemClicked(MenuItem item, int position) {
            int menuItemIndex = item.getItemId();
            switch (menuItemIndex) {
                case CONTEXT_MENU_DELETE_ITEM:
                    AnnotationDialogFragment.AnnotationInfo annotationInfo = getItem(position);
                    if (annotationInfo == null || mPdfViewCtrl == null) {
                        return;
                    }
                    Annot annot = annotationInfo.getAnnotation();
                    if (annot != null) {
                        int annotPageNum = annotationInfo.getPageNum();
                        HashMap<Annot, Integer> annots = new HashMap<>(1);
                        HashMap<Annot, Integer> annotsPostRemove = new HashMap<>(1);
                        annots.put(annot, annotPageNum);
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        boolean shouldUnlock = false;
                        try {
                            // Locks the document first as accessing annotation/doc information isn't thread
                            // safe.
                            mPdfViewCtrl.docLock(true);
                            shouldUnlock = true;
                            if (toolManager != null) {
                                toolManager.raiseAnnotationsPreRemoveEvent(annots);
                            }

                            Page page = mPdfViewCtrl.getDoc().getPage(annotPageNum);
                            annot = AnnotUtils.safeDeleteAnnotAndUpdate(mPdfViewCtrl, page, annot, annotPageNum);
                            annotsPostRemove.put(annot, annotPageNum);

                            // make sure to raise remove event after mPdfViewCtrl.update
                            if (toolManager != null) {
                                toolManager.raiseAnnotationsRemovedEvent(annotsPostRemove);
                            }
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        } finally {
                            if (shouldUnlock) {
                                mPdfViewCtrl.docUnlock();
                            }
                        }
                    }
                    onEventAction();
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                            AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_DELETE));
                    break;
                case CONTEXT_MENU_DELETE_ITEM_ON_PAGE:
                    annotationInfo = getItem(position);
                    if (annotationInfo != null && annotationInfo.getAnnotation() != null) {
                        deleteOnPage(annotationInfo);
                    }
                    onEventAction();
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                            AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_DELETE_ALL_ON_PAGE));
                    break;
                case CONTEXT_MENU_DELETE_ALL:
                    deleteAll();
                    onEventAction();
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                            AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_DELETE_ALL_IN_DOC));
                    break;
            }
        }

        private void onEventAction() {
            if (mAnalyticsEventListener != null) {
                mAnalyticsEventListener.onEventAction();
            }
        }

        private void deleteOnPage(AnnotationDialogFragment.AnnotationInfo annotationInfo) {
            if (mPdfViewCtrl == null) {
                return;
            }

            int pageNum = annotationInfo.getPageNum();
            boolean hasChange = false;
            boolean shouldUnlock = false;
            try {
                // Locks the document first as accessing annotation/doc information isn't thread
                // safe.
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                ArrayList<AnnotationDialogFragment.AnnotationInfo> items = getItemsOnPage(pageNum);
                Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
                // first raise pre-remove event
                raisePreRemoveAnnotationsEvent(items);
                // perform remove
                for (AnnotationDialogFragment.AnnotationInfo info : items) {
                    if (info.getAnnotation() != null) {
                        page.annotRemove(info.getAnnotation());
                        remove(info);
                    }
                }
                mPdfViewCtrl.update(true);
                hasChange = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            if (hasChange) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager != null) {
                    toolManager.raiseAnnotationsRemovedEvent(pageNum);
                }
            }

            notifyDataSetChanged();
        }

        private void deleteAll() {
            if (mPdfViewCtrl == null) {
                return;
            }

            boolean hasChange = false;
            boolean shouldUnlock = false;
            try {
                // Locks the document first as accessing annotation/doc information isn't thread
                // safe.
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                ArrayList<AnnotationDialogFragment.AnnotationInfo> items = getItems();
                // first raise pre-remove event
                raisePreRemoveAnnotationsEvent(items);
                // perform remove
                for (AnnotationDialogFragment.AnnotationInfo info : items) {
                    if (info.getAnnotation() != null) {
                        Page page = mPdfViewCtrl.getDoc().getPage(info.getPageNum());
                        page.annotRemove(info.getAnnotation());
                        remove(info);
                    }
                }
                mPdfViewCtrl.update(true);
                hasChange = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            if (hasChange) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager != null) {
                    toolManager.raiseAllAnnotationsRemovedEvent();
                }
            }

            notifyDataSetChanged();
        }

        private void raisePreRemoveAnnotationsEvent(ArrayList<AnnotationDialogFragment.AnnotationInfo> items) {
            HashMap<Annot, Integer> annotsToRemove = new HashMap<>(items.size());
            // first find the annotations about to be removed, and raise event
            for (AnnotationDialogFragment.AnnotationInfo info : items) {
                if (info.getAnnotation() != null) {
                    annotsToRemove.put(info.getAnnotation(), info.getPageNum());
                }
            }
            if (!annotsToRemove.isEmpty()) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager != null) {
                    toolManager.raiseAnnotationsPreRemoveEvent(annotsToRemove);
                }
            }
        }
    }
}
package com.pdftron.pdf.dialog.pdflayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.common.PDFNetException;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.recyclertreeview.ItemMoveCallback;
import com.pdftron.recyclertreeview.PdfLayerNode;
import com.pdftron.recyclertreeview.PdfLayerNodeBinder;
import com.pdftron.recyclertreeview.PdfLayerTreeViewAdapter;
import com.pdftron.recyclertreeview.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class PdfLayerDialogFragment extends DialogFragment implements
        PdfLayerNodeBinder.PdfLayerNodeClickListener,
        PdfLayerTreeViewAdapter.OnPdfLayerTreeNodeListener {

    public static final String TAG = PdfLayerDialogFragment.class.getName();
    protected PDFViewCtrl mPdfViewCtrl;
    protected PdfLayerTreeViewAdapter mTreeViewAdapter;
    protected View mFragmentView;
    private final List<TreeNode<PdfLayerNode>> mSelectedNodes = new ArrayList<>();
    private final CompositeDisposable mDisposables = new CompositeDisposable();
    protected float mScale;

    /**
     * Returns a new instance of the class
     */
    public static PdfLayerDialogFragment newInstance() {
        return new PdfLayerDialogFragment();
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @return This class
     */
    public PdfLayerDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFragmentView = view;
    }

    public boolean isEmpty() {
        return mTreeViewAdapter == null || mTreeViewAdapter.getItemCount() == 0;
    }

    @Override
    public void onExpandNode(TreeNode<PdfLayerNode> selectedNode, int position) {
        int positionStart = mTreeViewAdapter.getExpandableStartPosition(selectedNode);
        if (!selectedNode.isExpand()) {
            // ui
            mTreeViewAdapter.notifyItemRangeInserted(positionStart, mTreeViewAdapter.addChildNodes(selectedNode, positionStart));
        } else {
            deselectChildren(selectedNode);
            if (mSelectedNodes.isEmpty()) {
                mTreeViewAdapter.setSelectionCount(mSelectedNodes.size());
                mTreeViewAdapter.notifyDataSetChanged();
            }
            // ui
            mTreeViewAdapter.notifyItemRangeRemoved(positionStart, mTreeViewAdapter.removeChildNodes(selectedNode, true));
        }
    }

    private void deselectChildren(TreeNode<PdfLayerNode> parent) {
        List<TreeNode<PdfLayerNode>> childList = parent.getChildList();
        if (childList == null) {
            return;
        }
        for (int i = 0; i < childList.size(); i++) {
            TreeNode<PdfLayerNode> childNode = childList.get(i);
            if (childNode.isExpand()) {
                deselectChildren(childNode);
            }
            if (childNode.getContent().mIsSelected) {
                childNode.getContent().mIsSelected = false;
                mSelectedNodes.remove(childNode);
            }
        }
    }

    @Override
    public void onNodeCheckBoxSelected(TreeNode<PdfLayerNode> treeNode, RecyclerView.ViewHolder viewHolder) {
        boolean isChecked = ((PdfLayerNodeBinder.ViewHolder) viewHolder).getCheckBox().isChecked();
        setLayerCheckedChange(treeNode, isChecked);
    }

    protected void setLayerCheckedChange(TreeNode<PdfLayerNode> treeNode, boolean isChecked) {
        try {
            treeNode.getContent().mIsSelected = isChecked;
            treeNode.getContent().getPdfLayer().setChecked(isChecked);
            PdfLayerUtils.setLayerCheckedChange(mPdfViewCtrl, treeNode.getContent().getPdfLayer().getGroup(), isChecked);
            if (treeNode.isExpand()) {
                mTreeViewAdapter.notifyDataSetChanged();
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    public boolean onClick(TreeNode<PdfLayerNode> node, RecyclerView.ViewHolder holder) {
        CheckBox checkBox = ((PdfLayerNodeBinder.ViewHolder) holder).getCheckBox();
        PdfLayer pdfLayer = node.getContent().getPdfLayer();
        if (!pdfLayer.isLocked() && pdfLayer.isChecked() != null && checkBox.isEnabled()) {
            checkBox.toggle();
            onNodeCheckBoxSelected(node, holder);
        }
        return true;
    }

    @Override
    public void onToggle(boolean isExpand, RecyclerView.ViewHolder holder) {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.controls_fragment_pdf_layer_dialog, container, false);
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return view;
        }

        Theme theme = Theme.fromContext(view.getContext());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerview_control_pdf_layer);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mScale = getResources().getDisplayMetrics().density;
        createAdaptor();
        mTreeViewAdapter.setTheme(theme);
        mTreeViewAdapter.setOnTreeNodeListener(this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemMoveCallback(mTreeViewAdapter));
        recyclerView.setAdapter(mTreeViewAdapter);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.action_pdf_layers);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        populateBookmarksTreeView();

        return view;
    }

    protected void createAdaptor() {
        mTreeViewAdapter = new PdfLayerTreeViewAdapter(new ArrayList<>(), Collections.singletonList(new PdfLayerNodeBinder(this)), mPdfViewCtrl, mScale);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposables.clear();
    }

    private void populateBookmarksTreeView() {
        if (mPdfViewCtrl != null) {
            boolean shouldUnlockRead = false;
            ArrayList<PdfLayer> pdfLayers = null;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                pdfLayers = PdfLayerUtils.getLayers(mPdfViewCtrl, mPdfViewCtrl.getDoc());
            } catch (PDFNetException ignored) {

            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            if (pdfLayers == null) {
                return;
            }
            mDisposables.add(buildPdfLayers(pdfLayers)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<List<TreeNode<PdfLayerNode>>>() {
                        @Override
                        public void accept(List<TreeNode<PdfLayerNode>> treeNodes) throws Exception {
                            mTreeViewAdapter.setItems(treeNodes);
                            mFragmentView.findViewById(R.id.control_outline_textview_empty).setVisibility(isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable));
                        }
                    }));
        }
    }

    private Observable<List<TreeNode<PdfLayerNode>>> buildPdfLayers(@NonNull ArrayList<PdfLayer> pdfLayers) {
        return Observable.create(new ObservableOnSubscribe<List<TreeNode<PdfLayerNode>>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<TreeNode<PdfLayerNode>>> emitter) throws Exception {
                if (mPdfViewCtrl != null) {
                    List<TreeNode<PdfLayerNode>> treeNodes = new ArrayList<>();
                    for (PdfLayer pdfLayer : pdfLayers) {
                        if (emitter.isDisposed()) {
                            emitter.onComplete();
                            return;
                        }
                        PdfLayerNode bNode = new PdfLayerNode(pdfLayer);
                        TreeNode<PdfLayerNode> treeNode = new TreeNode<>(bNode);
                        if (pdfLayer.hasChildren()) {
                            List<TreeNode<PdfLayerNode>> childNodes = PdfLayerTreeViewAdapter.buildPdfLayerTreeNodeList(mPdfViewCtrl, pdfLayer);
                            treeNode.setChildList(childNodes);
                            treeNode.expand();
                        }
                        treeNodes.add(treeNode);
                    }
                    emitter.onNext(treeNodes);
                }
            }
        });
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Theme {
        @ColorInt
        public final int textColor;
        @ColorInt
        public final int disabledTextColor;
        @ColorInt
        public final int secondaryTextColor;
        @ColorInt
        public final int checkBoxColor;
        @ColorInt
        public final int disabledCheckBoxColor;

        Theme(int textColor, int disabledTextColor, int secondaryTextColor, int checkBoxColor, int disabledCheckBoxColor) {
            this.textColor = textColor;
            this.disabledTextColor = disabledTextColor;
            this.secondaryTextColor = secondaryTextColor;
            this.checkBoxColor = checkBoxColor;
            this.disabledCheckBoxColor = disabledCheckBoxColor;
        }

        public static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.PdfLayerDialogTheme, R.attr.pt_pdf_layer_dialog_style, R.style.PTPdfLayerDialogTheme);

            int textColor = a.getColor(R.styleable.PdfLayerDialogTheme_textColor, Utils.getPrimaryTextColor(context));
            int disabledTextColor = a.getColor(R.styleable.PdfLayerDialogTheme_disabledTextColor, context.getResources().getColor(R.color.pt_disabled_state_color));
            int secondaryTextColor = a.getColor(R.styleable.PdfLayerDialogTheme_secondaryTextColor, context.getResources().getColor(R.color.pt_secondary_color));
            int checkBoxColor = a.getColor(R.styleable.PdfLayerDialogTheme_checkBoxColor, context.getResources().getColor(R.color.pt_accent_color));
            int disabledCheckBoxColor = a.getColor(R.styleable.PdfLayerDialogTheme_disabledCheckBoxColor, context.getResources().getColor(R.color.pt_disabled_state_color));
            a.recycle();

            return new Theme(textColor, disabledTextColor, secondaryTextColor, checkBoxColor, disabledCheckBoxColor);
        }
    }
}

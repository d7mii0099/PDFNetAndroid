package com.pdftron.pdf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class SavedSignatureAdapter extends SimpleRecyclerViewAdapter<File, SavedSignatureAdapter.ViewHolder> {

    private final WeakReference<Context> mContextRef;

    private List<File> mSignatureFiles = new ArrayList<>();

    private CompositeDisposable mDisposables = new CompositeDisposable();

    public SavedSignatureAdapter(@NonNull Context context, ViewHolderBindListener bindListener) {
        super(bindListener);

        mContextRef = new WeakReference<>(context);
    }

    public void setSignatures(@NonNull List<File> signatureFiles) {
        mSignatureFiles = new ArrayList<>(signatureFiles);
        notifyDataSetChanged();
    }

    @Override
    public File getItem(int position) {
        if (position < 0 || position >= mSignatureFiles.size()) {
            return null;
        }
        return mSignatureFiles.get(position);
    }

    @Override
    public void add(File item) {

    }

    @Override
    public boolean remove(File item) {
        return false;
    }

    @Override
    public File removeAt(int position) {
        // Delete both the signature PDF and thumbnail jpg
        File file = mSignatureFiles.get(position);
        File savedSignatureJpegFile = StampManager.getInstance().getSavedSignatureJpegFile(mContextRef.get(), file);
        if (savedSignatureJpegFile != null && savedSignatureJpegFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            savedSignatureJpegFile.delete();
        }
        boolean success = file.delete();
        if (success) {
            StampManager.getInstance().savedSignatureDeleted();
            return mSignatureFiles.remove(position);
        }
        return null;
    }

    @Override
    public void insert(File item, int position) {

    }

    @Override
    public void updateSpanCount(int count) {

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_signature, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        //noinspection RedundantThrows
        mDisposables.add(
                Single.just(getItem(position))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(new Function<File, File>() {
                            @Override
                            public File apply(File file) throws Exception {
                                return StampManager.getInstance().getSavedSignatureJpegFile(mContextRef.get(), file);
                            }
                        })
                        .subscribe(new Consumer<File>() {
                            @Override
                            public void accept(File jpgFile) throws Exception {
                                int[] dimensions = Utils.getImageFileDimensions(jpgFile, 1024);
                                Picasso.get()
                                        .load(jpgFile)
                                        .resize(dimensions[0], dimensions[1])
                                        .onlyScaleDown()
                                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                                        .into(holder.mImageView);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable));
                            }
                        })
        );
    }

    public void dispose() {
        mDisposables.clear();
    }

    @Override
    public int getItemCount() {
        return mSignatureFiles.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CardView mPreviewBackground;
        AppCompatImageView mImageView;

        ViewHolder(View itemView) {
            super(itemView);
            mPreviewBackground = itemView.findViewById(R.id.preview_background);
            mImageView = itemView.findViewById(R.id.stamp_image_view);
        }
    }
}

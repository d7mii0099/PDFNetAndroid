package com.pdftron.demo.browser.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.demo.browser.db.file.FileDataSource;
import com.pdftron.demo.browser.db.file.FileEntity;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.Utils;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

@SuppressWarnings("RedundantThrows")
public class FilesViewModel extends ViewModel {
    private static final String TAG = FilesViewModel.class.toString();

    // Keep reference to added and delete files to update the data later
    private final FileDataSource mDataSource;

    @NonNull
    private final CompositeDisposable mFetcherDisposables = new CompositeDisposable();
    @NonNull
    private final CompositeDisposable mDatabaseUpdaterDisposable = new CompositeDisposable();

    private final MutableLiveData<FileListState> mFileListStateLiveData = new MutableLiveData<>();

    private FilesViewModel(@NonNull FileDataSource repo) {
        mDataSource = repo;
    }

    /**
     * Factory to create a new {@link FilesViewModel}
     */
    @NonNull
    public static FilesViewModel from(@NonNull Fragment fragment,
            @NonNull FileDataSource dao) {
        return ViewModelProviders.of(fragment, new ListFilesViewModelFactory(dao))
                .get(FilesViewModel.class);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mFetcherDisposables.clear();
        mDatabaseUpdaterDisposable.clear();
    }

    public void clearFiles(@NonNull Consumer<Boolean> listener) {
        mDatabaseUpdaterDisposable.add(
                Single.just(mDataSource)
                        .map(new Function<FileDataSource, Boolean>() {
                            @Override
                            public Boolean apply(@NonNull FileDataSource dataSource) throws Exception {
                                Utils.throwIfOnMainThread();
                                dataSource.clear();
                                return true;
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(listener,
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        throw new RuntimeException(throwable);
                                    }
                                })
        );
    }

    void updateDatabase(@NonNull Context applicationContext, @NonNull DatabaseUpdater databaseUpdater) {
        mFetcherDisposables.clear();
        mFetcherDisposables.add(
                databaseUpdater.getFiles(applicationContext)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                mFileListStateLiveData.setValue(FileListState.LOADING_STARTED);
                            }
                        })
                        .doOnDispose(new Action() {
                            @Override
                            public void run() throws Exception {
                                mFileListStateLiveData.setValue(FileListState.LOADING_INTERRUPTED);
                            }
                        })
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean isFinished) throws Exception {
                                if (isFinished) {
                                    mFileListStateLiveData.setValue(FileListState.LOADING_FINISHED);
                                } else {
                                    mFileListStateLiveData.setValue(FileListState.LOADING_ERRORED);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                if (throwable instanceof DocumentFilesDatabaseUpdater.MissingRootDirectoryException) {
                                    mFileListStateLiveData.setValue(FileListState.MISSING_ROOT);
                                } else {
                                    throw new RuntimeException(throwable);
                                }
                            }
                        })
        );
    }

    LiveData<FileListState> getFileListState() {
        return mFileListStateLiveData;
    }

    void emitStateUpdate(@NonNull FileListState fileListState) {
        Utils.throwIfNotOnMainThread();
        mFileListStateLiveData.setValue(fileListState);
    }

    public void add(FileInfo fileInfo) {
        mDatabaseUpdaterDisposable.add(
                Single.just(fileInfo)
                        .observeOn(Schedulers.io())
                        .subscribe(new Consumer<FileInfo>() {
                            @Override
                            public void accept(FileInfo fileInfo) throws Exception {
                                mDataSource.add(fileInfo);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                throw new RuntimeException(throwable);
                            }
                        })
        );
    }

    public void delete(FileInfo fileInfo) {
        mDatabaseUpdaterDisposable.add(
                Single.just(fileInfo)
                        .observeOn(Schedulers.io())
                        .subscribe(new Consumer<FileInfo>() {
                            @Override
                            public void accept(FileInfo fileInfo) throws Exception {
                                mDataSource.delete(fileInfo);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                throw new RuntimeException(throwable);
                            }
                        })
        );
    }

    public Flowable<List<FileEntity>> getFilesFlowable(FileDataSource.QueryParams queryParams) {
        return mDataSource.getFilesFlowable(queryParams);
    }

    public enum FileListState {
        // Missing root folders event, used for Android Q
        MISSING_ROOT,
        // Recycler View loading events
        LOADING_FINISHED, LOADING_STARTED, LOADING_ERRORED, EMPTY_LIST, LOADING_INTERRUPTED,
        // Filtering events
        FILTER_NO_MATCHES, FILTER_FINISHED,
        // Search events
        SEARCH_NO_MATCHES,
    }

    static class ListFilesViewModelFactory implements ViewModelProvider.Factory {
        private FileDataSource mDao;

        ListFilesViewModelFactory(FileDataSource dao) {
            mDao = dao;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(FilesViewModel.class)) {
                return (T) new FilesViewModel(mDao);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}

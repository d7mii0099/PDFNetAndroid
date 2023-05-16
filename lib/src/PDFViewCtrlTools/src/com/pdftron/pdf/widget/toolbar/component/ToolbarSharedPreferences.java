package com.pdftron.pdf.widget.toolbar.component;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItemContent;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.data.ToolbarDatabase;
import com.pdftron.pdf.widget.toolbar.data.ToolbarEntity;
import com.pdftron.pdf.widget.toolbar.data.ToolbarItemEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * Class that controls toolbar shared preference settings such as: storing the last used tool,
 * storing the last used toolbar, storing the customized toolbars
 */
public class ToolbarSharedPreferences {

    private static final String KEY_LAST_OPENED_TOOLBAR = "custom_toolbar_last_opened_toolbar";
    private static final String KEY_LAST_USED_TOOL = "custom_toolbar_last_used_tool";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public LinkedHashMap<String, List<ToolbarItem>> getAllDefaultToolbarItems(@NonNull Context context) {
        LinkedHashMap<String, List<ToolbarItem>> result = new LinkedHashMap<>();
        result.put(DefaultToolbars.defaultAnnotateToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultAnnotateToolbar.getToolbarItems()));
        result.put(DefaultToolbars.defaultDrawToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultDrawToolbar.getToolbarItems()));
        result.put(DefaultToolbars.defaultFillAndSignToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultFillAndSignToolbar.getToolbarItems()));
        result.put(DefaultToolbars.defaultPrepareFormToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultPrepareFormToolbar.getToolbarItems()));
        result.put(DefaultToolbars.defaultInsertToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultInsertToolbar.getToolbarItems()));
        result.put(DefaultToolbars.defaultMeasureToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultMeasureToolbar.getToolbarItems()));
        result.put(DefaultToolbars.defaultPensToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultPensToolbar.getToolbarItems()));
        result.put(DefaultToolbars.defaultRedactionToolbar.getToolbarName(context), getCustomizableSublist(DefaultToolbars.defaultRedactionToolbar.getToolbarItems()));
        return result;
    }

    /**
     * Returns the given list of toolbar items excluding non customizable items. For now,
     * only edit toolbar button. Also remove duplicate tools.
     *
     * @param toolbarItems the list of toolbar items to filter
     * @return
     */
    public static List<ToolbarItem> getCustomizableSublist(List<ToolbarItem> toolbarItems) {
        List<ToolbarItem> itemsWithoutCustom = new ArrayList<>(toolbarItems);
        Iterator<ToolbarItem> itr = itemsWithoutCustom.listIterator();
        while (itr.hasNext()) {
            ToolbarItem item = itr.next();
            // Do not customize custom items and edit toolbar button
            if (item.toolbarButtonType == ToolbarButtonType.CUSTOM_CHECKABLE ||
                    item.toolbarButtonType == ToolbarButtonType.CUSTOM_UNCHECKABLE ||
                    item.toolbarButtonType == ToolbarButtonType.EDIT_TOOLBAR) {
                itr.remove();
            }
        }

        // Remove duplicates
        List<ToolbarItem> result = new ArrayList<>();
        HashSet<ToolbarButtonType> existingTypes = new HashSet<>();
        for (ToolbarItem item : itemsWithoutCustom) {
            if (!existingTypes.contains(item.toolbarButtonType)) {
                result.add(item);
                existingTypes.add(item.toolbarButtonType);
            }
        }
        return result;
    }

    public AnnotationToolbarBuilder getViewToolbar() {
        return DefaultToolbars.defaultViewToolbar.copy();
    }

    @WorkerThread
    public AnnotationToolbarBuilder getAnnotateToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultAnnotateToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getDrawToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultDrawToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getInsertToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultInsertToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getFillAndSignToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultFillAndSignToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getPrepareFormToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultPrepareFormToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getPensToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultPensToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getMeasureToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultMeasureToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getRedactToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultRedactionToolbar);
    }

    @WorkerThread
    public AnnotationToolbarBuilder getFavoriteToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultFavoriteToolbar, true);
    }

    public AnnotationToolbarBuilder getCompactAnnotateToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultAnnotateToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactDrawToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultDrawToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactInsertToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultInsertToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactFillAndSignToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultFillAndSignToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactPrepareFormToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultPrepareFormToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactPensToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultPensToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactMeasureToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultMeasureToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactRedactToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultRedactionToolbarCompact);
    }

    public AnnotationToolbarBuilder getCompactFavoriteToolbar(@NonNull Context context) {
        return getCustomToolbar(context, DefaultToolbars.defaultFavoriteToolbarCompact, true);
    }

    /**
     * Returns the custom toolbar stored in the database, defined by the original toolbar builder.
     *
     * @param context         for the database. This is usually the Application context.
     * @param originalToolbar the toolbar containing the custom toolbar's original toolbar items and its ordering
     * @return the toolbar stored in the data base, defined by the original toolbar builder.
     */
    @WorkerThread
    public AnnotationToolbarBuilder getCustomToolbar(@NonNull Context context, @NonNull AnnotationToolbarBuilder originalToolbar) {
        if (originalToolbar.getToolbarTag().equals(DefaultToolbars.defaultFavoriteToolbar.getToolbarTag())) {
            return getCustomToolbar(context, originalToolbar, true);
        } else {
            return getCustomToolbar(context, originalToolbar, false);
        }
    }

    @WorkerThread
    public AnnotationToolbarBuilder getCustomToolbar(@NonNull Context context, @NonNull AnnotationToolbarBuilder originalToolbar, boolean isFavorite) {

        ToolbarDatabase db = ToolbarDatabase.getInstance(context);

        String toolbarId = originalToolbar.getToolbarTag();
        String toolbarName = originalToolbar.getToolbarName(context);
        ToolbarEntity toolbarEntity = db.getToolbarDao().getToolbar(toolbarId);
        // Toolbar in db (not first launch), so we fetch it
        if (toolbarEntity != null) {
            Set<ToolbarItemEntity> toolbarItemEntities = new HashSet<>(db.getToolbarItemDao().getToolbarItemsFromToolbar(toolbarId));

            // Now reorder toolbar items in default toolbar depending on values from db
            // if favorite toolbar, then we not only reorder but insert new tools
            if (isFavorite) {
                return originalToolbar.copyWithNewToolbarItems(toolbarItemEntities);
            } else {
                return originalToolbar.copyWithNewOrder(toolbarItemEntities);
            }
        } else {
            // First time checking db, so add toolbar to db and all its items
            db.getToolbarDao().insertAll(new ToolbarEntity(toolbarId, toolbarName));

            List<ToolbarItem> toolbarBuilderItems = originalToolbar.getToolbarItems();
            List<ToolbarItemEntity> dbToolbarItemEntities = new ArrayList<>();
            for (int i = 0; i < toolbarBuilderItems.size(); i++) {
                ToolbarItem toolbarItem = toolbarBuilderItems.get(i);
                dbToolbarItemEntities.add(
                        new ToolbarItemEntity(
                                toolbarItem.buttonId,
                                toolbarId,
                                toolbarItem.order,
                                toolbarItem.toolbarButtonType.getValue())
                );
            }
            ToolbarItemEntity[] items = new ToolbarItemEntity[toolbarBuilderItems.size()];
            dbToolbarItemEntities.toArray(items);
            db.getToolbarItemDao().insertAll(items);
            return originalToolbar;
        }
    }

    public static Observable<Boolean> updateToolbarItemsInDb(
            @NonNull final Context context,
            @NonNull final String toolbarId,
            @NonNull final String toolbarName,
            @NonNull final List<MenuEditorItem> menuEditorItems,
            boolean shouldClear) {

        final List<ToolbarItemEntity> toolbarItemEntities = new ArrayList<>();

        int position = 0;
        for (MenuEditorItem menuEditorItem : menuEditorItems) {
            if (menuEditorItem instanceof MenuEditorItemContent) {
                MenuEditorItemContent content = (MenuEditorItemContent) menuEditorItem;
                int id = content.getId();
                ToolbarButtonType toolbarButtonType = content.getToolbarButtonType();
                if (toolbarButtonType != null) {
                    toolbarItemEntities.add(
                            new ToolbarItemEntity(
                                    id,
                                    toolbarId,
                                    position,
                                    toolbarButtonType.getValue())
                    );
                    position++;
                }
            }
        }

        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Boolean> emitter) throws Exception {
                try {
                    // Add items to our database
                    // First add toolbar, if it already exists then nothing will happen
                    ToolbarDatabase db = ToolbarDatabase.getInstance(context);
                    db.getToolbarDao().insertAll(new ToolbarEntity(toolbarId, toolbarName));

                    // Then add toolbar items, and replace existing ones
                    ToolbarItemEntity[] items = new ToolbarItemEntity[toolbarItemEntities.size()];
                    toolbarItemEntities.toArray(items);
                    db.getToolbarItemDao().clearAndInsertAll(toolbarId, items);

                    emitter.onNext(true);
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onNext(false);
                    emitter.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Boolean> clearToolbarItemsInDb(
            @NonNull final Context context,
            @NonNull final String toolbarId) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Boolean> emitter) throws Exception {
                try {
                    // Clear toolbar items from db
                    ToolbarDatabase db = ToolbarDatabase.getInstance(context);
                    List<ToolbarItemEntity> items = db.getToolbarItemDao().getToolbarItemsFromToolbar(toolbarId);
                    if (!items.isEmpty()) {
                        db.getToolbarItemDao().clear(toolbarId);
                        emitter.onNext(true);
                    } else {
                        emitter.onNext(false);
                    }
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onNext(false);
                    emitter.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    @Nullable
    public static String getLastOpenedToolbarTag(@NonNull Context context) {
        return PdfViewCtrlSettingsManager.getDefaultSharedPreferences(context).getString(KEY_LAST_OPENED_TOOLBAR, null);
    }

    public static void setLastOpenedToolbarTag(@NonNull Context context, @NonNull String toolbarTag) {
        SharedPreferences.Editor editor = PdfViewCtrlSettingsManager.getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_LAST_OPENED_TOOLBAR, toolbarTag);
        editor.apply();
    }

    public static int getLastUsedTool(@NonNull Context context) {
        return PdfViewCtrlSettingsManager.getDefaultSharedPreferences(context).getInt(KEY_LAST_USED_TOOL, -1);
    }

    public static void setLastUsedTool(@NonNull Context context, int buttonId) {
        SharedPreferences.Editor editor = PdfViewCtrlSettingsManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_LAST_USED_TOOL, buttonId);
        editor.apply();
    }
}

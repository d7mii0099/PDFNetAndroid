package com.pdftron.pdf.dialog.annotlist;

import androidx.annotation.NonNull;

import com.pdftron.pdf.widget.base.BaseObservable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Data class used to represent the annotation filter dialog state.
 */
@SuppressWarnings("unused")
public class AnnotationListFilterInfo extends BaseObservable {

    public enum FilterState {
        OFF, HIDE_ALL, ON, ON_LIST_ONLY
    }

    @NonNull
    private FilterState mFilterState;
    @NonNull
    private HashSet<TypeState> typesEnabledSet = new HashSet<>();
    @NonNull
    private HashSet<AuthorState> authorsEnabledSet = new HashSet<>();
    @NonNull
    private HashSet<StatusState> statusEnabledSet = new HashSet<>();
    @NonNull
    private HashSet<ColorState> colorsEnabledSet = new HashSet<>();

    private boolean isTypeEnabled;
    private boolean isAuthorEnabled;
    private boolean isStatusEnabled;
    private boolean isColorEnabled;

    public AnnotationListFilterInfo(@NonNull FilterState filterState) {
        setFilterState(filterState);
    }

    public void deselectAll() {
        for (TypeState typeState : typesEnabledSet) {
            typeState.selected = false;
        }
        for (AuthorState authorState : authorsEnabledSet) {
            authorState.selected = false;
        }
        for (StatusState statusState : statusEnabledSet) {
            statusState.selected = false;
        }
        for (ColorState colorState : colorsEnabledSet) {
            colorState.selected = false;
        }
        notifyChange();
    }

    public void clear() {
        typesEnabledSet.clear();
        authorsEnabledSet.clear();
        statusEnabledSet.clear();
        colorsEnabledSet.clear();
    }

    @NonNull
    public FilterState getFilterState() {
        return mFilterState;
    }

    public void setFilterState(@NonNull FilterState filterState) {
        mFilterState = filterState;
        switch (mFilterState) {
            case ON_LIST_ONLY:
            case ON:
                isTypeEnabled = true;
                isAuthorEnabled = true;
                isStatusEnabled = true;
                isColorEnabled = true;
                break;
            case HIDE_ALL:
            case OFF:
                isTypeEnabled = false;
                isAuthorEnabled = false;
                isStatusEnabled = false;
                isColorEnabled = false;
                break;
        }
        notifyChange();
    }

    public void toggleType(int type) {
        for (TypeState state : typesEnabledSet) {
            if (state.type == type) {
                state.selected = !state.selected;
                notifyChange();
                break;
            }
        }
    }

    public void toggleAuthor(String author) {
        for (AuthorState state : authorsEnabledSet) {
            if (state.name.equals(author)) {
                state.selected = !state.selected;
                notifyChange();
                break;
            }
        }
    }

    public void toggleStatus(String status) {
        for (StatusState state : statusEnabledSet) {
            if (state.status.equals(status)) {
                state.selected = !state.selected;
                notifyChange();
                break;
            }
        }
    }

    public void toggleColor(String color) {
        for (ColorState state : colorsEnabledSet) {
            if (state.color.equals(color)) {
                state.selected = !state.selected;
                notifyChange();
                break;
            }
        }
    }

    public void addType(boolean selected, int type) {
        typesEnabledSet.add(new TypeState(selected, type));
        notifyChange();
    }

    public void addAuthor(boolean selected, String author) {
        authorsEnabledSet.add(new AuthorState(selected, author));
        notifyChange();
    }

    public void addStatus(boolean selected, String status) {
        statusEnabledSet.add(new StatusState(selected, status));
        notifyChange();
    }

    public void addColor(boolean selected, String color) {
        colorsEnabledSet.add(new ColorState(selected, color));
        notifyChange();
    }

    public void removeType(int type) {
        Iterator<TypeState> iterator = typesEnabledSet.iterator();
        while (iterator.hasNext()) {
            TypeState state = iterator.next();
            if (state.type == type) {
                iterator.remove();
                notifyChange();
                break;
            }
        }
    }

    public void removeAuthor(String author) {
        Iterator<AuthorState> iterator = authorsEnabledSet.iterator();
        while (iterator.hasNext()) {
            AuthorState state = iterator.next();
            if (state.name.equals(author)) {
                iterator.remove();
                notifyChange();
                break;
            }
        }
    }

    public void removeStatus(String status) {
        Iterator<StatusState> iterator = statusEnabledSet.iterator();
        while (iterator.hasNext()) {
            StatusState state = iterator.next();
            if (state.status.equals(status)) {
                iterator.remove();
                notifyChange();
                break;
            }
        }
    }

    public void removeColor(boolean selected, String color) {
        Iterator<ColorState> iterator = colorsEnabledSet.iterator();
        while (iterator.hasNext()) {
            ColorState state = iterator.next();
            if (state.color.equals(color)) {
                iterator.remove();
                notifyChange();
                break;
            }
        }
    }

    public boolean containsType(int type) {
        for (TypeState typeState : typesEnabledSet) {
            if (typeState.type == type) {
                return true;
            }
        }
        return false;
    }

    public boolean containsStatus(@NonNull String status) {
        for (StatusState statusState : statusEnabledSet) {
            if (statusState.status.equals(status)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAuthor(@NonNull String author) {
        for (AuthorState authorState : authorsEnabledSet) {
            if (authorState.name.equals(author)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsColor(@NonNull String color) {
        for (ColorState colorState : colorsEnabledSet) {
            if (colorState.color.equals(color)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTypeEnabled() {
        return isTypeEnabled;
    }

    public boolean isAuthorEnabled() {
        return isAuthorEnabled;
    }

    public boolean isStatusEnabled() {
        return isStatusEnabled;
    }

    public boolean isColorEnabled() {
        return isColorEnabled;
    }

    public Boolean isTypeSelected(int type) {
        for (TypeState state : typesEnabledSet) {
            if (state.type == type) {
                return state.selected;
            }
        }
        return null;
    }

    public Boolean isAuthorSelected(@NonNull String author) {
        for (AuthorState state : authorsEnabledSet) {
            if (state.name.equals(author)) {
                return state.selected;
            }
        }
        return null;
    }

    public Boolean isStatusSelected(@NonNull String status) {
        for (StatusState state : statusEnabledSet) {
            if (state.status.equals(status)) {
                return state.selected;
            }
        }
        return null;
    }

    public Boolean isColorSelected(@NonNull String color) {
        for (ColorState state : colorsEnabledSet) {
            if (state.color.equals(color)) {
                return state.selected;
            }
        }
        return null;
    }

    public boolean isAnyTypeSelected() {
        for (TypeState state : typesEnabledSet) {
            if (state.selected) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyAuthorSelected() {
        for (AuthorState state : authorsEnabledSet) {
            if (state.selected) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyStatusSelected() {
        for (StatusState state : statusEnabledSet) {
            if (state.selected) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyColorSelected() {
        for (ColorState state : colorsEnabledSet) {
            if (state.selected) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    HashSet<TypeState> getTypeSet() {
        return typesEnabledSet;
    }

    @NonNull
    HashSet<AuthorState> getAuthorSet() {
        return authorsEnabledSet;
    }

    @NonNull
    HashSet<StatusState> getStatusSet() {
        return statusEnabledSet;
    }

    Set<ColorState> getColorSet() {
        // make this set unmodifiable, we don't want any other external classes to modify this otherwise
        // observers will not get notified
        return Collections.unmodifiableSet(colorsEnabledSet);
    }

    public void updateFilterOptions(HashSet<Integer> typeSet, HashSet<String> authorSet, HashSet<String> statusSet, HashSet<String> colorSet) {

        Iterator<TypeState> iterType = typesEnabledSet.iterator();
        while (iterType.hasNext()) {
            TypeState type = iterType.next();
            if (!typeSet.contains(type.type)) {
                iterType.remove();
            }
        }

        Iterator<AuthorState> iterAuthor = authorsEnabledSet.iterator();
        while (iterAuthor.hasNext()) {
            AuthorState author = iterAuthor.next();
            if (!authorSet.contains(author.name)) {
                iterAuthor.remove();
            }
        }

        Iterator<StatusState> iterStatus = statusEnabledSet.iterator();
        while (iterStatus.hasNext()) {
            StatusState status = iterStatus.next();
            if (!statusSet.contains(status.status)) {
                iterStatus.remove();
            }
        }

        Iterator<ColorState> iterColor = colorsEnabledSet.iterator();
        while (iterColor.hasNext()) {
            ColorState color = iterColor.next();
            if (!colorSet.contains(color.color)) {
                iterColor.remove();
            }
        }
    }
}

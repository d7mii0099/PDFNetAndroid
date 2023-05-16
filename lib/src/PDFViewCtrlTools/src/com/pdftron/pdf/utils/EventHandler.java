package com.pdftron.pdf.utils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Singleton instance class that sends viewer events to any listeners.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class EventHandler {
    public static final String ANNOT_LIST_FILTER_EVENT = "pdftron_annotation_list_filter";
    public static final String EDIT_OUTLINE_EVENT = "pdftron_edit_outline";
    public static final String FAVORITE_TOOLBAR_EVENT = "pdftron_favorite_toolbar";
    public static final String APPLY_REDACTION_EVENT = "pdftron_apply_redaction";
    public static final String FILE_PICKER_VISIBLE_EVENT = "pdftron_file_picker_visible";
    public static final String ANNOT_TOOLBAR_TOOL_EVENT = "pdftron_annot_toolbar_tool"; // for tool inside a toolbar
    public static final String ANNOT_TOOLBAR_EVENT = "pdftron_annot_toolbar"; // for toolbar switching
    public static final String REFLOW_TEXT_MARKUP = "pdftron_reflow_text_markup";

    public static final String ANNOT_TOOLBAR_BUTTON_TYPE_METADATA_KEY = "pdftron_toolbarButtonType"; // used as a key for metadata for annot toolbar button event
    public static final String TOOLBAR_METADATA_KEY = "pdftron_toolbar";

    private static volatile EventHandler _INSTANCE;
    @NonNull
    private final List<EventListener> mListeners = new ArrayList<>();

    public static EventHandler getInstance() {
        if (_INSTANCE == null) {
            synchronized (EventHandler.class) {
                if (_INSTANCE == null) {
                    _INSTANCE = new EventHandler();
                }
            }
        }
        return _INSTANCE;
    }

    public static String eventIdFromToolbar(@NonNull String toolbarId, @NonNull int buttonId) {
        return toolbarId + "_" + buttonId;
    }

    /**
     * Sends event with given event id to the event listeners. Called before the event occurs.
     *
     * @param eventId unique event id that represents the type of event
     * @return True if the listeners intercepted event, false otherwise
     */
    @MainThread
    public static boolean sendPreEvent(@NonNull String eventId) {
        return sendPreEvent(eventId, new HashMap<>());
    }

    /**
     * Sends event with given event id and metadata to the event listeners. Called before the event occurs.
     *
     * @param eventId unique event id that represents the type of event
     * @param metadata metadata to attach to event
     * @return True if the listeners intercepted event, false otherwise
     */
    @MainThread
    public static boolean sendPreEvent(@NonNull String eventId, @NonNull HashMap<String, String> metadata) {
        Utils.throwIfNotOnMainThread();
        EventHandler eventHandler = getInstance();
        boolean intercepted = false;
        for (EventListener listener: eventHandler.mListeners) {
            if (listener.sendPreEvent(new EventType(eventId, metadata))) {
                intercepted = true;
            }
        }

        return intercepted;
    }

    /**
     * Adds an event listener to listen for viewer events.
     *
     * @param listener {@link EventListener} that listeners for viewer events
     */
    // This must only be called by client and not within the library
    public static void addListener(@NonNull EventListener listener) {
        EventHandler eventHandler = getInstance();
        if (listener == null) {
            throw new RuntimeException("EventListener can not be null!");
        }
        eventHandler.mListeners.add(listener);
    }

    /**
     * Removes an event listener.
     *
     * @param listener to remove
     */
    public static void removeListener(@NonNull EventListener listener) {
        EventHandler eventHandler = getInstance();
        if (listener == null) {
            throw new RuntimeException("EventListener can not be null!");
        }
        eventHandler.mListeners.remove(listener);
    }

    /**
     * Encapsulates and event type.
     */
    public static class EventType {
        public final String id;
        public final HashMap<String, String> metadata;

        public EventType(@NonNull String id, @NonNull HashMap<String, String> metadata) {
            this.id = id;
            this.metadata = metadata;
        }
    }

    /**
     * Listener used to listen for viewer events.
     */
    public interface EventListener {
        /**
         * @return True if this listener should intercept the event, false otherwise.
         */
        boolean sendPreEvent(@NonNull EventType eventType);
    }

}

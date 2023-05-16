package com.pdftron.collab.ui.reply.component.header;

import com.pdftron.collab.ui.base.component.BaseUIEvent;
import com.pdftron.collab.ui.reply.model.ReplyHeader;

/**
 * Represents a user event from interaction the header in the annotation reply UI.
 */
public class HeaderEvent extends BaseUIEvent<HeaderEvent.Type, ReplyHeader> {

    HeaderEvent(HeaderEvent.Type eventType, ReplyHeader data) {
        super(eventType, data);
    }

    public enum Type {
        CLOSE_CLICKED,  // emitted when the close button is clicked
        LIST_CLICKED,   // emitted when the list button is tapped in the header
        ANNOT_COMMENT_MODIFY,   // emitted when the comment modify button has been tapped
        REVIEW_STATE_NONE_CLICKED, // emitted when a review state button is clicked
        REVIEW_STATE_ACCEPTED_CLICKED, // emitted when a review state button is clicked
        REVIEW_STATE_REJECTED_CLICKED, // emitted when a review state button is clicked
        REVIEW_STATE_CANCELLED_CLICKED, // emitted when a review state button is clicked
        REVIEW_STATE_COMPLETED_CLICKED // emitted when a review state button is clicked
    }
}

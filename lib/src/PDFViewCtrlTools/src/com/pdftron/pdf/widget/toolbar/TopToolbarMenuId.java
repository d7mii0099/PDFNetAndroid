package com.pdftron.pdf.widget.toolbar;

import com.pdftron.pdf.tools.R;

public enum TopToolbarMenuId {

    TABS(R.id.action_tabs),
    SEARCH(R.id.action_search),
    VIEW_MODE(R.id.action_viewmode),
    THUMBNAILS(R.id.action_thumbnails),
    OUTLINE(R.id.action_outline),
    UNDO(R.id.undo),
    SHARE(R.id.action_share),
    REFLOW_MODE(R.id.action_reflow_mode),
    EDIT_PAGES(R.id.action_editpages),
    EXPORT(R.id.action_export_options),
    PRINT(R.id.action_print),
    FILE_ATTACHMENT(R.id.action_file_attachment),
    OCG_LAYERS(R.id.action_pdf_layers),
    DIGITAL_SIGNATURES(R.id.action_digital_signatures),
    CLOSE_TAB(R.id.action_close_tab);

    private final int mId;

    TopToolbarMenuId(int id) {
        mId = id;
    }

    public int value() {
        return mId;
    }
}

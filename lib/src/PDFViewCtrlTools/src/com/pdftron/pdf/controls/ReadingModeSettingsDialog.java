package com.pdftron.pdf.controls;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Reflow;
import com.pdftron.pdf.tools.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReadingModeSettingsDialog extends DialogFragment {

    /**
     * @hide View mode entry key -- text
     */
    protected static final String KEY_ITEM_TITLE = "item_read_mode_title";
    /**
     * @hide View mode entry key -- id
     */
    protected static final String KEY_ITEM_ID = "item_read_mode_id";
    /**
     * @hide View mode entry key -- control
     */
    protected static final String KEY_ITEM_DESCRIPTION = "item_read_mode_description";

    /**
     * Hide background images
     */
    protected static final int ITEM_ID_HIDE_BACKGROUND_IMAGES = 200;
    /**
     * Hide Images under Text
     */
    protected static final int ITEM_ID_HIDE_IMAGES_UNDER_TEXT = 201;
    /**
     * Hide Images under Invisible Text
     */
    protected static final int ITEM_ID_HIDE_IMAGES_UNDER_INVISIBLE_TEXT = 202;
    /**
     * Hide Text over Images
     */
    protected static final int ITEM_ID_HIDE_TEXT_OVER_IMAGES = 203;

    private ReadingModeAdapter mReadingModeAdapter;
    private ReflowControl mReflowControl;

    /**
     * @return new instance of this class
     */
    public static ReadingModeSettingsDialog newInstance() {
        return new ReadingModeSettingsDialog();
    }

    public ReadingModeSettingsDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Activity activity = getActivity();
        if (activity != null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(R.layout.fragment_reading_mode_settings, null);

            List<HashMap<String, Object>> list = new ArrayList<>();
            list.add(createItem(ITEM_ID_HIDE_BACKGROUND_IMAGES, activity.getString(R.string.reading_mode_hide_background_images), activity.getString(R.string.reading_mode_hide_background_images_desc)));
            list.add(createItem(ITEM_ID_HIDE_IMAGES_UNDER_TEXT, activity.getString(R.string.reading_mode_hide_images_under_txt), activity.getString(R.string.reading_mode_hide_images_under_txt_desc)));
            list.add(createItem(ITEM_ID_HIDE_IMAGES_UNDER_INVISIBLE_TEXT, activity.getString(R.string.reading_mode_hide_images_under_invisible_txt), activity.getString(R.string.reading_mode_hide_images_under_invisible_txt_dec)));
            list.add(createItem(ITEM_ID_HIDE_TEXT_OVER_IMAGES, activity.getString(R.string.reading_mode_hide_text_over_images), activity.getString(R.string.reading_mode_hide_text_over_images_desc)));
            mReadingModeAdapter = new ReadingModeAdapter(activity, list);

            ListView readingModeListView = view.findViewById(R.id.reading_mode_listview);
            readingModeListView.setAdapter(mReadingModeAdapter);
            builder.setView(view);
            builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mReflowControl.clearAdapterCacheAndReset();
                    dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
        }
        return builder.create();
    }

    public void setReflowControl(ReflowControl reflowControl) {
        mReflowControl = reflowControl;
    }

    /**
     * Creates a read mode entry
     *
     * @param id          item id
     * @param title       item title
     * @param description item description
     * @return read mode entry
     */
    protected HashMap<String, Object> createItem(int id, String title, String description) {
        HashMap<String, Object> item = new HashMap<>();
        item.put(KEY_ITEM_ID, id);
        item.put(KEY_ITEM_TITLE, title);
        item.put(KEY_ITEM_DESCRIPTION, description);
        return item;
    }

    private class ReadingModeAdapter extends ArrayAdapter<HashMap<String, Object>> {
        private final List<HashMap<String, Object>> mEntries;

        public ReadingModeAdapter(@NonNull Context context, List<HashMap<String, Object>> entries) {
            super(context, 0, entries);
            this.mEntries = entries;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            final ReadingModeSettingsDialog.ReadingModeAdapter.ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_item_view_reading_mode, parent, false);

                holder = new ReadingModeSettingsDialog.ReadingModeAdapter.ViewHolder();
                holder.itemView = convertView.findViewById(R.id.reading_mode_main_layout);
                holder.title = convertView.findViewById(R.id.reading_mode_title);
                holder.description = convertView.findViewById(R.id.reading_mode_description);
                holder.checkBox = convertView.findViewById(R.id.reading_mode_checkBox);

                convertView.setTag(holder);
            } else {
                holder = (ReadingModeSettingsDialog.ReadingModeAdapter.ViewHolder) convertView.getTag();
            }

            HashMap<String, Object> map = this.mEntries.get(position);

            holder.title.setText((String) map.get(KEY_ITEM_TITLE));
            holder.description.setText((String) map.get(KEY_ITEM_DESCRIPTION));
            Integer id = (Integer) map.get(KEY_ITEM_ID);
            if (id != null) {
                switch (id) {
                    case ITEM_ID_HIDE_BACKGROUND_IMAGES:
                        holder.checkBox.setChecked(mReflowControl.getIsHideBackgroundImages());
                        break;
                    case ITEM_ID_HIDE_IMAGES_UNDER_TEXT:
                        holder.checkBox.setChecked(mReflowControl.getIsHideImagesUnderText());
                        break;
                    case ITEM_ID_HIDE_IMAGES_UNDER_INVISIBLE_TEXT:
                        holder.checkBox.setChecked(mReflowControl.getIsHideImagesUnderInvisibleText());
                        break;
                    case ITEM_ID_HIDE_TEXT_OVER_IMAGES:
                        holder.checkBox.setChecked(mReflowControl.getIsDoNotReflowTextOverImages());
                        break;
                }
            }

            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setItemState(map, holder);
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setItemState(map, holder);
                }
            });
            return convertView;
        }

        private class ViewHolder {
            protected ConstraintLayout itemView;
            protected TextView title;
            protected TextView description;
            protected CheckBox checkBox;
        }
    }

    private void setItemState(HashMap<String, Object> map, ReadingModeAdapter.ViewHolder holder) {
        Integer id = (Integer) map.get(KEY_ITEM_ID);
        if (id != null) {
            switch (id) {
                case ITEM_ID_HIDE_BACKGROUND_IMAGES:
                    holder.checkBox.setChecked(!mReflowControl.getIsHideBackgroundImages());
                    mReflowControl.setHideBackgroundImages(holder.checkBox.isChecked());
                    break;
                case ITEM_ID_HIDE_IMAGES_UNDER_TEXT:
                    holder.checkBox.setChecked(!mReflowControl.getIsHideImagesUnderText());
                    mReflowControl.setHideImagesUnderText(holder.checkBox.isChecked());
                    break;
                case ITEM_ID_HIDE_IMAGES_UNDER_INVISIBLE_TEXT:
                    holder.checkBox.setChecked(!mReflowControl.getIsHideImagesUnderInvisibleText());
                    mReflowControl.setHideImagesUnderInvisibleText(holder.checkBox.isChecked());
                    break;
                case ITEM_ID_HIDE_TEXT_OVER_IMAGES:
                    holder.checkBox.setChecked(!mReflowControl.getIsDoNotReflowTextOverImages());
                    mReflowControl.setDoNotReflowTextOverImages(holder.checkBox.isChecked());
                    break;
            }
        }
    }
}

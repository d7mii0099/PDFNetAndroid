package com.pdftron.demo.app.setting;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.pdf.utils.Utils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchSettingFragmentBase extends Fragment {

    public static final String START_PREFERENCE = "start_preference";

    RecyclerView mRecyclerView;
    PreferenceItemAdapter mAdapter;
    View mRootView;
    Map<String, PreferenceItem> mPrefMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.search_setting_fragment, container, false);
        setupListView(mRootView);
        setHasOptionsMenu(true);
        return mRootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        setupSearchView(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStop() {
        super.onStop();
        FragmentActivity activity = getActivity();
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        }
    }

    protected int getContainerId() {
        return R.id.settings_container;
    }

    protected void setupSearchView(@NonNull Menu menu) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            SearchView searchView = new SearchView(fragmentActivity);
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setIconified(false);
            searchView.setIconifiedByDefault(false);
            searchView.onActionViewExpanded();
            mAdapter.getFilter().filter(getEmptySearchString());
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText.isEmpty()) {
                        mAdapter.getFilter().filter(getEmptySearchString());
                    } else {
                        mAdapter.getFilter().filter(newText.trim());
                    }
                    return true;
                }
            });

            menu.add(getResources().getString(R.string.action_search)).setActionView(searchView)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    private String getEmptySearchString() {
        //.filter("") returns all records, so "EMPTY_EMPTY_EMPTY" is used to prevent return all records for empty search string
        return "EMPTY_EMPTY_EMPTY";
    }

    private void setupListView(View mRootView) {
        mPrefMap.clear();
        for (Map.Entry<Integer, String> entry : getSettingFragments().entrySet()) {
            loadPreferencesList(entry.getKey(), entry.getValue());
        }

        checkPreferencesVisibility();

        mRecyclerView = mRootView.findViewById(R.id.search_setting_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new PreferenceItemAdapter(new ArrayList<>(mPrefMap.keySet()));
        mRecyclerView.setAdapter(mAdapter);
    }

    protected Fragment getSettingsFragment(String selectedPreferenceCategory, String selectedPreferenceKey) {
        PreferenceFragmentCompat mPreferenceFragment = null;
        switch (selectedPreferenceCategory) {
            case SettingsManager.KEY_PREF_CATEGORY_GENERAL:
                mPreferenceFragment = new GeneralFragmentBase();
                break;
            case SettingsManager.KEY_PREF_CATEGORY_VIEWING:
                mPreferenceFragment = new ViewingFragmentBase();
                break;
            case SettingsManager.KEY_PREF_CATEGORY_TABS:
                mPreferenceFragment = new TabsFragmentBase();
                break;
            case SettingsManager.KEY_PREF_CATEGORY_ANNOTATING:
                mPreferenceFragment = new AnnotatingFragmentBase();
                break;
            case SettingsManager.KEY_PREF_CATEGORY_STYLUS:
                mPreferenceFragment = new StylusFragmentBase();
                break;
            case SettingsManager.KEY_PREF_CATEGORY_ABOUT:
                mPreferenceFragment = new AboutFragmentBase();
                break;
        }

        if (mPreferenceFragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString(START_PREFERENCE, selectedPreferenceKey);
            mPreferenceFragment.setArguments(bundle);
        }
        return mPreferenceFragment;
    }

    protected Map<Integer, String> getSettingFragments() {
        Map<Integer, String> settingFragmentsMap = new HashMap<>();
        settingFragmentsMap.put(R.xml.setting_general_preferences, SettingsManager.KEY_PREF_CATEGORY_GENERAL);
        settingFragmentsMap.put(R.xml.setting_viewing_preferences, SettingsManager.KEY_PREF_CATEGORY_VIEWING);
        settingFragmentsMap.put(R.xml.setting_tabs_preferences, SettingsManager.KEY_PREF_CATEGORY_TABS);
        settingFragmentsMap.put(R.xml.setting_annotating_preferences, SettingsManager.KEY_PREF_CATEGORY_ANNOTATING);
        settingFragmentsMap.put(R.xml.setting_stylus_preferences, SettingsManager.KEY_PREF_CATEGORY_STYLUS);
        settingFragmentsMap.put(R.xml.setting_about_preferences, SettingsManager.KEY_PREF_CATEGORY_ABOUT);
        return settingFragmentsMap;
    }

    private void loadPreferencesList(int xmlId, String category) {

        String namespace = "http://schemas.android.com/apk/res/android";
        XmlResourceParser mPreferences = getResources().getXml(xmlId);

        try {
            while (mPreferences.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (mPreferences.getName() != null &&
                        !mPreferences.getName().equals("PreferenceScreen") &&
                        mPreferences.getAttributeCount() > 0 &&
                        mPreferences.getAttributeValue(namespace, "title") != null) {

                    if (mPreferences.getName().equals("PreferenceCategory") ||
                            mPreferences.getName().contains("Preference")) {
                        PreferenceItem mPreferenceItem = new PreferenceItem();
                        mPreferenceItem.setKey(mPreferences.getAttributeValue(namespace, "key"));
                        mPreferenceItem.setTitle(
                                getResources().getString(Integer.parseInt(
                                        mPreferences.getAttributeValue(namespace, "title").substring(1))));
                        mPreferenceItem.setCategory(category);
                        mPrefMap.put(mPreferenceItem.getTitle(), mPreferenceItem);
                    }
                }
                mPreferences.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void checkPreferencesVisibility() {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            if (!Utils.isChromebook(fragmentActivity)) {
                removePreference(SettingsManager.KEY_PREF_DESKTOP_UI_MODE);
            }

            if (Utils.isTablet(fragmentActivity)) {
                removePreference(SettingsManager.KEY_PREF_NEW_UI_SHOW_TAB_BAR_PHONE);
            } else {
                removePreference(SettingsManager.KEY_PREF_NEW_UI_SHOW_TAB_BAR);
            }

            if (!Utils.isKitKat()) {
                removePreference(SettingsManager.KEY_PREF_FULL_SCREEN_MODE);
            }

            if (!Utils.isMarshmallow()) {
                removePreference(SettingsManager.KEY_PREF_SCROLLBAR_GUIDELINE);
            }

            if (!Utils.isPie()) {
                removePreference(SettingsManager.KEY_PREF_FOLLOW_SYSTEM_DARK_MODE);
            }
        }
    }

    protected void removePreference(String preference) {
        String prefKeyToRemove = "";
        for (Map.Entry<String, PreferenceItem> entry : mPrefMap.entrySet()) {
            if (entry.getValue().key != null && entry.getValue().key.equals(preference)) {
                prefKeyToRemove = entry.getKey();
                break;
            }
        }
        if (!prefKeyToRemove.isEmpty())
            mPrefMap.remove(prefKeyToRemove);
    }

    private class PreferenceItemAdapter extends RecyclerView.Adapter<PreferenceItemAdapter.PreferenceItemViewHolder>
            implements Filterable {
        private List<String> mPrefList;
        private List<String> mPrefListFiltered;

        public PreferenceItemAdapter(List<String> prefList) {
            mPrefList = prefList;
            mPrefListFiltered = prefList;
        }

        @NonNull
        @Override
        public PreferenceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    android.R.layout.simple_list_item_1, parent, false);
            return new PreferenceItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PreferenceItemViewHolder viewHolder, int position) {
            if (position < getItemCount()) {
                TextView textView = (TextView) viewHolder.itemView;
                textView.setText(mPrefListFiltered.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return mPrefListFiltered.size();
        }

        private class PreferenceItemViewHolder extends RecyclerView.ViewHolder {
            public PreferenceItemViewHolder(View view) {
                super(view);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PreferenceItem preferenceItem = mPrefMap.get(mPrefListFiltered.get(getAdapterPosition()));
                        FragmentActivity fragmentActivity = getActivity();
                        if (preferenceItem == null || fragmentActivity == null) {
                            return;
                        }
                        fragmentActivity.getSupportFragmentManager().beginTransaction()
                                .replace(getContainerId(), getSettingsFragment(preferenceItem.getCategory(), preferenceItem.getKey()))
                                .addToBackStack(null)
                                .commit();

                        ActionBar actionBar = ((AppCompatActivity) fragmentActivity).getSupportActionBar();
                        if (actionBar != null) {
                            String categoryName = getResources().getString(getResources()
                                    .getIdentifier(preferenceItem.getCategory(), "string", fragmentActivity.getPackageName()));
                            actionBar.setTitle(categoryName);
                        }
                    }
                });
            }
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();
                    if (charString.isEmpty()) {
                        mPrefListFiltered = mPrefList;
                    } else {
                        List<String> filteredList = new ArrayList<>();
                        for (String row : mPrefList) {
                            if (row.toLowerCase().contains(charString.toLowerCase()) || row.contains(charSequence)) {
                                filteredList.add(row);
                            }
                        }
                        Collections.sort(filteredList);
                        mPrefListFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = mPrefListFiltered;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    if (filterResults.values instanceof ArrayList) {
                        mPrefListFiltered = (ArrayList<String>) filterResults.values;
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }

    private static class PreferenceItem {
        private String key;
        private String title;
        private String category;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
}

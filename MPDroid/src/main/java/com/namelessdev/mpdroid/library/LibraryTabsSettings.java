/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.namelessdev.mpdroid.library;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.namelessdev.mpdroid.MPDApplication;
import com.namelessdev.mpdroid.R;
import com.namelessdev.mpdroid.adapters.SeparatedListAdapter;
import com.namelessdev.mpdroid.adapters.SeparatedListDataBinder;
import com.namelessdev.mpdroid.tools.LibraryTabsUtil;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class LibraryTabsSettings extends PreferenceActivity {

    private final MPDApplication mApp = MPDApplication.getInstance();

    public DragSortListView.DropListener mDropListener = new DragSortListView.DropListener() {

        public void drop(int from, int to) {
            if (from == to) {
                return;
            }
            Object item = mTabList.get(from);
            mTabList.remove(from);
            mTabList.add(to, item);
            if (getVisibleTabs().size() == 0) {
                // at least one tab should be visible so revert the changes
                mTabList.remove(to);
                mTabList.add(from, item);
            } else {
                saveSettings();
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private SeparatedListAdapter mAdapter;

    private ArrayList<Object> mTabList;

    private ArrayList<String> getVisibleTabs() {
        ArrayList<String> visibleTabs = new ArrayList<String>();
        // item 0 is a separator so we start with 1
        for (int i = 1; i < mTabList.size(); i++) {
            // if the item is a separator break
            if (mTabList.get(i) instanceof String) {
                break;
            }
            // if item is a TabItem add it to the list
            if (mTabList.get(i) instanceof TabItem) {
                visibleTabs.add(((TabItem) mTabList.get(i)).mText);
            }
        }
        return visibleTabs;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs_settings);

        refreshTable();

        DragSortListView mList;
        mList = (DragSortListView) getListView();
        mList.setDropListener(mDropListener);

        final DragSortController controller = new DragSortController(mList);
        controller.setDragHandleId(R.id.text1);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setDragInitMode(1);

        mList.setFloatViewManager(controller);
        mList.setOnTouchListener(controller);
        mList.setDragEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mpd_librarytabsmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                LibraryTabsUtil.resetLibraryTabs();
                refreshTable();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        mApp.setActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mApp.unsetActivity(this);
    }

    private void refreshTable() {
        // get a list of all tabs
        ArrayList<String> allTabs = LibraryTabsUtil.getAllLibraryTabs();

        // get a list of all currently visible tabs
        ArrayList<String> currentTabs = LibraryTabsUtil.
                getCurrentLibraryTabs();

        // create a list of all currently hidden tabs
        ArrayList<String> hiddenTabs = new ArrayList<String>();
        for (String tab : allTabs) {
            // add all items not in currentTabs
            if (!currentTabs.contains(tab)) {
                hiddenTabs.add(tab);
            }
        }

        mTabList = new ArrayList<Object>();
        // add a separator
        mTabList.add(getString(R.string.visibleTabs));
        // add all visible tabs
        for (int i = 0; i < currentTabs.size(); i++) {
            mTabList.add(new TabItem(currentTabs.get(i)));
        }
        // add a separator
        mTabList.add(getString(R.string.hiddenTabs));
        // add all hidden tabs
        for (int i = 0; i < hiddenTabs.size(); i++) {
            mTabList.add(new TabItem(hiddenTabs.get(i)));
        }
        mAdapter = new SeparatedListAdapter(this,
                R.layout.library_tabs_settings_item, new TabListDataBinder(),
                mTabList);
        setListAdapter(mAdapter);
    }

    private void saveSettings() {
        LibraryTabsUtil.saveCurrentLibraryTabs(getVisibleTabs());
    }
}

class TabItem {

    String mText;

    TabItem(String text) {
        this.mText = text;
    }
}

class TabListDataBinder implements SeparatedListDataBinder {

    public boolean isEnabled(int position, List<?> items, Object item) {
        return true;
    }

    public void onDataBind(Context context, View targetView,
            List<?> items, Object item, int position) {
        ((TextView) targetView).setText(LibraryTabsUtil.getTabTitleResId(((TabItem) item).mText));
    }

}

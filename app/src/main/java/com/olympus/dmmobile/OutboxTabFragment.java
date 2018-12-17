package com.olympus.dmmobile;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;
import android.widget.ListView;

/**
 * OutboxTabFragment is the Fragment class used for the outbox tab.
 * 
 * @version 1.0.1
 */
public class OutboxTabFragment extends Fragment {
	
	private ListView mListView = null;
	private DatabaseHandler mDbHandler = null;
	private static CustomCursorAdapter mListAdapter = null;
	private Cursor mCursor = null;
	private DMApplication mDMApplication = null;
	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
    	
    	View view = inflater.inflate(R.layout.fragment_outbox_tab, container, false);
    	
    	mListView = (ListView)view.findViewById(R.id.lstOutbox);
    	mListView.setTextFilterEnabled(true);
    	mDMApplication = (DMApplication) getActivity().getApplication();
    	mDbHandler = mDMApplication.getDatabaseHandler();
    	
    	return view;
    }
    
    @Override
    public void onActivityCreated(Bundle bundle) {
		super.onActivityCreated(bundle);
		mCursor = mDbHandler.getOutboxDictations();
		mListAdapter = new CustomCursorAdapter(getActivity(), mCursor);
		if (mListAdapter != null&&mListView!=null) {
			mListView.setAdapter(mListAdapter);
			mListAdapter.setFilterQueryProvider(new FilterQueryProvider() {
				@Override
				public Cursor runQuery(CharSequence constraint) {
					String partialValue = mDMApplication.editStringHasEscape(constraint.toString());
					if (mDMApplication.isPriorityOn())
						mCursor = mDbHandler.getSearchFilteredAndPrioritisedOutboxDictations(partialValue);
					else
						mCursor = mDbHandler.getSearchFilteredOutboxDictations(partialValue);
					return mCursor;
				}
			});
			if (mCursor != null && mDMApplication.getTabPos() == 1) {
				if (mCursor.getCount() > 0)
					mListAdapter.updateSendDeleteButtons(true);
				else
					mListAdapter.updateSendDeleteButtons(false);
			}
		}
	}
    /**
     * Used to get Dictation list adapter
     * @return Adapter as CustomCursorAdapter.
     */
    public CustomCursorAdapter getListAdapter() {
    	return mListAdapter;
    }
    
    /**
     * Used to refresh Outbox List.
     * @param mCursor New cursor to update list.
     * @param mCursorEnable Cursor to enable views.
     */
    public void onRefreshList(Cursor mCursor,Cursor mCursorEnable) {
    	if(mListAdapter != null) {

				mListAdapter.changeCursor(mCursor);
				mListAdapter.notifyDataSetChanged();
				if(mCursorEnable!=null) {
					if(mCursorEnable.getCount()>0)
						mListAdapter.updateSendDeleteButtons(true);
					else
						mListAdapter.updateSendDeleteButtons(false);
				}



    	}
    }
    
}

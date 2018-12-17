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
 * SendTabFragment is the Fragment class used for the Sent tab.
 * 
 * @version 1.0.1
 */
public class SendTabFragment extends Fragment {
	
	private ListView mListView = null;
	private DatabaseHandler mDbHandler = null;
	private static CustomCursorAdapter mListAdapter = null;
	private Cursor mCursor = null;
	private DMApplication mDMApplication = null;
	
	public void onCreate(Bundle savedInstanceState){ 
		super.onCreate(savedInstanceState); 
	} 
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_send_tab, container, false);
		
		mListView = (ListView)view.findViewById(R.id.lstSend);
		mDMApplication = (DMApplication) getActivity().getApplication();
		mDbHandler = mDMApplication.getDatabaseHandler();
		
		return view;
	}
	
    @Override
    public void onStart(){
    	super.onStart();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) { 
    	super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onActivityCreated(Bundle bundle) {
    	super.onActivityCreated(bundle);
    	mCursor = mDbHandler.getSentDictations();
    	mListAdapter = new CustomCursorAdapter(getActivity(), mCursor);
    	mListView.setAdapter(mListAdapter);
    	mListView.setSelector(android.R.color.transparent);
    	mListAdapter.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				String partialValue = mDMApplication.editStringHasEscape(constraint.toString());
				if(mDMApplication.isPriorityOn())
					mCursor = mDbHandler.getSearchFilteredAndPrioritisedSentDictations(partialValue);
				else
					mCursor = mDbHandler.getSearchFilteredSentDictations(partialValue);
	            return mCursor;
			}
		});
    	if(mCursor!=null&&mDMApplication.getTabPos()==2)
    	{
	    	if(mCursor.getCount()>0)
				mListAdapter.updateSendDeleteButtons(true);
			else
				mListAdapter.updateSendDeleteButtons(false);
    	}
    }
    /**
     * Used to get Dictation list adapter.
     * @return Adapter as CustomCursorAdapter.
     */
    public CustomCursorAdapter getListAdapter(){
    	if(mListAdapter==null)
		{mListAdapter = new CustomCursorAdapter(getActivity(), mCursor);

		}
    	return mListAdapter;
    }
    /**
     * Used to refresh Sent List.
     * @param mCursor New cursor to update list.
     * @param mCursorEnable Cursor to enable views.
     */
    public void onRefreshList(Cursor mCursor,Cursor mCursorEnable)
    {if(mListAdapter!=null)
	{
		mListAdapter.changeCursor(mCursor);
		mListAdapter.notifyDataSetChanged();
	}

    	if(mCursorEnable!=null)
    	{
	    	if(mCursorEnable.getCount()>0)
	    		if(mListAdapter!=null)
				{
					mListAdapter.updateSendDeleteButtons(true);
				}

			else
				{
				if(mListAdapter!=null)
				{
					mListAdapter.updateSendDeleteButtons(false);
				}
				}

    	}
    }
}

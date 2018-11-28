package com.olympus.dmmobile.settings;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.olympus.dmmobile.DMApplication;
import com.olympus.dmmobile.R;

import java.util.ArrayList;

/**
 * Custom adapter used with the Worktype list view in Settings.
 * 
 * @version 1.0.1
 */
public class WorktypeCustomAdapter extends BaseAdapter {

	private Activity mContext;
	private ViewHolder holder=null;
	private LayoutInflater mInflater;
	private ArrayList<String> mList;
	private String mailWorktype=null;
	private SharedPreferences.Editor editor ;
	private DMApplication dmApplication = null;
	
	public WorktypeCustomAdapter(Activity context, ArrayList<String> list){
		this.mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mList = list;
		dmApplication=(DMApplication)context.getApplication();
	}
	
	public void setList(ArrayList<String> list){
		this.mList = list;
		
	}
	
	
	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("ResourceAsColor")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			convertView = mInflater.inflate(R.layout.worktype_list_row, null);	
	        holder = new ViewHolder();
	        holder.text = (TextView)convertView.findViewById(R.id.text_view);
	        holder.delete = (Button)convertView.findViewById(R.id.btn_delete);
	        holder.delete.setTextColor(R.color.black);

	        holder.delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String tag = (String) v.getTag();
					mList.remove(tag);
					notifyDataSetChanged();
					for(int i=0;i<mList.size();i++)
					{
						if(mailWorktype==null)
							mailWorktype=mList.get(i);
						else
							mailWorktype=mailWorktype+":"+mList.get(i);
					}
				    setMailWorktype();
				    holder.delete.setVisibility(View.INVISIBLE);
				    dmApplication.worktypeSwiped = "";
				}
			});
	        convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.delete.setTag(getItem(position));
		holder.text.setText(mList.get(position));
		//System.out.println(getItem(position)+"  tag  "+holder.delete.getTag()+"    pos     "+position);
		if((holder.delete.getTag()+"").equalsIgnoreCase(dmApplication.worktypeSwiped))
			holder.delete.setVisibility(View.VISIBLE);
		else
			holder.delete.setVisibility(View.INVISIBLE);
		
		
		return convertView;
	}
	/**
	 * set worktypes into shared preference
	 */
	public void setMailWorktype() {

	     SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		 editor = sharedPref.edit();
		 editor.putString(mContext.getResources().getString(R.string.Worktype_Email_Key),mailWorktype );
		 editor.commit();
		 mailWorktype=null;
	}

	private static class ViewHolder{
		TextView text = null;
		Button delete = null;
	}
}


package com.olympus.dmmobile;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

/**
 * This class is an adapter for ViewPager that holds the fragments PendingTabFragment, OutboxTabFragment and SendTabFragment
 * @version 1.0.1
 *
 */
public class ViewPagerAdapter extends FragmentStatePagerAdapter {
	private List<Fragment> fragments=null;
	private FragmentManager fragmentManager=null;
    public ViewPagerAdapter(FragmentManager fragmentManager,List<Fragment> fragments) {
        super(fragmentManager);
        this.fragments=fragments;
        this.fragmentManager=fragmentManager;
    }
    @Override
    public Fragment getItem(int position) {
    	fragmentManager.beginTransaction().commitAllowingStateLoss();
    	return fragments.get(position);
    }
    @Override
    public int getCount() {
        return fragments.size();
    }
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object)
    {
    	super.setPrimaryItem(container,0,fragments.get(0));
    }
    @Override
    public void notifyDataSetChanged()
    {
    	super.notifyDataSetChanged();
    }
    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
    	try
    	{
	    	fragmentManager.executePendingTransactions();
	    	// fragmentManager.saveFragmentInstanceState(fragments.get(position));
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
}

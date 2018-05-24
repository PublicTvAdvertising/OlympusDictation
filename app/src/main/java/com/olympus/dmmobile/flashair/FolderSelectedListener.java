package com.olympus.dmmobile.flashair;

/**
 * FolderSelectedListener listens for folder selection.
 * 
 * @version 1.0.1
 */
public interface FolderSelectedListener 
{
	/**
	 * Invokes when a folder is selected.
	 * @param url Url to fetch folder information.
	 */
	public void onNewFolderSelected(String url);
}

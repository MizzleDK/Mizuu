package com.miz.mizuu.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.miz.db.DbAdapterSources;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class FileBrowserFragment extends Fragment {

	private ArrayList<File> showingFiles = new ArrayList<File>(), showingParents = new ArrayList<File>();
	private File currentFolder;
	private ListView parent, current;
	private Button addSource;
	private LinearLayout content;
	private ProgressBar pbar;
	private boolean isMovie, isLoading;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public FileBrowserFragment() {}

	public static FileBrowserFragment newInstance(String path, boolean isMovie) {
		FileBrowserFragment frag = new FileBrowserFragment();
		Bundle b = new Bundle();
		b.putString("path", path);
		b.putBoolean("isMovie", isMovie);
		frag.setArguments(b);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);

		currentFolder = new File(getArguments().getString("path"));
		isMovie = getArguments().getBoolean("isMovie");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.filebrowser, container, false);

		content = (LinearLayout) v.findViewById(R.id.content);
		pbar = (ProgressBar) v.findViewById(R.id.progress);
		addSource = (Button) v.findViewById(R.id.ok);
		addSource.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();
				if (isMovie) {
					dbHelper.createSource(currentFolder.getAbsolutePath(), DbAdapterSources.KEY_TYPE_MOVIE, FileSource.FILE, "", "", "");
					getActivity().setResult(0); // Movie
				} else {
					dbHelper.createSource(currentFolder.getAbsolutePath(), DbAdapterSources.KEY_TYPE_SHOW, FileSource.FILE, "", "", "");
					getActivity().setResult(1); // Show
				}
				getActivity().finish();
				return;
			}
		});
		parent = (ListView) v.findViewById(R.id.parent);
		parent.setAdapter(new ParentAdapter());
		parent.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 == 0) {
					if (currentFolder.getParentFile() != null)
						if (currentFolder.getParentFile().getParentFile() != null)
							currentFolder = currentFolder.getParentFile().getParentFile();
				} else {
					currentFolder = showingParents.get(arg2 - 1);
				}
				browseFolder(currentFolder);
			}
		});
		current = (ListView) v.findViewById(R.id.current);
		current.setAdapter(new CurrentFolderAdapter());
		current.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 == 0) {
					if (currentFolder.getParentFile() != null)
						currentFolder = currentFolder.getParentFile();
				} else {
					currentFolder = showingFiles.get(arg2 - 1);
				}
				browseFolder(currentFolder);
			}
		});

		browseFolder(currentFolder);

		return v;
	}

	private void setLoading(boolean loading) {
		isLoading = loading;
		if (loading) {
			pbar.setVisibility(View.VISIBLE);
			content.setVisibility(View.GONE);
		} else {
			pbar.setVisibility(View.GONE);
			content.setVisibility(View.VISIBLE);
		}	
	}

	private void browseFolder(final File folder) {
		if (!isLoading && folder.isDirectory())
			new Thread() {
			@Override
			public void run() {

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setLoading(true);
					}});

				final ArrayList<File> currentFiles = new ArrayList<File>();	
				File[] listFiles = folder.listFiles();
				if (listFiles != null)
					for (int i = 0; i < listFiles.length; i++)
						currentFiles.add(listFiles[i]);

				sort(currentFiles);

				showingFiles.clear();
				showingFiles.addAll(currentFiles);

				final ArrayList<File> parentFolders = new ArrayList<File>();
				File parentFile = folder.getParentFile();
				if (parentFile != null) {
					File[] parentList = parentFile.listFiles();
					if (parentList != null)
						for (int i = 0; i < parentList.length; i++)
							if (parentList[i].isDirectory())
								parentFolders.add(parentList[i]);
				}

				sort(parentFolders);

				showingParents.clear();
				showingParents.addAll(parentFolders);

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((CurrentFolderAdapter) current.getAdapter()).setFiles(currentFiles);
						current.setSelection(0);
						((ParentAdapter) parent.getAdapter()).setFiles(parentFolders);
						parent.setSelection(0);
						getActivity().getActionBar().setSubtitle(currentFolder.getAbsolutePath());
						setLoading(false);
					}}
						);
			}
		}.start();
	}

	private void sort(List<File> list) {
		Collections.sort(list, new Comparator<File>() {  
			public int compare(File f1, File f2) {  
				if (f1.isDirectory() && !f2.isDirectory()) {  
					// Directory before non-directory  
					return -1;  
				} else if (!f1.isDirectory() && f2.isDirectory()) {  
					// Non-directory after directory  
					return 1;  
				} else {  
					// Alphabetic order otherwise  
					return f1.getName().compareToIgnoreCase(f2.getName());  
				}  
			}}
				);
	}

	static class ViewHolder {
		TextView name, size;
		ImageView icon;
	}

	private class ParentAdapter extends BaseAdapter {

		private List<File> files = new ArrayList<File>();
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public void setFiles(List<File> l) {
			files.clear();
			files.addAll(l);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return files.size() + 1;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.file_list_item, null);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.text1);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			if (position == 0) {
				holder.name.setText("...");
				holder.icon.setImageResource(R.drawable.up);
			} else {
				holder.name.setText(files.get(position - 1).getName());
				holder.icon.setImageResource(R.drawable.folder);

				if (currentFolder.getName().equals(files.get(position - 1).getName()))
					convertView.setBackgroundResource(R.drawable.bg);
				else
					convertView.setBackgroundColor(Color.TRANSPARENT);
			}
			holder.size.setVisibility(View.GONE);

			return convertView;
		}
	}

	private class CurrentFolderAdapter extends BaseAdapter {

		private List<File> files = new ArrayList<File>();
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public void setFiles(List<File> l) {
			files.clear();
			files.addAll(l);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return files.size() + 1;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.file_list_item, null);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.text1);
				holder.size = (TextView) convertView.findViewById(R.id.size);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			if (position == 0) {
				holder.name.setText("...");
				holder.icon.setImageResource(R.drawable.up);
				holder.size.setVisibility(View.GONE);
			} else {
				holder.name.setText(files.get(position - 1).getName());
				holder.size.setText(MizLib.filesizeToString(files.get(position - 1).length()));
				if (files.get(position - 1).isDirectory()) {
					holder.icon.setImageResource(R.drawable.folder);
					holder.size.setText("");
					holder.size.setVisibility(View.GONE);
				} else {
					holder.icon.setImageResource(R.drawable.file);
					holder.size.setText(MizLib.filesizeToString(files.get(position - 1).length()));
					holder.size.setVisibility(View.VISIBLE);
				}
			}

			return convertView;
		}
	}

	public void onBackPressed() {
		if (currentFolder.getParentFile() != null) {
			currentFolder = currentFolder.getParentFile();
			browseFolder(currentFolder);
		}
	}
}
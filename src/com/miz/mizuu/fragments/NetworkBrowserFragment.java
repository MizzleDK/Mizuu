package com.miz.mizuu.fragments;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.Toast;

import com.miz.db.DbAdapterSources;
import com.miz.functions.FileSource;
import com.miz.functions.MizLib;
import com.miz.functions.NetworkFile;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class NetworkBrowserFragment extends Fragment {

	private ArrayList<String> currentPathContent = new ArrayList<String>(), parentPathContent = new ArrayList<String>();

	private String user, pass, domain, server, parentPath = "", parentParentPath = "", currentPathName = "", visiblePath = "";
	private boolean isMovie, isLoading;

	private ListView parent, current;
	private LinearLayout content;
	private ProgressBar pbar;
	private Button addSource;

	private CurrentFolderAdapter currentPathAdapter;
	private ParentAdapter parentPathAdapter;

	public NetworkBrowserFragment() {}

	public static NetworkBrowserFragment newInstance(String server, String user, String pass, String domain, boolean isMovie) {
		NetworkBrowserFragment frag = new NetworkBrowserFragment();
		Bundle b = new Bundle();
		b.putString("server", server);
		b.putString("user", user);
		b.putString("pass", pass);
		b.putString("domain", domain);
		b.putBoolean("isMovie", isMovie);
		frag.setArguments(b);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		isMovie = getArguments().getBoolean("isMovie");

		domain = getArguments().getString("domain");
		user = getArguments().getString("user");
		pass = getArguments().getString("pass");
		server = getArguments().getString("server").replace("smb://", "");

		currentPathAdapter = new CurrentFolderAdapter();
		parentPathAdapter = new ParentAdapter();

		if (isAdded())
			getActivity().setTitle(getString(R.string.addFileSourceTitle));
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
					dbHelper.createSource("smb://" + visiblePath.replace("smb://", ""), DbAdapterSources.KEY_TYPE_MOVIE, FileSource.SMB, user, pass, domain);
				} else {
					dbHelper.createSource("smb://" + visiblePath.replace("smb://", ""), DbAdapterSources.KEY_TYPE_SHOW, FileSource.SMB, user, pass, domain);
				}
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-filesource-change"));
				getActivity().finish();
				return;
			}
		});
		parent = (ListView) v.findViewById(R.id.parent);
		parent.setAdapter(parentPathAdapter);
		parent.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 == 0) { // Go up
					if (!parentParentPath.equals("smb://"))
						browse(parentParentPath);
					else
						Toast.makeText(getActivity(), getString(R.string.noParentFolder), Toast.LENGTH_SHORT).show();
				} else { // Go to the selected path
					browse(parentPathContent.get(arg2 - 1));
				}
			}
		});
		current = (ListView) v.findViewById(R.id.current);
		current.setAdapter(currentPathAdapter);
		current.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 == 0) { // Go up
					if (!parentPath.isEmpty())
						browse(parentPath);
					else
						Toast.makeText(getActivity(), getString(R.string.noParentFolder), Toast.LENGTH_SHORT).show();
				} else { // Go to the selected path
					browse(currentPathContent.get(arg2 - 1));
				}
			}
		});

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		try {
			browse(MizLib.URLEncodeUTF8(MizLib.createSmbLoginString(URLEncoder.encode(domain, "utf-8"), URLEncoder.encode(user, "utf-8"), URLEncoder.encode(pass, "utf-8"), server, true)));
		} catch (UnsupportedEncodingException e) {}
	}


	private void browse(final String path) {
		if (!isLoading && path.endsWith("/")) {
			new Thread() {
				@Override
				public void run() {
					try {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setLoading(true);
							}	
						});

						// Get the user visible path
						visiblePath = path.substring(path.indexOf("@") + 1);

						// Get content of the currently selected path
						SmbFile currentPath = new SmbFile(path);

						currentPathName = currentPath.getName();

						SmbFile[] list = currentPath.listFiles();
						currentPathContent.clear();
						final ArrayList<NetworkFile> currentList = new ArrayList<NetworkFile>();
						for (int i = 0; i < list.length; i++) {
							currentList.add(new NetworkFile(list[i]));
							currentPathContent.add(list[i].getCanonicalPath());
						}

						if (isAdded())
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									currentPathAdapter.setFiles(currentList);
									currentPathAdapter.notifyDataSetChanged();
								}	
							});

						if (!currentPath.getParent().equals("smb://")) {
							// Set parent path
							parentPath = currentPath.getParent();

							parentParentPath = new SmbFile(parentPath).getParent();

							// Get content of the parent path
							SmbFile[] pList = new SmbFile(parentPath).listFiles();
							parentPathContent.clear();
							final ArrayList<NetworkFile> parentList = new ArrayList<NetworkFile>();
							for (int i = 0; i < pList.length; i++) {
								if (pList[i].isDirectory()) {
									parentList.add(new NetworkFile(pList[i]));
									parentPathContent.add(pList[i].getCanonicalPath());
								}
							}

							if (isAdded())
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										parentPathAdapter.setFiles(parentList);
										parentPathAdapter.notifyDataSetChanged();
									}	
								});
						} else {
							if (isAdded())
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										parentPathAdapter.setFiles(new ArrayList<NetworkFile>());
										parentPathAdapter.notifyDataSetChanged();
									}	
								});
						}

						if (isAdded())
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									getActivity().getActionBar().setSubtitle(visiblePath);

									setLoading(false);
								}	
							});
					}
					catch (MalformedURLException e) {}
					catch (final SmbException e) {
						if (isAdded()) {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
								}}

									);
						}
					}
				}
			}.start();
		}
	}

	static class ViewHolder {
		TextView name, size;
		ImageView icon;
	}

	private class CurrentFolderAdapter extends BaseAdapter {

		private List<NetworkFile> files = new ArrayList<NetworkFile>();
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public void setFiles(List<NetworkFile> l) {
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

	private class ParentAdapter extends BaseAdapter {

		private List<NetworkFile> files = new ArrayList<NetworkFile>();
		private LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		public void setFiles(List<NetworkFile> l) {
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

				if (currentPathName.equals(files.get(position - 1).getName() + "/"))
					convertView.setBackgroundResource(R.drawable.bg);
				else
					convertView.setBackgroundColor(Color.TRANSPARENT);
			}
			holder.size.setVisibility(View.GONE);

			return convertView;
		}
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
}
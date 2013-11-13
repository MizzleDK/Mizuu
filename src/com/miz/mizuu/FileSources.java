package com.miz.mizuu;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterSources;

import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.miz.functions.FileSource;

public class FileSources extends MizActivity {

	private ArrayList<FileSource> sources = new ArrayList<FileSource>();
	private ListView list;
	private LinearLayout noFileSources;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.filesources_layout);

		noFileSources = (LinearLayout) findViewById(R.id.noFileSources);
		list = (ListView) findViewById(R.id.listView1);
		list.setAdapter(new ListAdapter());

		loadSources();

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-filesource-change"));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			loadSources();
		}
	};

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void loadSources() {
		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();

		sources.clear();

		// Fetch all movie sources and add them to the array
		Cursor cursor = dbHelper.fetchAllSources();
		while (cursor.moveToNext()) {
			sources.add(new FileSource(
					cursor.getLong(cursor.getColumnIndex(DbAdapterSources.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
					cursor.getInt(cursor.getColumnIndex(DbAdapterSources.KEY_IS_SMB)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_USER)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_TYPE))
					));
		}

		cursor.close();

		if (sources.size() == 0)
			noFileSources.setVisibility(View.VISIBLE);
		else
			noFileSources.setVisibility(View.GONE);

		notifyDataSetChanged();
	}

	public void addLocalSource(MenuItem m) {
		addSource(true);
	}

	public void addNetworkSource(MenuItem m) {
		addSource(false);
	}

	private void addSource(final boolean isLocalSource) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.addFileSource)
		.setItems(R.array.filesource_picker, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (isLocalSource) {
					if (which == 0)
						addLocalMovie();
					else
						addLocalShow();
				} else {
					if (which == 0)
						addNetworkMovie();
					else
						addNetworkShow();
				}
			}
		});
		builder.show();
	}

	public void addLocalMovie() {
		Intent intent = new Intent();
		intent.setClass(this, AddLocalFileSource.class);
		intent.putExtra("type", "movie");
		startActivityForResult(intent, 0); // 0 = movie
	}

	public void addNetworkMovie() {
		Intent intent = new Intent();
		intent.setClass(this, AddNetworkFilesourceDialog.class);
		intent.putExtra("type", "movie");
		startActivity(intent);
	}

	public void addLocalShow() {
		Intent intent = new Intent();
		intent.setClass(this, AddLocalFileSource.class);
		intent.putExtra("type", "tvshow");
		startActivityForResult(intent, 1); // 1 = show
	}

	public void addNetworkShow() {
		Intent intent = new Intent();
		intent.setClass(this, AddNetworkFilesourceDialog.class);
		intent.putExtra("type", "tvshow");
		startActivity(intent);
	}

	public void removeSelectedSource(final int id) {
		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();
		dbHelper.deleteSource(sources.get(id).getRowId());

		loadSources();
	}

	@Override
	public void onBackPressed() {
		finish();
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.filebrowser, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		loadSources();
	}

	private void notifyDataSetChanged() {
		try {
			list.setAdapter(new ListAdapter());
		} catch (Exception e) {}
	}

	static class ViewHolder {
		TextView folderName, fullPath;
		ImageView remove, icon;
	}

	public class ListAdapter extends BaseAdapter {
		private LayoutInflater inflater;

		public ListAdapter() {
			inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public int getCount() {
			return sources.size();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.filesource_list, null);

				holder = new ViewHolder();
				holder.folderName = (TextView) convertView.findViewById(R.id.txtListTitle);
				holder.folderName.setTextAppearance(FileSources.this, android.R.style.TextAppearance_Medium);
				holder.fullPath = (TextView) convertView.findViewById(R.id.txtListPlot);
				holder.remove = (ImageView) convertView.findViewById(R.id.imageView2);
				holder.icon = (ImageView) convertView.findViewById(R.id.traktIcon);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.folderName.setText(sources.get(position).getTitle());
			holder.fullPath.setText(sources.get(position).getFilepath());

			if (sources.get(position).isMovie())
				holder.icon.setImageResource(R.drawable.ic_action_movie);
			else
				holder.icon.setImageResource(R.drawable.ic_action_tv);

			holder.remove.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					removeSelectedSource(position);
				}
			});

			return convertView;
		}
	}
}
package com.miz.mizuu;

import java.util.ArrayList;
import java.util.Locale;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.functions.AsyncTask;
import com.miz.functions.DecryptedShowEpisode;
import com.miz.functions.MizLib;
import com.miz.functions.TheTVDb;
import com.miz.functions.Tvshow;
import com.miz.service.TheTVDB;
import com.miz.widgets.ShowBackdropWidgetProvider;
import com.miz.widgets.ShowCoverWidgetProvider;
import com.miz.widgets.ShowStackWidgetProvider;
import com.squareup.picasso.Picasso;

public class IdentifyTvShow extends MizActivity {

	private String[] files;
	private String oldShowId;
	private ArrayList<Result> results = new ArrayList<Result>();
	private ListView lv;
	private EditText searchText, seasonText, episodeText;
	private ProgressBar pbar;
	private StartSearch startSearch;
	private long rowId;
	private boolean localizedInfo, isShow, includeShowData;
	private ListAdapter mAdapter;
	private SharedPreferences settings;
	private CheckBox useSystemLanguage;
	private Locale locale;
	private Picasso mPicasso;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		setContentView(R.layout.identify_episode_layout);

		// Initialize the PreferenceManager variable and preference variable(s)
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		localizedInfo = settings.getBoolean("prefsUseLocalData", false);

		isShow = getIntent().getExtras().getBoolean("isShow");
		includeShowData = getIntent().getExtras().getBoolean("includeShowData");
		rowId = Long.valueOf(getIntent().getExtras().getString("rowId"));
		files = getIntent().getExtras().getStringArray("files");

		if (getIntent().getExtras().getString("showName") != null) {
			oldShowId = getIntent().getExtras().getString("oldShowId");
		}

		mAdapter = new ListAdapter(this);
		
		mPicasso = MizuuApplication.getPicasso(this);

		locale = Locale.getDefault();

		pbar = (ProgressBar) findViewById(R.id.pbar);

		useSystemLanguage = (CheckBox) findViewById(R.id.searchLanguage);
		useSystemLanguage.setText(getString(R.string.searchIn) + " " + locale.getDisplayLanguage(Locale.ENGLISH));
		if (localizedInfo)
			useSystemLanguage.setChecked(true);

		lv = (ListView) findViewById(android.R.id.list);
		lv.setAdapter(mAdapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				update(arg2);
			}
		});

		DecryptedShowEpisode result = MizLib.decryptEpisode(files[0], settings.getString("ignoredTags", ""));

		seasonText = (EditText) findViewById(R.id.seasonText);
		seasonText.setText(String.valueOf(result.getSeason()));
		episodeText = (EditText) findViewById(R.id.episodeText);
		episodeText.setText(String.valueOf(result.getEpisode()));

		if (isShow || files.length > 1) {
			seasonText.setVisibility(View.GONE);
			episodeText.setVisibility(View.GONE);
			setTitle(getString(R.string.identifyShow));
		}

		searchText = (EditText) findViewById(R.id.search);
		searchText.setText(result.getDecryptedFileName());
		searchText.setSelection(searchText.length());
		searchText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_SEARCH)
					searchForShows();
				return true;
			}
		});

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-tvshows-identification"));

		startSearch = new StartSearch();

		if (MizLib.isOnline(this)) {
			startSearch.execute(searchText.getText().toString());
		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
		}
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("tvshow-episode-changed"));
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("mizuu-shows-update"));

			updateWidgets();

			finish();
			return;
		}
	};

	private void updateWidgets() {
		AppWidgetManager awm = AppWidgetManager.getInstance(this);
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowStackWidgetProvider.class)), R.id.stack_view); // Update stack view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowCoverWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
		awm.notifyAppWidgetViewDataChanged(awm.getAppWidgetIds(new ComponentName(this, ShowBackdropWidgetProvider.class)), R.id.widget_grid); // Update grid view widget
	}

	public void searchForShows(View v) {
		searchForShows();
	}

	private void searchForShows() {
		results.clear();
		if (MizLib.isOnline(this)) {
			if (!searchText.getText().toString().isEmpty()) {
				startSearch.cancel(true);
				startSearch = new StartSearch();
				startSearch.execute(searchText.getText().toString());
			} else mAdapter.notifyDataSetChanged();
		} else Toast.makeText(getApplicationContext(), getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
	}

	protected class StartSearch extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
			showProgressBar();
		}

		@Override
		protected String doInBackground(String... params) {
			try {

				TheTVDb instance = new TheTVDb(getApplicationContext());

				ArrayList<Tvshow> shows;
				if (useSystemLanguage.isChecked())
					shows = instance.searchForShows(params[0], getLocaleShortcode());
				else
					shows = instance.searchForShows(params[0], "en");

				int count = shows.size();
				for (int i = 0; i < count; i++)
					results.add(new Result(shows.get(i).getTitle(), shows.get(i).getId(), shows.get(i).getCover_url(), shows.get(i).getDescription(), shows.get(i).getFirst_aired()));

			} catch (Exception e) {}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			hideProgressBar();
			if (searchText.getText().toString().length() > 0) {
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	private String getLocaleShortcode() {
		String language = locale.toString();
		if (language.contains("_"))
			language = language.substring(0, language.indexOf("_"));
		return language;
	}

	private void update(int id) {
		if (MizLib.isOnline(this)) {
			if (isShow)
				Toast.makeText(this, getString(R.string.updatingShowInfo), Toast.LENGTH_LONG).show();
			else
				Toast.makeText(this, getString(R.string.updatingEpisodeInfo), Toast.LENGTH_LONG).show();

			Intent tvdbIntent = new Intent(this, TheTVDB.class);
			Bundle b = new Bundle();
			b.putStringArray("files", files);
			String[] arr = getIntent().getExtras().getStringArray("rowIds");
			if (arr != null)
				b.putStringArray("rowsToDrop", arr);
			b.putString("tvdbId", results.get(id).getId());
			b.putBoolean("isEpisodeIdentify", !isShow);
			b.putBoolean("isShowIdentify", isShow);

			if (isShow) {
				b.putString("oldShowId", oldShowId);
			} else {
				b.putLong("rowId", rowId);
				b.putString("season", MizLib.addIndexZero(seasonText.getText().toString()));
				b.putString("episode", MizLib.addIndexZero(episodeText.getText().toString()));
				if (includeShowData)
					b.putBoolean("isUnidentifiedIdentify", true);
			}

			if (useSystemLanguage.isChecked())
				b.putString("language", getLocaleShortcode());

			tvdbIntent.putExtras(b);
			startService(tvdbIntent);
		} else
			Toast.makeText(this, getString(R.string.noInternet), Toast.LENGTH_SHORT).show();
	}

	static class ViewHolder {
		TextView title, description, descriptionTitle, release, releasedate;
		ImageView cover;
		LinearLayout layout;
	}

	public class ListAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private final Context mContext;
		private int mItemHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;

		public ListAdapter(Context context) {
			super();
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mImageViewLayoutParams = new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}

		public int getCount() {
			return results.size();
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item_movie, parent, false);

				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.text);
				holder.description = (TextView) convertView.findViewById(R.id.origTitle);
				holder.descriptionTitle = (TextView) convertView.findViewById(R.id.overviewMessage);
				holder.descriptionTitle.setText(getString(R.string.overview));
				holder.release = (TextView) convertView.findViewById(R.id.releasedate);
				holder.releasedate = (TextView) convertView.findViewById(R.id.TextView01);
				holder.releasedate.setText(getString(R.string.detailsFirstAired));
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.layout = (LinearLayout) convertView.findViewById(R.id.cover_layout);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			// Check the height matches our calculated column width
			if (holder.layout.getLayoutParams().height != mItemHeight) {
				holder.layout.setLayoutParams(mImageViewLayoutParams);
			}

			holder.title.setText(results.get(position).getName());
			holder.description.setText(results.get(position).getOriginalTitle());
			holder.release.setText(results.get(position).getRelease());

			if (!results.get(position).getPic().contains("null"))
			mPicasso.load(results.get(position).getPic()).placeholder(R.drawable.gray).error(R.drawable.loading_image).into(holder.cover);
			else
				holder.cover.setImageResource(R.drawable.loading_image);

			return convertView;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.notifyDataSetChanged();
	}

	public class Result {
		String name, id, pic, originaltitle, release;

		public Result(String name, String id, String pic, String originalTitle, String release) {
			this.name = name;
			this.id = id;
			this.pic = pic;
			this.originaltitle = originalTitle;
			this.release = release;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		public String getPic() {
			return pic;
		}

		public String getOriginalTitle() {
			return originaltitle;
		}

		public String getRelease() {
			if (release.equals("null"))
				return getString(R.string.unknownYear);
			return release;
		}
	}

	private void showProgressBar() {
		lv.setVisibility(View.GONE);
		pbar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		lv.setVisibility(View.VISIBLE);
		pbar.setVisibility(View.GONE);
	}
}
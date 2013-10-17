package com.miz.mizuu.fragments;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.functions.DeleteFile;
import com.miz.functions.ImageLoad;
import com.miz.functions.MizLib;
import com.miz.mizuu.DbAdapter;
import com.miz.mizuu.DbAdapterTvShow;
import com.miz.mizuu.DbAdapterTvShowEpisode;
import com.miz.mizuu.IdentifyTvShow;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.TvShowEpisode;
import com.miz.mizuu.R;
import com.test.smbstreamer.variant1.Streamer;

public class ShowEpisodeDetailsFragment extends Fragment {

	private DbAdapterTvShowEpisode dbHelper;
	private ImageView cover, playbutton;
	private TextView title, plot, airdate, rating, director, writer, gueststars, file;
	private String rowId;
	private TvShowEpisode thisEpisode = null;
	private boolean useWildcard, prefsDisableEthernetWiFiCheck, ignoreDeletedFiles;
	private Typeface tf;
	private long videoPlaybackStarted, videoPlaybackEnded;

	public static ShowEpisodeDetailsFragment newInstance(String rowId) {
		ShowEpisodeDetailsFragment frag = new ShowEpisodeDetailsFragment();
		Bundle b = new Bundle();
		b.putString("rowId", rowId);
		frag.setArguments(b);
		return frag;
	}

	public ShowEpisodeDetailsFragment() {}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		useWildcard = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsIgnoreFileType", false);
		prefsDisableEthernetWiFiCheck = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsDisableEthernetWiFiCheck", false);
		ignoreDeletedFiles = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsIgnoredFilesEnabled", false);

		tf = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Thin.ttf");

		rowId = getArguments().getString("rowId");

		if (!rowId.isEmpty()) {
			dbHelper = MizuuApplication.getTvEpisodeDbAdapter();
			Cursor cursor = dbHelper.getEpisode(rowId);
			while (cursor.moveToNext()) {
				thisEpisode = new TvShowEpisode(getActivity(),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_ROWID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SHOW_ID)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_FILEPATH)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_TITLE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_PLOT)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_SEASON)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_AIRDATE)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_DIRECTOR)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_WRITER)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_GUESTSTARS)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EPISODE_RATING)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_HAS_WATCHED)),
						cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisode.KEY_EXTRA_1))
						);
			}
			cursor.close();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (thisEpisode == null)
			return inflater.inflate(R.layout.empty_layout, null, false);
		return inflater.inflate(R.layout.episode_details, null, false);
	}

	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		
		if (thisEpisode != null) {
			cover = (ImageView) v.findViewById(R.id.traktIcon);
			playbutton = (ImageView) v.findViewById(R.id.imageView2);
			title = (TextView) v.findViewById(R.id.overviewMessage);
			plot = (TextView) v.findViewById(R.id.textView2);
			airdate = (TextView) v.findViewById(R.id.textView9);
			rating = (TextView) v.findViewById(R.id.textView11);
			director = (TextView) v.findViewById(R.id.textView7);
			writer = (TextView) v.findViewById(R.id.textView3);
			gueststars = (TextView) v.findViewById(R.id.TextView04);
			file = (TextView) v.findViewById(R.id.TextView07);

			// Set the episode details
			title.setText(thisEpisode.getTitle());
			title.setTypeface(tf);
			if (MizLib.isGoogleTV(getActivity()))
				title.setTextSize(28f);
			title.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			if (!thisEpisode.getDescription().equals(getString(R.string.stringNA)) && !MizLib.isEmpty(thisEpisode.getDescription()))
				plot.setText(thisEpisode.getDescription());
			else
				plot.setText(getString(R.string.stringNoPlot));

			if (MizLib.isEmpty(thisEpisode.getReleasedate()) || thisEpisode.getReleasedate().equals(getString(R.string.stringNA))) {
				v.findViewById(R.id.tableRow1).setVisibility(View.GONE);
			} else {
				// Set the genre first aired date
				try {
					String[] date = thisEpisode.getReleasedate().split("-");
					Calendar cal = Calendar.getInstance();
					cal.set(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));

					airdate.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(cal.getTime()));
				} catch (Exception e) { // Fall back if something goes wrong
					airdate.setText(thisEpisode.getReleasedate());
				}
			}

			if (!thisEpisode.getRating().equals("0/10"))
				rating.setText(thisEpisode.getRating());
			else
				v.findViewById(R.id.tableRow2).setVisibility(View.GONE);

			if (MizLib.isEmpty(thisEpisode.getDirector()) || thisEpisode.getDirector().equals(getString(R.string.stringNA))) {
				v.findViewById(R.id.tableRow3).setVisibility(View.GONE);
			} else {		
				director.setText(thisEpisode.getDirector());
			}

			if (MizLib.isEmpty(thisEpisode.getWriter()) || thisEpisode.getWriter().equals(getString(R.string.stringNA))) {
				v.findViewById(R.id.tableRow4).setVisibility(View.GONE);
			} else {		
				writer.setText(thisEpisode.getWriter());
			}

			if (MizLib.isEmpty(thisEpisode.getGuestStars()) || thisEpisode.getGuestStars().equals(getString(R.string.stringNA))) {
				v.findViewById(R.id.tableRow5).setVisibility(View.GONE);
			} else {		
				gueststars.setText(thisEpisode.getGuestStars());
			}

			file.setText(thisEpisode.getFilepath());

			playbutton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					videoPlaybackStarted = System.currentTimeMillis();
					if (thisEpisode.isNetworkFile()) {
						playNetworkFile();
					} else {
						try { // Attempt to launch intent based on the MIME type
							startActivity(MizLib.getVideoIntent(thisEpisode.getFilepath(), useWildcard, thisEpisode));
							checkIn();
						} catch (Exception e) {
							try { // Attempt to launch intent based on wildcard MIME type
								startActivity(MizLib.getVideoIntent(thisEpisode.getFilepath(), "video/*", thisEpisode));
								checkIn();
							} catch (Exception e2) {
								Toast.makeText(getActivity(), getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
							}
						}
					}
				}
			});

			if (new File(thisEpisode.getEpisodePhoto()).exists()) {
				ImageLoad loader = new ImageLoad();
				loader.setFileUrl(thisEpisode.getEpisodePhoto());
				loader.setImageView(cover);
				loader.setDuration(250);
				loader.execute();
			} else {
				if (new File(MizLib.getTvShowThumbFolder(getActivity()) + "/" + thisEpisode.getShowId() + ".jpg").exists()) {
					ImageLoad loader = new ImageLoad();
					loader.setFileUrl(MizLib.getTvShowBackdropFolder(getActivity()) + "/" + thisEpisode.getShowId() + "_tvbg.jpg");
					loader.setImageView(cover);
					loader.setDuration(250);
					loader.execute();
				} else {
					cover.setImageResource(R.drawable.nobackdrop);
				}
			}
		}
	}

	public void onResume() {
		super.onResume();

		videoPlaybackEnded = System.currentTimeMillis();

		if (videoPlaybackStarted > 0 && videoPlaybackEnded - videoPlaybackStarted > (1000 * 60 * 5)) {
			if (!thisEpisode.hasWatched())
				watched(false); // Mark it as watched
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (!(!MizLib.runsInPortraitMode(getActivity()) && MizLib.runsOnTablet(getActivity())) && isVisible())
			inflater.inflate(R.menu.episode_details, menu);

		try {
			if (thisEpisode.hasWatched()) {
				menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsUnwatched);
			} else {
				menu.findItem(R.id.watched).setTitle(R.string.stringMarkAsWatched);
			}
		} catch (Exception e) {}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuDeleteEpisode:
			deleteEpisode();
			break;
		case R.id.watched:
			watched(true);
			break;
		case R.id.identify:
			identifyEpisode();
		}
		return false;
	}

	private void identifyEpisode() {
		Intent i = new Intent();
		i.setClass(getActivity(), IdentifyTvShow.class);
		i.putExtra("rowId", thisEpisode.getRowId());
		i.putExtra("files", new String[]{thisEpisode.getFullFilepath()});
		i.putExtra("isShow", false);
		startActivity(i);
	}

	private void deleteEpisode() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		View dialogLayout = getActivity().getLayoutInflater().inflate(R.layout.delete_file_dialog_layout, null);
		final CheckBox cb = (CheckBox) dialogLayout.findViewById(R.id.deleteFile);

		builder.setTitle(getString(R.string.removeEpisode) + " S" + thisEpisode.getSeason() + "E" + thisEpisode.getEpisode())
		.setView(dialogLayout)
		.setCancelable(false)
		.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {	
				// Create and open database
				dbHelper = MizuuApplication.getTvEpisodeDbAdapter();
				boolean deleted = false;
				if (ignoreDeletedFiles)
					deleted = dbHelper.ignoreEpisode(thisEpisode.getRowId());
				else
					deleted = dbHelper.deleteEpisode(thisEpisode.getRowId());

				if (deleted) {
					try {
						// Delete episode images
						File episodePhoto = new File(MizLib.getTvShowEpisodeFolder(getActivity()), thisEpisode.getShowId() + "_S" + thisEpisode.getSeason() + "E" + thisEpisode.getEpisode() + ".jpg");
						if (episodePhoto.exists()) {
							MizLib.deleteFile(episodePhoto);
						}
					} catch (NullPointerException e) {} // No file to delete

					if (dbHelper.getEpisodeCount(thisEpisode.getShowId()) == 0) { // No more episodes for this show
						DbAdapterTvShow dbShow = MizuuApplication.getTvDbAdapter();
						boolean deletedShow = dbShow.deleteShow(thisEpisode.getShowId());

						if (deletedShow) {
							MizLib.deleteFile(new File(MizLib.getTvShowThumbFolder(getActivity()), thisEpisode.getShowId() + ".jpg"));
							MizLib.deleteFile(new File(MizLib.getTvShowBackdropFolder(getActivity()), thisEpisode.getShowId() + "_tvbg.jpg"));
						}
					}

					if (cb.isChecked()) {
						Intent deleteIntent = new Intent(getActivity(), DeleteFile.class);
						deleteIntent.putExtra("filepath", thisEpisode.getFilepath());
						getActivity().startService(deleteIntent);
					}

					notifyDatasetChanges();
					getActivity().finish();
				} else {
					Toast.makeText(getActivity(), getString(R.string.failedToRemoveEpisode), Toast.LENGTH_SHORT).show();
				}
			}
		})
		.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		})
		.show();
	}

	private void watched(boolean showToast) {
		// Create and open database
		dbHelper = MizuuApplication.getTvEpisodeDbAdapter();

		thisEpisode.setHasWatched(!thisEpisode.hasWatched()); // Reverse the hasWatched boolean

		if (dbHelper.updateSingleItem(Long.valueOf(thisEpisode.getRowId()), DbAdapter.KEY_HAS_WATCHED, thisEpisode.getHasWatched())) {
			getActivity().invalidateOptionsMenu();

			if (showToast)
				if (thisEpisode.hasWatched()) {
					Toast.makeText(getActivity(), getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getActivity(), getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
				}

			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("tvshow-episode-changed"));

		} else {
			if (showToast)
				Toast.makeText(getActivity(), getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
		}

		new Thread() {
			@Override
			public void run() {
				ArrayList<TvShowEpisode> episode = new ArrayList<TvShowEpisode>();
				episode.add(thisEpisode);
				MizLib.markEpisodeAsWatched(episode, getActivity());
			}
		}.start();
	}

	private void notifyDatasetChanges() {
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-shows-update"));
	}

	private void checkIn() {
		new Thread() {
			@Override
			public void run() {
				MizLib.checkInEpisodeTrakt(thisEpisode, getActivity());
			}
		}.start();
	}

	private void playNetworkFile() {
		if (!MizLib.isWifiConnected(getActivity(), prefsDisableEthernetWiFiCheck)) {
			Toast.makeText(getActivity(), getString(R.string.noConnection), Toast.LENGTH_LONG).show();
			return;
		}

		int bufferSize;
		String buff = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("prefsBufferSize", getString(R.string._16kb));
		if (buff.equals(getString(R.string._16kb)))
			bufferSize = 8192 * 2; // This appears to be the limit for most video players
		else bufferSize = 8192;

		final Streamer s = Streamer.getInstance();
		if (s != null)
			s.setBufferSize(bufferSize);
		else {
			Toast.makeText(getActivity(), getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
			return;
		}

		final NtlmPasswordAuthentication auth = MizLib.getAuthFromFilepath(MizLib.TYPE_SHOWS, thisEpisode.getFilepath());

		new Thread(){
			public void run(){
				try{
					final SmbFile file = new SmbFile(
							MizLib.createSmbLoginString(
									URLEncoder.encode(auth.getDomain(), "utf-8"),
									URLEncoder.encode(auth.getUsername(), "utf-8"),
									URLEncoder.encode(auth.getPassword(), "utf-8"),
									thisEpisode.getFilepath(),
									false
									));

					s.setStreamSrc(file, MizLib.getSubtitleFiles(thisEpisode.getFilepath(), auth)); //the second argument can be a list of subtitle files
					getActivity().runOnUiThread(new Runnable(){
						public void run(){
							try{
								Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(thisEpisode.getFilepath()).getPath())).getEncodedPath());	
								startActivity(MizLib.getVideoIntent(uri, useWildcard, thisEpisode));
								checkIn();
							} catch (Exception e) {
								try { // Attempt to launch intent based on wildcard MIME type
									Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(thisEpisode.getFilepath()).getPath())).getEncodedPath());	
									startActivity(MizLib.getVideoIntent(uri, "video/*", thisEpisode));
									checkIn();
								} catch (Exception e2) {
									Toast.makeText(getActivity(), getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
								}
							}
						}
					});
				}
				catch (MalformedURLException e) {}
				catch (UnsupportedEncodingException e1) {}
			}
		}.start();
	}
}
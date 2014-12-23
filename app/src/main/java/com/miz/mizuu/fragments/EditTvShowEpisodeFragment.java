package com.miz.mizuu.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShowEpisode;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TypefaceUtils;

import java.util.Calendar;

@SuppressLint("InflateParams")
public class EditTvShowEpisodeFragment extends Fragment {

    private TvShowEpisode mEpisode;
    private Toolbar mToolbar;
    private EditText mTitle, mDescription, mDirector, mWriter, mGuestStars;
    private Button mRating, mReleaseDate;

    public EditTvShowEpisodeFragment() {} // Empty constructor

    public static EditTvShowEpisodeFragment newInstance(String showId, int season, int episode) {
        EditTvShowEpisodeFragment fragment = new EditTvShowEpisodeFragment();
        Bundle args = new Bundle();
        args.putString("showId", showId);
        args.putInt("season", season);
        args.putInt("episode", episode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        // Load the show details
        loadTvShowEpisode(getArguments().getString("showId"), getArguments().getInt("season"), getArguments().getInt("episode"));

        // Hide the keyboard when the Activity starts
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_episode, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        ((MizActivity) getActivity()).setSupportActionBar(mToolbar);

        // Text fields
        mTitle = (EditText) v.findViewById(R.id.edit_title);
        mDescription = (EditText) v.findViewById(R.id.edit_description);
        mDirector = (EditText) v.findViewById(R.id.edit_director);
        mWriter = (EditText) v.findViewById(R.id.edit_writer);
        mGuestStars = (EditText) v.findViewById(R.id.edit_guest_stars);

        // Buttons
        mRating = (Button) v.findViewById(R.id.edit_rating);
        mReleaseDate = (Button) v.findViewById(R.id.edit_release_date);

        setupValues(true);
    }

    private void setupValues(boolean resetTextFields) {
        if (resetTextFields) {
            // Set title
            mTitle.setText(mEpisode.getTitle());
            mTitle.setTypeface(TypefaceUtils.getRobotoBold(getActivity()));
            mTitle.setSelection(mEpisode.getTitle().length());

            // Set description
            if (!mEpisode.getDescription().equals(getString(R.string.stringNoPlot)))
                mDescription.setText(mEpisode.getDescription());

            // Set director
            mDirector.setText(mEpisode.getDirector());

            // Set writer
            mWriter.setText(mEpisode.getWriter());

            // Set guest stars
            mGuestStars.setText(mEpisode.getGuestStars());
        }

        // Set rating
        if (!mEpisode.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(mEpisode.getRating()) * 10);
                mRating.setText(rating + " %");
            } catch (NumberFormatException e) {
                mRating.setText(mEpisode.getRating());
            }
        } else {
            mRating.setText(R.string.stringNA);
        }
        mRating.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showRatingDialog(mEpisode.getRawRating());
            }
        });

        // Set release date
        mReleaseDate.setText(MizLib.getPrettyDatePrecise(getActivity(), mEpisode.getReleasedate()));
        mReleaseDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(mEpisode.getReleasedate());
            }
        });
    }

    private void loadTvShowEpisode(String showId, int season, int episode) {
        Cursor cursor = MizuuApplication.getTvEpisodeDbAdapter().getEpisode(showId, season, episode);
        if (cursor.moveToFirst()) {
            try {
                mEpisode = new TvShowEpisode(getActivity(),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
                );

                mEpisode.setFilepaths(MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                ));
            } catch (Exception e) {} finally {
                cursor.close();
            }
        }

        if (mEpisode == null) {
            Toast.makeText(getActivity(), R.string.errorSomethingWentWrong, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    private void showRatingDialog(double initialValue) {
        final View numberPickerLayout = getActivity().getLayoutInflater().inflate(R.layout.number_picker_dialog, null, false);
        final NumberPicker numberPicker = (NumberPicker) numberPickerLayout.findViewById(R.id.number_picker);
        numberPicker.setMaxValue(100);
        numberPicker.setMinValue(0);
        numberPicker.setValue((int) (initialValue * 10));
        final TextView numberPickerText = (TextView) numberPickerLayout.findViewById(R.id.number_picker_text);
        numberPickerText.setText("%");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_rating);
        builder.setView(numberPickerLayout);
        builder.setNeutralButton(R.string.set, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Update the rating
                mEpisode.setRating(numberPicker.getValue());

                // Update the UI with the new value
                setupValues(false);

                // Dismiss the dialog
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void showDatePickerDialog(String initialValue) {
        String[] dateArray = initialValue.split("-");
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(dateArray[0]), Integer.parseInt(dateArray[1]) - 1, Integer.parseInt(dateArray[2]));
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Update the date
                mEpisode.setReleaseDate(year, monthOfYear + 1, dayOfMonth);

                // Update the UI with the new value
                setupValues(false);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveChanges() {
        MizuuApplication.getTvEpisodeDbAdapter().editEpisode(mEpisode.getShowId(), MizLib.getInteger(mEpisode.getSeason()), MizLib.getInteger(mEpisode.getEpisode()),
                mTitle.getText().toString(), mDescription.getText().toString(), mDirector.getText().toString(), mWriter.getText().toString(),
                mGuestStars.getText().toString(), mEpisode.getRating(), mEpisode.getReleasedate());

        LocalBroadcastUtils.updateTvShowEpisodesOverview(getActivity());
        LocalBroadcastUtils.updateTvShowEpisodeDetailsView(getActivity());


        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_movie_and_tv_show, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.cancel_editing:
                getActivity().finish();
                return true;
            case R.id.done_editing:
                saveChanges();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
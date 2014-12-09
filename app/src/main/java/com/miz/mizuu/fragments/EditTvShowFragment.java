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
import android.text.TextUtils;
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
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.utils.TypefaceUtils;

import java.util.ArrayList;
import java.util.Calendar;

@SuppressLint("InflateParams")
public class EditTvShowFragment extends Fragment {

    private TvShow mShow;
    private Toolbar mToolbar;
    private EditText mTitle, mDescription, mGenres;
    private Button mRuntime, mRating, mReleaseDate, mCertification;

    public EditTvShowFragment() {} // Empty constructor

    public static EditTvShowFragment newInstance(String showId) {
        EditTvShowFragment fragment = new EditTvShowFragment();
        Bundle args = new Bundle();
        args.putString("showId", showId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        // Load the show details
        loadTvShow(getArguments().getString("showId"));

        // Hide the keyboard when the Activity starts
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_movie, container, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        ((MizActivity) getActivity()).setSupportActionBar(mToolbar);

        // Text fields
        mTitle = (EditText) v.findViewById(R.id.edit_title);
        mDescription = (EditText) v.findViewById(R.id.edit_description);
        mGenres = (EditText) v.findViewById(R.id.edit_genres);

        // Buttons
        mRuntime = (Button) v.findViewById(R.id.edit_runtime);
        mRating = (Button) v.findViewById(R.id.edit_rating);
        mReleaseDate = (Button) v.findViewById(R.id.edit_release_date);
        mCertification = (Button) v.findViewById(R.id.edit_certification);

        // Hide tagline stuff
        v.findViewById(R.id.edit_tagline).setVisibility(View.GONE);
        v.findViewById(R.id.textView2).setVisibility(View.GONE);

        // Change "Release date" to "First aired"
        ((TextView) v.findViewById(R.id.TextView02)).setText(R.string.detailsFirstAired);

        setupValues(true);
    }

    private void setupValues(boolean resetTextFields) {
        if (resetTextFields) {
            // Set title
            mTitle.setText(mShow.getTitle());
            mTitle.setTypeface(TypefaceUtils.getRobotoBold(getActivity()));
            mTitle.setSelection(mShow.getTitle().length());

            // Set description
            if (!mShow.getDescription().equals(getString(R.string.stringNoPlot)))
                mDescription.setText(mShow.getDescription());

            // Set genres
            mGenres.setText(mShow.getGenres());
        }

        // Set runtime
        mRuntime.setText(MizLib.getPrettyRuntime(getActivity(), MizLib.getInteger(mShow.getRuntime())));
        mRuntime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showRuntimeDialog(MizLib.getInteger(mShow.getRuntime()));
            }
        });

        // Set rating
        if (!mShow.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(mShow.getRating()) * 10);
                mRating.setText(rating + " %");
            } catch (NumberFormatException e) {
                mRating.setText(mShow.getRating());
            }
        } else {
            mRating.setText(R.string.stringNA);
        }
        mRating.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showRatingDialog(mShow.getRawRating());
            }
        });

        // Set release date
        mReleaseDate.setText(MizLib.getPrettyDatePrecise(getActivity(), mShow.getFirstAirdate()));
        mReleaseDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(mShow.getFirstAirdate());
            }
        });

        // Set certification
        if (!TextUtils.isEmpty(mShow.getCertification())) {
            mCertification.setText(mShow.getCertification());
        } else {
            mCertification.setText(R.string.stringNA);
        }
        mCertification.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showCertificationDialog();
            }
        });
    }

    private void loadTvShow(String showId) {
        Cursor cursor = MizuuApplication.getTvDbAdapter().getShow(showId);
        if (cursor.moveToFirst()) {
            try {
                mShow = new TvShow(getActivity(),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_PLOT)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RATING)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_GENRES)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ACTORS)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_CERTIFICATION)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RUNTIME)),
                        false,
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FAVOURITE)),
                        MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)))
                );
            } catch (Exception e) {} finally {
                cursor.close();
            }
        }

        if (mShow == null) {
            Toast.makeText(getActivity(), R.string.errorSomethingWentWrong, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    private void showRuntimeDialog(int initialValue) {
        final View numberPickerLayout = getActivity().getLayoutInflater().inflate(R.layout.number_picker_dialog, null, false);
        final NumberPicker numberPicker = (NumberPicker) numberPickerLayout.findViewById(R.id.number_picker);
        numberPicker.setMaxValue(600);
        numberPicker.setMinValue(0);
        numberPicker.setValue(initialValue);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_runtime);
        builder.setView(numberPickerLayout);
        builder.setNeutralButton(R.string.set, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Update the runtime
                mShow.setRuntime(numberPicker.getValue());

                // Update the UI with the new value
                setupValues(false);

                // Dismiss the dialog
                dialog.cancel();
            }
        });
        builder.show();
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
                mShow.setRating(numberPicker.getValue());

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
                mShow.setFirstAiredDate(year, monthOfYear + 1, dayOfMonth);

                // Update the UI with the new value
                setupValues(false);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showCertificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_certification);

        ArrayList<String> temp = MizuuApplication.getTvDbAdapter().getCertifications();
        final CharSequence[] items = new CharSequence[temp.size() + 1];
        items[0] = getString(R.string.create_new_certification);
        for (int i = 0; i < temp.size(); i++) {
            items[i + 1] = temp.get(i);
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Create new certification dialog
                    showNewCertificationDialog();
                } else {
                    // Set certification
                    mShow.setCertification(items[which].toString());

                    // Update the UI with the new value
                    setupValues(false);

                    // Dismiss the dialog
                    dialog.cancel();
                }
            }
        });
        builder.show();
    }

    private void showNewCertificationDialog() {
        final EditText input = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.create_new_certification);
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Set certification
                mShow.setCertification(input.getText().toString());

                // Update the UI with the new value
                setupValues(false);

                // Dismiss the dialog
                dialog.cancel();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveChanges() {
        MizuuApplication.getTvDbAdapter().editShow(mShow.getId(), mTitle.getText().toString(), mDescription.getText().toString(),
                mGenres.getText().toString(), mShow.getRuntime(), mShow.getRating(), mShow.getFirstAirdate(), mShow.getCertification());

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
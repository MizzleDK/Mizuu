package com.miz.mizuu.fragments;

import android.annotation.SuppressLint;
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
import com.miz.db.DbAdapterMovies;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.utils.TypefaceUtils;

import java.util.ArrayList;
import java.util.Calendar;

@SuppressLint("InflateParams")
public class EditMovieFragment extends Fragment {

    private Movie mMovie;
    private Toolbar mToolbar;
    private EditText mTitle, mTagline, mDescription, mGenres;
    private Button mRuntime, mRating, mReleaseDate, mCertification;

    public EditMovieFragment() {} // Empty constructor

    public static EditMovieFragment newInstance(String movieId) {
        EditMovieFragment fragment = new EditMovieFragment();
        Bundle args = new Bundle();
        args.putString("movieId", movieId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        // Load the movie details
        loadMovie(getArguments().getString("movieId"));

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
        mTagline = (EditText) v.findViewById(R.id.edit_tagline);
        mDescription = (EditText) v.findViewById(R.id.edit_description);
        mGenres = (EditText) v.findViewById(R.id.edit_genres);

        // Buttons
        mRuntime = (Button) v.findViewById(R.id.edit_runtime);
        mRating = (Button) v.findViewById(R.id.edit_rating);
        mReleaseDate = (Button) v.findViewById(R.id.edit_release_date);
        mCertification = (Button) v.findViewById(R.id.edit_certification);

        setupValues(true);
    }

    private void setupValues(boolean resetTextFields) {
        if (resetTextFields) {
            // Set title
            mTitle.setText(mMovie.getTitle());
            mTitle.setTypeface(TypefaceUtils.getRobotoBold(getActivity()));
            mTitle.setSelection(mMovie.getTitle().length());

            // Set tagline
            mTagline.setText(mMovie.getTagline());

            // Set description
            if (!mMovie.getPlot().equals(getString(R.string.stringNoPlot)))
                mDescription.setText(mMovie.getPlot());

            // Set genres
            mGenres.setText(mMovie.getGenres());
        }

        // Set runtime
        mRuntime.setText(MizLib.getPrettyRuntime(getActivity(), MizLib.getInteger(mMovie.getRuntime())));
        mRuntime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showRuntimeDialog(MizLib.getInteger(mMovie.getRuntime()));
            }
        });

        // Set rating
        if (!mMovie.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(mMovie.getRating()) * 10);
                mRating.setText(rating + " %");
            } catch (NumberFormatException e) {
                mRating.setText(mMovie.getRating());
            }
        } else {
            mRating.setText(R.string.stringNA);
        }
        mRating.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showRatingDialog(mMovie.getRawRating());
            }
        });

        // Set release date
        mReleaseDate.setText(MizLib.getPrettyDatePrecise(getActivity(), mMovie.getReleasedate()));
        mReleaseDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(mMovie.getReleasedate());
            }
        });

        // Set certification
        if (!TextUtils.isEmpty(mMovie.getCertification())) {
            mCertification.setText(mMovie.getCertification());
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

    private void loadMovie(String movieId) {
        Cursor cursor = MizuuApplication.getMovieAdapter().fetchMovie(movieId);
        try {
            mMovie = new Movie(getActivity(),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TITLE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_PLOT)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TAGLINE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TMDB_ID)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_IMDB_ID)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RATING)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RELEASEDATE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_CERTIFICATION)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RUNTIME)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TRAILER)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_GENRES)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_FAVOURITE)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_ACTORS)),
                    MizuuApplication.getCollectionsAdapter().getCollection(cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID))),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TO_WATCH)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_HAS_WATCHED)),
                    cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_DATE_ADDED)),
                    false
            );
        } catch (Exception e) {} finally {
            cursor.close();
        }

        if (mMovie == null) {
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
                mMovie.setRuntime(numberPicker.getValue());

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
                mMovie.setRating(numberPicker.getValue());

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
                mMovie.setReleaseDate(year, monthOfYear + 1, dayOfMonth);

                // Update the UI with the new value
                setupValues(false);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showCertificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_certification);

        ArrayList<String> temp = MizuuApplication.getMovieAdapter().getCertifications();
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
                    mMovie.setCertification(items[which].toString());

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
                mMovie.setCertification(input.getText().toString());

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
        MizuuApplication.getMovieAdapter().editMovie(mMovie.getTmdbId(), mTitle.getText().toString(), mTagline.getText().toString(), mDescription.getText().toString(),
                mGenres.getText().toString(), mMovie.getRuntime(), mMovie.getRating(), mMovie.getReleasedate(), mMovie.getCertification());

        getActivity().setResult(4);
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
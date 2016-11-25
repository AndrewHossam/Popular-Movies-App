package com.example.andrew.popular_movies_app;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.example.andrew.popular_movies_app.Data.MovieContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;


public class FragmentHolder extends Fragment {

    ImageAdapter imageAdapter;
    GridView movieGrid;
    int page = 1;
    View rootView;
    ArrayList<Movie> movieList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.movie_grid, container, false);


        return rootView;

    }

    @Override
    public void onResume() {
        super.onResume();

        if ("favorite".equals(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getString(R.string.pref_sort_key), getString(R.string.pref_sort_default)))) {
            FetchFavoriteTask fetchFavoriteTask = new FetchFavoriteTask();
            fetchFavoriteTask.execute();
        } else {
            if (isNetworkAvailable()) {
                FetchMovieTask fetchMovieTask = new FetchMovieTask(getContext());
                fetchMovieTask.execute();
            } else {
                Toast.makeText(getContext(), "Error: No internet connection !", Toast.LENGTH_SHORT).show();

            }
        }
        movieGrid = (GridView) rootView.findViewById(R.id.gridView);
        movieGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
        movieGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                ImageAdapter adapter = (ImageAdapter) parent.getAdapter();
                Movie movie = (Movie) adapter.getItem(position);
                if (isNetworkAvailable())
                    ((Callback) getActivity()).onItemSelected(movie);
                else
                    Toast.makeText(getContext(), "Error: No internet connection !", Toast.LENGTH_SHORT).show();

                //startActivity(intent);
                //Toast.makeText(getContext(), "heelloooo" + position, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public interface Callback {
        void onItemSelected(Movie movie);
    }

    public class FetchMovieTask extends AsyncTask<Void, Void, Collection<Movie>> {

        private final String LOG_TAG = FetchMovieTask.class.getSimpleName();
        private final Context mContext;

        public FetchMovieTask(Context context) {
            mContext = context;
        }


        @Override
        protected Collection<Movie> doInBackground(Void... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieJsonStr = null;

            try {

                final String API_SORT_PARAM = "sort_by";
                final String API_KEY_PARAM = "api_key";
                final String API_PAGES_PARAM = "page";


                Uri dataUri = Uri.parse(Movie.DISCOVER_BASE_URL).buildUpon()
                        .appendQueryParameter(API_SORT_PARAM, PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getString(R.string.pref_sort_key), getString(R.string.pref_sort_default)))
                        .appendQueryParameter(API_PAGES_PARAM, String.valueOf(page))
                        .appendQueryParameter(API_KEY_PARAM, BuildConfig.OPEN_MOVIE_DB_API_KEY)
                        .build();

                //Log.d(LOG_TAG, "doInBackground() returned: " + PreferenceManager.getDefaultSharedPreferences(getContext()).getString(getString(R.string.pref_sort_key), getString(R.string.pref_sort_default)).toString());
                //Log.v(LOG_TAG, "Built URI " + dataUri.toString());
                URL dataURL = new URL(dataUri.toString());

                urlConnection = (HttpURLConnection) dataURL.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                movieJsonStr = buffer.toString();

                // Log.v(LOG_TAG, "movie string: " + movieJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
                return null;

            }

            // This will only happen if there was an error getting or parsing the forecast.
        }

        public Collection<Movie> getMovieDataFromJson(String movieJsonStr) throws JSONException {
            JSONObject json = new JSONObject(movieJsonStr);
            JSONArray movies = json.getJSONArray("results");
            ArrayList result = new ArrayList<>();
            for (int i = 0; i < movies.length(); i++) {
                result.add(Movie.fromJson(movies.getJSONObject(i)));
            }
            return result;
        }

        @Override
        protected void onPostExecute(Collection<Movie> movies) {
            super.onPostExecute(movies);
            imageAdapter = new ImageAdapter(getActivity(), movies);
            movieGrid.setAdapter(imageAdapter);

        }


    }

    public class FetchFavoriteTask extends AsyncTask<Void, Void, Collection<Movie>> {

        @Override
        protected Collection<Movie> doInBackground(Void... params) {
            Cursor cursor = getContext().getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    MovieContract.MovieEntry.COLUMNS,   // projection
                    null, // selection
                    null,   // selectionArgs
                    null    // sort order
            );
            //Log.d(cursor.getColumnName(0), "doInBackground() returned: " + Arrays.toString(cursor.getColumnNames()));

            if (cursor.moveToFirst()) {
                do {
                    Movie movie = new Movie(cursor);
                    movieList.add(movie);
                } while (cursor.moveToNext());
                cursor.close();
            }

            return movieList;
        }

        @Override
        protected void onPostExecute(Collection<Movie> movies) {

            imageAdapter = new ImageAdapter(getActivity(), movies);
            movieGrid.setAdapter(imageAdapter);
        }

    }

}

package com.example.andrew.popular_movies_app;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.andrew.popular_movies_app.Data.MovieContract;
import com.squareup.picasso.Picasso;

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

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {
    public static final String TAG = DetailActivityFragment.class.getSimpleName();

    static final String DETAIL_MOVIE = "DETAIL_MOVIE";
    View rootView;
    Movie movie;
    ListView listViewReviews;
    ListView listViewVideo;
    TextView reviews;
    TextView trailers;
    ArrayAdapter<String> mReviewsAdapter;
    ArrayAdapter<String> mVideosAdapter;

    public DetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.movie_details, container, false);

        Bundle arguments = getArguments();
        ScrollView mDetailLayout = (ScrollView) rootView.findViewById(R.id.container);

        if (arguments != null) {
            movie = arguments.getParcelable(DetailActivityFragment.DETAIL_MOVIE);
            FetchReviewsAndVideosTask fetchReviewsAndVideosTask = new FetchReviewsAndVideosTask();
            fetchReviewsAndVideosTask.execute(movie);

            mDetailLayout.setVisibility(View.VISIBLE);


        } else {
            movie = new Movie(0, "", "", "", 0.0, "", "2015", null, null, null, null);
            mDetailLayout.setVisibility(View.INVISIBLE);

        }

        final ImageButton imageButton = (ImageButton) rootView.findViewById(R.id.favorite);

        if (isFavorite(movie)) {
            imageButton.setImageResource(R.drawable.abc_btn_rating_star_on_mtrl_alpha);
        }
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFavorite(movie)) {
                    getActivity().getContentResolver().delete(
                            MovieContract.MovieEntry.CONTENT_URI,
                            MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                            new String[]{String.valueOf(movie.id)}
                    );
                    imageButton.setImageResource(R.drawable.abc_btn_rating_star_off_mtrl_alpha);
                    Toast.makeText(getContext(), movie.title + " is removed from favorites", Toast.LENGTH_SHORT).show();

                } else {
                    {
                        ContentValues moviesValues = new ContentValues();

                        moviesValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movie.id);
                        moviesValues.put(MovieContract.MovieEntry.COLUMN_TITLE, movie.title);
                        moviesValues.put(MovieContract.MovieEntry.COLUMN_POSTER, movie.poster_path);
                        moviesValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, movie.overview);
                        moviesValues.put(MovieContract.MovieEntry.COLUMN_RATING, movie.vote_average);
                        moviesValues.put(MovieContract.MovieEntry.COLUMN_DATE, movie.release_date);

                        getActivity().getContentResolver().insert(
                                MovieContract.MovieEntry.CONTENT_URI,
                                moviesValues
                        );
                        imageButton.setImageResource(R.drawable.abc_btn_rating_star_on_mtrl_alpha);
                        Toast.makeText(getContext(), movie.title + " is added to favorites", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        ((TextView) rootView.findViewById(R.id.title)).setText(movie.title);
        ((TextView) rootView.findViewById(R.id.rating)).setText(movie.vote_average + " / 10");
        ((TextView) rootView.findViewById(R.id.overview)).setText(movie.overview);
        ((TextView) rootView.findViewById(R.id.year)).setText(movie.release_date.substring(0, 4));

        Picasso.with(getContext()).load(Movie.BASE_IMAGE_URL + PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_image_quality_key), getString(R.string.pref_image_quality_default)).toString() + "//" + movie.poster_path).into((ImageView) rootView.findViewById(R.id.poster));


        getActivity().setTitle(movie.title);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        listViewReviews = (ListView) rootView.findViewById(R.id.reviews_listview);
        listViewVideo = (ListView) rootView.findViewById(R.id.trailers_listview);
        reviews = (TextView) rootView.findViewById(R.id.reviews);
        trailers = (TextView) rootView.findViewById(R.id.trailers);

        listViewVideo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                watchVideosOnYoutube(movie.keyVideo.get(position));
            }
        });


    }

    public void watchVideosOnYoutube(String key) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + key));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + key)));
        }


    }

    public boolean isFavorite(Movie movie) {

        Cursor cursor = getActivity().getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                new String[]{MovieContract.MovieEntry.COLUMN_MOVIE_ID},   // projection
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?", // selection
                new String[]{String.valueOf(movie.id)},   // selectionArgs
                null    // sort order
        );
        if (cursor.getCount() != 0) {
            cursor.close();
            return true;
        }

        cursor.close();
        return false;
    }

    public class FetchReviewsAndVideosTask extends AsyncTask<Movie, Void, Movie> {

        final String API_PARAM_KEY = "api_key";
        final String myKey = "f1765a23972129bb19c1bdd7cc50355f";
        private final String LOG_TAG = FetchReviewsAndVideosTask.class.getSimpleName();
        Movie movie;

        @Override
        protected Movie doInBackground(Movie... params) {
            HttpURLConnection urlConnectionReviews = null;
            HttpURLConnection urlConnectionVideos = null;
            BufferedReader readerReviews = null;
            BufferedReader readerVideos = null;
            String responseJsonReviews = null;
            String responseJsonVideos = null;
            movie = params[0];

            try {


                Uri reviewsUri = Uri.parse(Movie.MOVIE_BASE_URL + movie.id + "/" + Movie.REVIEWS).buildUpon()
                        .appendQueryParameter(API_PARAM_KEY, myKey)
                        .build();
                Uri videosUri = Uri.parse(Movie.MOVIE_BASE_URL + movie.id + "/" + Movie.VIDEOS).buildUpon()
                        .appendQueryParameter(API_PARAM_KEY, myKey)
                        .build();

                //  Log.d(LOG_TAG, reviewsUri.toString());
                //  Log.d(LOG_TAG, videosUri.toString());

                URL reviewsUrl = new URL(reviewsUri.toString());
                URL videosUrl = new URL(videosUri.toString());

                urlConnectionReviews = (HttpURLConnection) reviewsUrl.openConnection();
                urlConnectionReviews.setRequestMethod("GET");
                urlConnectionReviews.connect();

                urlConnectionVideos = (HttpURLConnection) videosUrl.openConnection();
                urlConnectionVideos.setRequestMethod("GET");
                urlConnectionVideos.connect();


                InputStream inputStreamReviews = urlConnectionReviews.getInputStream();
                InputStream inputStreamVideos = urlConnectionVideos.getInputStream();
                StringBuffer bufferReviews = new StringBuffer();
                StringBuffer bufferVideos = new StringBuffer();
                if (inputStreamReviews == null && inputStreamVideos == null) {
                    // Nothing to do.
                    return null;
                }
                readerReviews = new BufferedReader(new InputStreamReader(inputStreamReviews));
                readerVideos = new BufferedReader(new InputStreamReader(inputStreamVideos));

                String lineReviews;
                String lineVideos;
                while ((lineReviews = readerReviews.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    bufferReviews.append(lineReviews + "\n");
                }
                while ((lineVideos = readerVideos.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    bufferVideos.append(lineVideos + "\n");
                }

                if (bufferReviews.length() == 0 && bufferVideos.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                responseJsonReviews = bufferReviews.toString();
                responseJsonVideos = bufferVideos.toString();

            } catch (Exception ex) {
                Log.e(LOG_TAG, "Error", ex);
                return null;
            } finally {
                if (urlConnectionReviews != null) {
                    urlConnectionReviews.disconnect();
                }
                if (urlConnectionVideos != null) {
                    urlConnectionVideos.disconnect();
                }
                if (readerReviews != null) {
                    try {
                        readerReviews.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
                if (readerVideos != null) {
                    try {
                        readerVideos.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            try {
                JSONObject json = new JSONObject(responseJsonReviews);
                JSONArray reviews = json.getJSONArray("results");
                ArrayList authorList = new ArrayList<>(reviews.length());
                ArrayList contentList = new ArrayList<>(reviews.length());
                for (int i = 0; i < reviews.length(); i++) {
                    authorList.add(reviews.getJSONObject(i).getString("author"));
                    contentList.add(reviews.getJSONObject(i).getString("content"));
                }
                movie.author = authorList;
                movie.content = contentList;

                json = new JSONObject(responseJsonVideos);
                JSONArray videos = json.getJSONArray("results");
                ArrayList nameList = new ArrayList<>(videos.length());
                ArrayList keyList = new ArrayList<>(videos.length());
                for (int i = 0; i < videos.length(); i++) {
                    nameList.add(videos.getJSONObject(i).getString("name"));
                    keyList.add(videos.getJSONObject(i).getString("key"));
                }
                movie.nameVideo = nameList;
                movie.keyVideo = keyList;
                Log.d("reviews", "reviews attacched");

            } catch (JSONException ex) {
                Log.d(LOG_TAG, "Can't parse JSON: " + responseJsonReviews, ex);
            }
            return movie;

        }

        @Override
        protected void onPostExecute(Movie movie) {
            super.onPostExecute(movie);
            if (movie.content != null) {

                if (movie.content.size() != 0) {
                    mReviewsAdapter = new ArrayAdapter<String>(
                            getContext(),
                            R.layout.text_view,
                            R.id.text_view,
                            movie.content
                    );
                    listViewReviews.setAdapter(mReviewsAdapter);
                    reviews.setVisibility(View.VISIBLE);
                } else {
                    reviews.setVisibility(View.INVISIBLE);
                }
            }
            if (movie.keyVideo != null) {
                if (movie.keyVideo.size() != 0) {

                    mVideosAdapter = new ArrayAdapter<String>(
                            getContext(),
                            R.layout.text_view,
                            R.id.text_view,
                            movie.nameVideo
                    );
                    listViewVideo.setAdapter(mVideosAdapter);

                    trailers.setVisibility(View.VISIBLE);

                } else {
                    trailers.setVisibility(View.INVISIBLE);
                }
            }
        }
    }


}



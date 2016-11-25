package com.example.andrew.popular_movies_app;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.example.andrew.popular_movies_app.Data.MovieContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Movie implements Parcelable {
    public static final String BASE_IMAGE_URL = "http://image.tmdb.org/t/p/";
    public static final String DISCOVER_BASE_URL =
            "http://api.themoviedb.org/3/discover/movie";
    public static final String MOVIE_BASE_URL =
            "http://api.themoviedb.org/3/movie/";
    public static final String REVIEWS = "reviews";
    public static final String VIDEOS = "videos";

    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_OVERVIEW = "overview";
    public static final String KEY_POSTER_PATH = "poster_path";
    public static final String KEY_VOTE_AVERAGE = "vote_average";
    public static final String KEY_BACK_DROP = "backdrop_path";
    public static final String KEY_RELEASE_DATE = "release_date";

    public static final String KEY_REVIEWS_AUTHOR = "author";
    public static final String KEY_REVIEWS_CONTENT = "content";
    public static final String KEY_REVIEWS_NAME_VIDEO = "nameVideo";
    public static final String KEY_REVIEWS_KEY_VIDEO = "keyVideo";
    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };
    public final long id;
    public final String title;
    public final String overview;
    public final String poster_path;
    public final double vote_average;
    public final String back_drop;
    public final String release_date;
    public ArrayList<String> author;
    public ArrayList<String> content;
    public ArrayList<String> nameVideo;
    public ArrayList<String> keyVideo;

    public Movie(long id,
                 String title,
                 String overview,
                 String poster_path,
                 double vote_average,
                 String back_drop,
                 String release_date,
                 ArrayList<String> author,
                 ArrayList<String> content,
                 ArrayList<String> nameVideo,
                 ArrayList<String> keyVideo) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.poster_path = poster_path;
        this.vote_average = vote_average;
        this.back_drop = back_drop;
        this.release_date = release_date;
        this.author = author;
        this.content = content;
        this.nameVideo = nameVideo;
        this.keyVideo = keyVideo;
    }

    /*
        public Movie(Bundle bundle) {
            this(
                    bundle.getLong(KEY_ID),
                    bundle.getString(KEY_TITLE),
                    bundle.getString(KEY_OVERVIEW),
                    bundle.getString(KEY_POSTER_PATH),
                    bundle.getDouble(KEY_VOTE_AVERAGE),
                    bundle.getString(KEY_BACK_DROP),
                    bundle.getString(KEY_RELEASE_DATE)
            );
        }
    */
    public Movie(Cursor cursor) {
        this.id = cursor.getLong(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_MOVIE_ID));
        this.title = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_TITLE));
        this.overview = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_OVERVIEW));
        this.poster_path = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_POSTER));
        this.vote_average = cursor.getDouble(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_RATING));
        this.back_drop = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_BACKDROP));
        this.release_date = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_DATE));
    }

    protected Movie(Parcel in) {
        id = in.readLong();
        title = in.readString();
        overview = in.readString();
        poster_path = in.readString();
        vote_average = in.readDouble();
        back_drop = in.readString();
        release_date = in.readString();
        author = in.readArrayList(ClassLoader.getSystemClassLoader());
        content = in.readArrayList(ClassLoader.getSystemClassLoader());
        nameVideo = in.readArrayList(ClassLoader.getSystemClassLoader());
        keyVideo = in.readArrayList(ClassLoader.getSystemClassLoader());


                /*
                this.author = author;
        this.content = content;
        this.nameVideo = nameVideo;
        this.keyVideo = keyVideo;
                 */
    }


    public Movie(Bundle bundle) {
        this(
                bundle.getLong(KEY_ID),
                bundle.getString(KEY_TITLE),
                bundle.getString(KEY_OVERVIEW),
                bundle.getString(KEY_POSTER_PATH),
                bundle.getDouble(KEY_VOTE_AVERAGE),
                bundle.getString(KEY_BACK_DROP),
                bundle.getString(KEY_RELEASE_DATE),
                bundle.getStringArrayList(KEY_REVIEWS_AUTHOR),
                bundle.getStringArrayList(KEY_REVIEWS_CONTENT),
                bundle.getStringArrayList(KEY_REVIEWS_NAME_VIDEO),
                bundle.getStringArrayList(KEY_REVIEWS_KEY_VIDEO)

        );
    }

    public static Movie fromJson(JSONObject jsonObject) throws JSONException {
        return new Movie(
                jsonObject.getLong(KEY_ID),
                jsonObject.getString(KEY_TITLE),
                jsonObject.getString(KEY_OVERVIEW),
                jsonObject.getString(KEY_POSTER_PATH),
                jsonObject.getDouble(KEY_VOTE_AVERAGE),
                jsonObject.getString(KEY_BACK_DROP),
                jsonObject.getString(KEY_RELEASE_DATE),
                null, null, null, null
        );
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putLong(KEY_ID, id);
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_OVERVIEW, overview);
        bundle.putString(KEY_POSTER_PATH, poster_path);
        bundle.putDouble(KEY_VOTE_AVERAGE, vote_average);
        bundle.putString(KEY_BACK_DROP, back_drop);
        bundle.putString(KEY_RELEASE_DATE, release_date);

        bundle.putStringArrayList(KEY_REVIEWS_AUTHOR, author);
        bundle.putStringArrayList(KEY_REVIEWS_CONTENT, content);


        return bundle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(poster_path);
        dest.writeDouble(vote_average);
        dest.writeString(back_drop);
        dest.writeString(release_date);
    }
}
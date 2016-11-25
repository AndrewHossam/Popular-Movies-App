package com.example.andrew.popular_movies_app;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;


public class ImageAdapter extends BaseAdapter {
    Context context;
    ArrayList<Movie> movies = new ArrayList<>();
    private LayoutInflater inflater = null;

    public ImageAdapter(Context context, Collection<Movie> movies) {
        this.context = context;
        this.movies = (ArrayList<Movie>) movies;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return movies.size();
    }

    @Override
    public Object getItem(int position) {
        return movies.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View inflatedView = convertView;

        if (convertView == null) {
            inflatedView = inflater.inflate(R.layout.image_view, null);
        }


        ImageView imageView = (ImageView) inflatedView.findViewById(R.id.grid_item_movieposter_imageview);
        //  Log.d("url", "url: " + movies.get(position));
        Picasso.with(context).load(Movie.BASE_IMAGE_URL + PreferenceManager.getDefaultSharedPreferences(parent.getContext()).getString(parent.getContext().getString(R.string.pref_image_quality_key), parent.getContext().getString(R.string.pref_image_quality_default)).toString() + "//" + movies.get(position).poster_path).into(imageView);
        return inflatedView;
    }
}

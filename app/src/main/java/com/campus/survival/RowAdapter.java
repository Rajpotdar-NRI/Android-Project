package com.campus.survival;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/** One reusable ListView adapter: give it a row layout and a bind function. */
public class RowAdapter<T> extends BaseAdapter {

    public interface Binder<T> {
        void bind(View row, T item, int position);
    }

    private final LayoutInflater inflater;
    private final int layout;
    private final Binder<T> binder;
    private List<T> items = new ArrayList<>();

    public RowAdapter(Context context, int layout, Binder<T> binder) {
        this.inflater = LayoutInflater.from(context);
        this.layout = layout;
        this.binder = binder;
    }

    public void setItems(List<T> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }

    @Override public T getItem(int position) { return items.get(position); }

    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView != null ? convertView : inflater.inflate(layout, parent, false);
        binder.bind(row, items.get(position), position);
        return row;
    }
}

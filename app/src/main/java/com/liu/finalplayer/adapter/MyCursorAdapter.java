package com.liu.finalplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.widget.SimpleCursorAdapter;

import com.liu.finalplayer.R;
import com.liu.finalplayer.database.DbContent;

/**
 * Created by StormGuoson on 2017/1/12.
 */

public class MyCursorAdapter extends SimpleCursorAdapter {
    private Cursor cursor1;

    public MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
        cursor1 = c;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        View view1 = view.findViewById(R.id.layout_cell);
        if (cursor1.getInt(cursor1.getColumnIndex(DbContent.LAST)) == 1)
            view1.setBackgroundColor(Color.rgb(153, 204, 0));
        else view1.setBackgroundColor(Color.WHITE);
//        ImageView imageView = (ImageView) view.findViewById(R.id.ivPic);
//        try {
//            File img = new File(this.cursor1.getString(this.cursor1.getColumnIndex(DbContent.IMG)));
//            imageView.setImageURI(Uri.fromFile(img));
//        } catch (NullPointerException ignored) {
//        }
    }
}

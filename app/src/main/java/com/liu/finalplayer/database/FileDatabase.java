package com.liu.finalplayer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by StormGuoson on 2017/1/9.
 */

public class FileDatabase extends SQLiteOpenHelper {
    public FileDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + DbContent.TABLE_NAME +
                "(_id integer primary key autoincrement,"
                + DbContent.TITLE + " text,"
                + DbContent.SIZE + " text,"
                + DbContent.DataBYTE + " integer,"
                + DbContent.DIR + " text,"
                + DbContent.DataWATCH + " integer,"
                + DbContent.LAST + " integer,"
                + DbContent.WATCH + " text,"
                + DbContent.DATE + " text,"
                + DbContent.FILE + " text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}

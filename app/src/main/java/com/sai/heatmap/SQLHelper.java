package com.sai.heatmap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

public class SQLHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "heat.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "locations";

    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_HEAT = "heat";

    //vars
    private Marker marker;

    String[] allColumns = {COLUMN_TITLE,COLUMN_LATITUDE,COLUMN_LONGITUDE,COLUMN_HEAT};

    SQLiteDatabase db;

    public SQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //CREATE table
        db.execSQL(CREATE_TABLE);
    }

    // Create table SQL query
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_TITLE + " TEXT,"
                    + COLUMN_LATITUDE + " DOUBLE,"
                    + COLUMN_LONGITUDE + " DOUBLE,"
                    + COLUMN_HEAT + " INT,"
                    + "PRIMARY KEY(" + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE + "));";


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //drop older table if exists
        db.execSQL("DROP TABLE IF EXISTS " + SQLHelper.TABLE_NAME);

        //create table again
        onCreate(db);
    }

    public void open() throws SQLException {
        db = this.getWritableDatabase();
    }

    public void close() {
        db.close();
    }

    public Marker addMarker(Marker m) {
        // get writable database as we want to write data
        Log.d("------------:", m.getTitle() + m.getPosition() + m.getTag());
        ContentValues values = new ContentValues();

        values.put(COLUMN_TITLE, m.getTitle());
        values.put(COLUMN_LATITUDE, m.getPosition().latitude);
        values.put(COLUMN_LONGITUDE, m.getPosition().longitude);
        values.put(COLUMN_HEAT, m.getTag().toString());


        // insert row
        db.insert(SQLHelper.TABLE_NAME, null, values);
        return m;
    }

    public Cursor getAllMarkers() {

        return db.query(SQLHelper.TABLE_NAME, allColumns, null, null,
                null, null, null);


    }


    public Marker updateMarker(Marker m) {
        Log.i("db working",db.isOpen() + "");
        Log.d("marker info sq:", m.getTitle() + m.getPosition());
//        String[] searchArgs = {String.valueOf(m.getPosition().latitude), String.valueOf(m.getPosition().longitude)};
//        Cursor c = db.query(TABLE_NAME, allColumns, COLUMN_LATITUDE + " = ?" + " AND "
//                + COLUMN_LONGITUDE + " = ?", searchArgs, null, null, null);

        Cursor c = db.query(TABLE_NAME, allColumns, COLUMN_LATITUDE + " = " + m.getPosition().latitude + " AND "
                + COLUMN_LONGITUDE + " = " + m.getPosition().longitude, null, null, null, null);


//        String[] searchC = {m.getTitle()};
//        Cursor c = db.query(TABLE_NAME, allColumns, Marker.COLUMN_TITLE + " = ?",
//                searchC, null, null, null);

        Log.d("c size", String.valueOf(c.getCount()));

        c.moveToFirst();
        Log.d("c.column title", c.getString(c.getColumnIndex(COLUMN_TITLE)));

        int value = c.getInt(c.getColumnIndex(COLUMN_HEAT)) + 1;
        m.setTag(value);
        Log.d("updateMarker", m.getTitle() + " value=" + m.getTag());
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_HEAT, m.getTag().toString());

        db.update(TABLE_NAME, contentValues, COLUMN_LATITUDE + " = " + m.getPosition().latitude
                + " AND + " + COLUMN_LONGITUDE + " = " + m.getPosition().longitude, null);

        //checking
//        c = db.query(TABLE_NAME, allColumns, Marker.COLUMN_TITLE + " = ?",
//                searchC, null, null, null);
//        c.moveToFirst();
//        Log.d("updated value", String.valueOf(c.getColumnIndex(Marker.COLUMN_VALUE)));

//        db.rawSQL("UPDATE " + SQLHelper.TABLE_NAME+
//                " SET "+ Marker.COLUMN_VALUE+"=+1"+
//                " WHERE "+ Marker.COLUMN_LAT+"="+ m.getLat() + " AND " + Marker.COLUMN_LON + "="
//                + m.getLon() + ");");

        c.close();

        Log.d("marker tag:", m.getTag().toString());
        return m;
    }

    public boolean isExists(Marker m){

        boolean exists = false;

        Cursor c = db.query(TABLE_NAME, allColumns, COLUMN_LATITUDE + " = " + m.getPosition().latitude
                + " AND " + COLUMN_LONGITUDE + " = " + m.getPosition().longitude,
                null, null, null, null);

        if(c.moveToFirst())
            exists = true;
        c.close();
        return exists;
    }

    public void deleteMarker(Marker m) {

        db.delete(TABLE_NAME, COLUMN_LATITUDE + " = '" + m.getPosition().latitude+ " AND "
                + COLUMN_LONGITUDE + " = '" + m.getPosition().longitude + "'", null);
    }

    public int TotalHeat() {
        int sum =0;
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_HEAT + ") FROM " + TABLE_NAME, null);
        if(c.moveToFirst())
        {
            sum = c.getInt(0);
        }
        c.close();
        return sum;
    }

    public int MaxHeat() {
        int max =0;
        Cursor c = db.rawQuery("SELECT MAX(" + COLUMN_HEAT + ") FROM " + TABLE_NAME, null);
        if(c.moveToFirst())
        {
            max = c.getInt(0);
        }
        c.close();
        return max;
    }
}
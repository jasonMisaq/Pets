package com.example.android.pets.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Created by john on 6/4/17.
 */

public class PetDbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "shelter.db";
    public static final int DB_VERSION = 1;

    public PetDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_PETS_TABLE = "CREATE TABLE " + PetEntry.TABLE_NAME
                + "(" + PetEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + PetEntry.COLUMN_PET_NAME + " TEXT,"
                + PetEntry.COLUMN_PET_BREED + " TEXT,"
                + PetEntry.COLUMN_PET_GENDER + " INTEGER NOT NULL,"
                + PetEntry.COLUMN_PET_WEIGHT + " INTEGER NOT NULL DEFAULT 0);";
        db.execSQL(SQL_CREATE_PETS_TABLE);


    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}

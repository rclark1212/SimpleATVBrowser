/*
 * Copyright (C) 2016 Richard Clark
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.production.rclark.simpleatvbrowser.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by rclar on 4/4/2016.
 */
public class FavDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "favorites.db";

    public FavDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        // Create a table to hold favorites.
        final String SQL_CREATE_FAVS_TABLE = "CREATE TABLE " + FavContract.FavoritesEntry.TABLE_NAME + " (" +
                FavContract.FavoritesEntry._ID + " INTEGER PRIMARY KEY, " +
                FavContract.FavoritesEntry.COLUMN_FAVORITES_URL + " TEXT UNIQUE NOT NULL, " +
                FavContract.FavoritesEntry.COLUMN_FAVORITE_TITLE + " TEXT, " +
                FavContract.FavoritesEntry.COLUMN_FAVORITE_HTTP + " TEXT, " +
                FavContract.FavoritesEntry.COLUMN_FAVORITES_THUMB + " BLOB " +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_FAVS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + FavContract.FavoritesEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

}

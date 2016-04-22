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

package com.example.rclark.simpleatvbrowser.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by rclar on 4/4/2016.
 */
public class FavProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private FavDbHelper mOpenHelper;

    static final int FAVS = 100;
    static final int FAVS_WITH_URL = 101;

    //favs db - label_setting = ?
    private static final String sUrlSelection =
            FavContract.FavoritesEntry.TABLE_NAME +
                    "." + FavContract.FavoritesEntry.COLUMN_FAVORITES_URL + " = ? ";

    //get favorite by Url
    private Cursor getFavByUrl(Uri uri, String[] projection, String sortOrder) {
        String url = FavContract.FavoritesEntry.getUrlFromUri(uri);

        String[] selectionArgs;
        String selection;

        selectionArgs = new String[]{url};
        selection = sUrlSelection;

        return mOpenHelper.getReadableDatabase().query(
                FavContract.FavoritesEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    static UriMatcher buildUriMatcher() {

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = FavContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, FavContract.PATH_FAVORITES, FAVS);
        matcher.addURI(authority, FavContract.PATH_FAVORITES + "/*", FAVS_WITH_URL);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new FavDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case FAVS_WITH_URL:
                return FavContract.FavoritesEntry.CONTENT_ITEM_TYPE;
            case FAVS:
                return FavContract.FavoritesEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "favorites/*"
            case FAVS_WITH_URL:
            {
                retCursor = getFavByUrl(uri, projection, sortOrder);
                break;
            }
            // apps
            case FAVS: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        FavContract.FavoritesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case FAVS_WITH_URL:
            case FAVS: {
                long _id = db.insert(FavContract.FavoritesEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = FavContract.FavoritesEntry.buildFavoriteUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case FAVS:
            case FAVS_WITH_URL:
                rowsDeleted = db.delete(
                        FavContract.FavoritesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case FAVS:
                rowsUpdated = db.update(FavContract.FavoritesEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case FAVS_WITH_URL: {
                String url = FavContract.FavoritesEntry.getUrlFromUri(uri);
                String[] parse_selectionArgs = new String[]{url};
                String parse_selection = sUrlSelection;

                rowsUpdated = db.update(FavContract.FavoritesEntry.TABLE_NAME, values, parse_selection,
                        parse_selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}

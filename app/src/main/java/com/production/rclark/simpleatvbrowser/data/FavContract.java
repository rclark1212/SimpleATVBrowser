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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by rclar on 4/4/2016.
 */
public class FavContract {

    public static final String CONTENT_AUTHORITY = "com.production.rclark.simpleatvbrowser";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_FAVORITES = "favorites";

    /* Inner class that defines the table contents of the app table */
    // URI Format
    // /* - all or...
    // /url
    public static final class FavoritesEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITES).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITES;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FAVORITES;

        public static final String TABLE_NAME = "favorites";

        public static final String COLUMN_FAVORITES_URL = "url_label";      //the actual url
        public static final String COLUMN_FAVORITES_THUMB = "url_thumb";
        public static final String COLUMN_FAVORITE_TITLE = "url_title";
        public static final String COLUMN_FAVORITE_HTTP = "url_http";       //the starting (friendly) edit box name of web site

        public static Uri buildFavoriteUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildFavorite(String url) {
            return CONTENT_URI.buildUpon().appendPath(url).build();
        }

        public static String getUrlFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

}

/* 
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package captainfanatic.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for TrollyProvider
 */
public final class Trolly {
	public static final String AUTHORITY = "captainfanatic.provider.Trolly";
	
    /**
     * Trolly table
     */
    public static final class ShoppingList implements BaseColumns {
    	
    	/**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI
                = Uri.parse("content://captainfanatic.provider.Trolly/shoppinglist");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of items.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.captainfanatic.trolly";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single item.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.captainfanatic.trolly";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /**
         * The shopping list item
         * <P>Type: TEXT</P>
         */
        public static final String ITEM = "item";
        
        /**
         * An "off list" item
         * <P>An item that has been added to the list before 
         * but is not on the list at the moment.</P>
         */
        public static final int OFF_LIST = 0;
        
        /**
         * An "on list" item
         * <P>An item that has been added to the list 
         * and is on the list at the moment.</P>
         */
        public static final int ON_LIST = 1;
        
        /**
         * An "in trolley" item
         * <P>An item that has been added to the list and
         * is currently in the trolley - crossed off the list.</P>
         */
        public static final int IN_TROLLEY = 2;

        /**
         * The status of the shopping list item
         * <P>Type: INTEGER</P>
         */
        public static final String STATUS = "status";

        /**
         * The timestamp for when the note was created
         * <P>Type: INTEGER (long)</P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the note was last modified
         * <P>Type: INTEGER (long)</P>
         */
        public static final String MODIFIED_DATE = "modified";
    }
}

/**
	<Trolly is a simple shopping list application for android phones.>
	Copyright (C) 2009  Ben Caldwell
 	
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
   

package caldwell.ben.trolly;

import java.util.ArrayList;

import caldwell.ben.provider.Trolly.ShoppingList;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class Trolly extends ListActivity {
	
//	private static final String TAG = "Trolly";
	
	private static final String KEY_MODE = "mode";
	public static final String KEY_ITEM = "items";
	
	/**
	 * TrollyAdapter allows crossing items off the list and filtering
	 * on user text input.
	 * @author Ben
	 *
	 */
	private class TrollyAdapter extends SimpleCursorAdapter implements Filterable {

		private ContentResolver mContent;   
		
		public TrollyAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			mContent = context.getContentResolver();
		}
		
		@Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (getFilterQueryProvider() != null) {
                return getFilterQueryProvider().runQuery(constraint);
            }

            StringBuilder buffer = null;
            String[] args = null;
            if (constraint != null) {
                buffer = new StringBuilder();
                buffer.append("UPPER(");
                buffer.append(ShoppingList.ITEM);
                buffer.append(") GLOB ?");
                args = new String[] { "*" + constraint.toString().toUpperCase() + "*" };
            }

            return mContent.query(ShoppingList.CONTENT_URI, PROJECTION,
                    buffer == null ? null : buffer.toString(), args,
                    null);
        }

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView item = (TextView)view.findViewById(R.id.item);
			item.setText(cursor.getString(cursor.getColumnIndex(ShoppingList.ITEM)));
			switch(cursor.getInt(cursor.getColumnIndex(ShoppingList.STATUS))){
			case ShoppingList.OFF_LIST:
				item.setPaintFlags(item.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
				item.setTextColor(Color.DKGRAY);
				break;
			case ShoppingList.ON_LIST:
				item.setPaintFlags(item.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
				item.setTextColor(Color.GREEN);
				break;
			case ShoppingList.IN_TROLLEY:
				item.setPaintFlags(item.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				item.setTextColor(Color.GRAY);
				break;
			}
		}
	}
	
	/**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            ShoppingList._ID, // 0
            ShoppingList.ITEM, // 1
            ShoppingList.STATUS, // 2
    };
    
    // Menu item ids
    public static final int MENU_ITEM_DELETE = Menu.FIRST;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 1;
    public static final int MENU_ITEM_CHECKOUT = Menu.FIRST + 2;
    public static final int MENU_ITEM_PREFERENCE = Menu.FIRST + 3;
    public static final int MENU_ITEM_ON_LIST = Menu.FIRST + 4;
    public static final int MENU_ITEM_OFF_LIST = Menu.FIRST + 5;
    public static final int MENU_ITEM_IN_TROLLEY = Menu.FIRST + 6;
    public static final int MENU_ITEM_EDIT = Menu.FIRST + 7;
    public static final int MENU_ITEM_CLEAR = Menu.FIRST + 8;
    public static final int MENU_ITEM_RESET = Menu.FIRST + 9;
    
    /**
     * Case selections for the type of dialog box displayed
     */
    private static final int DIALOG_INSERT = 1;
    private static final int DIALOG_DELETE = 2;
    private static final int DIALOG_EDIT = 3;
    private static final int DIALOG_CLEAR = 4;
    private static final int DIALOG_RESET = 5;
    
    //Modes
    private static final int MODE_LISTING = 1;
    private static final int MODE_SHOPPING = 2;
    
  //Use private members for dialog textview to prevent weird persistence problem
	private EditText mDialogEdit;
	private TextView mDialogText;
	private View mDialogView;

	private Cursor mCursor;
	private Button btn_listMode;
	private Button btn_shopMode;
	private ImageView icon_mode;
	private TrollyAdapter mAdapter;
	private SharedPreferences mPrefs;
	private int mMode;

	private Uri mUri;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(ShoppingList.CONTENT_URI);
        }
              
        setContentView(R.layout.trolly);  
        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
               
        mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null,
                ShoppingList.DEFAULT_SORT_ORDER);

        mAdapter = new TrollyAdapter(this, R.layout.shoppinglist_item, mCursor,
                new String[] { ShoppingList.ITEM}, new int[] { R.id.item});
        setListAdapter(mAdapter);
              
        btn_listMode = (Button)findViewById(R.id.listing_mode);
        btn_shopMode = (Button)findViewById(R.id.shopping_mode);
        icon_mode = (ImageView)findViewById(R.id.icon_mode);
        
        //mPrefs = getSharedPreferences(null, MODE_PRIVATE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mMode = mPrefs.getInt(KEY_MODE, MODE_LISTING);
        setMode(mMode);
        
        btn_listMode.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mMode = MODE_LISTING;
				setMode(mMode);
			}
        });
        
        btn_shopMode.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mMode = MODE_SHOPPING;
				setMode(mMode);
			}
        });
        
        if (intent.hasExtra(KEY_ITEM))
        	addIntentItems();
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		mMode = mPrefs.getInt(KEY_MODE, MODE_LISTING);
        setMode(mMode);
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences.Editor ed = mPrefs.edit();
		ed.putInt(KEY_MODE, mMode);
		ed.commit();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//super.onListItemClick(l, v, position, id);
		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		Cursor c = getContentResolver().query(uri, PROJECTION, null, null, null);
		c.moveToFirst();
		ContentValues values = new ContentValues();
		switch (c.getInt(c.getColumnIndex(ShoppingList.STATUS)))
		{
		case ShoppingList.OFF_LIST:
			//move from off the list to on the list
			values.put(ShoppingList.STATUS, ShoppingList.ON_LIST);
			getContentResolver().update(uri, values, null, null);
			break;
		case ShoppingList.ON_LIST:
			//move from on the list to in the trolley
			values.put(ShoppingList.STATUS, ShoppingList.IN_TROLLEY);
			getContentResolver().update(uri, values, null, null);
			break;
		case ShoppingList.IN_TROLLEY:
			//move back from in the trolley to on the list
			values.put(ShoppingList.STATUS, ShoppingList.ON_LIST);
			getContentResolver().update(uri, values, null, null);
			break;
		}
		
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            return false;
        }
        
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return false;
        }
        
        mUri = ContentUris.withAppendedId(getIntent().getData(), 
        									cursor.getLong(cursor.getColumnIndex(ShoppingList._ID)));
		Cursor c = getContentResolver().query(mUri, PROJECTION, null, null, null);
		c.moveToFirst();
		ContentValues values = new ContentValues();

        switch (item.getItemId()) {
	        case MENU_ITEM_ON_LIST:
                // Change to "on list" status
	        	values.put(ShoppingList.STATUS, ShoppingList.ON_LIST);
	        	getContentResolver().update(mUri, values, null, null);
	        	return true;	        	
	        case MENU_ITEM_OFF_LIST:
                // Change to "off list" status
	        	values.put(ShoppingList.STATUS, ShoppingList.OFF_LIST);
	        	getContentResolver().update(mUri, values, null, null);
                return true;
	        case MENU_ITEM_IN_TROLLEY:
	        	//Change to "in trolley" status
	        	values.put(ShoppingList.STATUS, ShoppingList.IN_TROLLEY);
	        	getContentResolver().update(mUri, values, null, null);
	        	return true;
	        case MENU_ITEM_EDIT:
	        	//Show edit dialog
	        	showDialog(DIALOG_EDIT);
	        	mDialogEdit.setText(c.getString(c.getColumnIndex(ShoppingList.ITEM)));
	        	return true;
	        case MENU_ITEM_DELETE:
	        	//Show are you sure dialog then delete
	        	showDialog(DIALOG_DELETE);
	        	mDialogText.setText(c.getString(c.getColumnIndex(ShoppingList.ITEM)));
	        	return true;
        }
        return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            return;
        }
		Cursor cursor = (Cursor)getListAdapter().getItem(info.position);
		if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(cursor.getColumnIndex(ShoppingList.ITEM)));
        int status = cursor.getInt(cursor.getColumnIndex(ShoppingList.STATUS));
        
    	//Add context menu items depending on current state
    	switch (status) {
        case ShoppingList.OFF_LIST:
        	menu.add(0, MENU_ITEM_ON_LIST, 0, R.string.move_on_list);
        	menu.add(0, MENU_ITEM_IN_TROLLEY, 0, R.string.move_in_trolley);
        	break;
        case ShoppingList.ON_LIST:
        	menu.add(0, MENU_ITEM_IN_TROLLEY, 0, R.string.move_in_trolley);
        	menu.add(0, MENU_ITEM_OFF_LIST, 0, R.string.move_off_list);
        	break;
        case ShoppingList.IN_TROLLEY:
        	menu.add(0, MENU_ITEM_ON_LIST, 0, R.string.move_on_list);
        	menu.add(0, MENU_ITEM_OFF_LIST, 0, R.string.move_off_list);
        	break;
        }
    	
        // Add context menu items that are relevant for all items
    	menu.add(0, MENU_ITEM_EDIT, 0, R.string.edit_item);
    	menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_INSERT, 1, R.string.insert_item)
        .setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_ITEM_CHECKOUT, 2, R.string.checkout)
        .setIcon(android.R.drawable.ic_media_next);
		menu.add(0, MENU_ITEM_CLEAR, 3, R.string.clear_list)
        .setIcon(android.R.drawable.ic_menu_revert);
		menu.add (0, MENU_ITEM_PREFERENCE, 4, R.string.preferences)
        .setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_ITEM_RESET, 5, R.string.reset_list)
        .setIcon(android.R.drawable.ic_menu_delete);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case MENU_ITEM_INSERT:
            // open dialog to insert a new item
        	showDialog(DIALOG_INSERT);
        	mDialogEdit.setText("");
            return true;
        case MENU_ITEM_CHECKOUT:
        	//Change all items from in trolley to off list
        	checkout();
        	return true;
        case MENU_ITEM_CLEAR:
        	//Change all items to off list
        	showDialog(DIALOG_CLEAR);
        	mDialogText.setText(R.string.clear_prompt);
        	return true;
        case MENU_ITEM_RESET:
        	//Change all items to off list
        	showDialog(DIALOG_RESET);
        	mDialogText.setText(R.string.reset_prompt);
        	return true;
        case MENU_ITEM_PREFERENCE:
        	startActivity(new Intent(this,TrollyPreferences.class));
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater factory = LayoutInflater.from(this);
		switch (id) {
		case DIALOG_INSERT:
            mDialogView = factory.inflate(R.layout.dialog_insert, null);
            mDialogEdit = (EditText)mDialogView.findViewById(R.id.insert);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.insert_item)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                		ContentValues values = new ContentValues();
                        values.put(ShoppingList.ITEM, mDialogEdit.getText().toString());
                		getContentResolver().insert(ShoppingList.CONTENT_URI,values);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		case DIALOG_EDIT:
            mDialogView = factory.inflate(R.layout.dialog_edit, null);
            mDialogEdit = (EditText)mDialogView.findViewById(R.id.edit);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.edit_item)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                		ContentValues values = new ContentValues();
                        values.put(ShoppingList.ITEM, mDialogEdit.getText().toString());
                		getContentResolver().update(mUri, values, null, null);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		case DIALOG_DELETE:
            mDialogView = factory.inflate(R.layout.dialog_confirm, null);
            mDialogText = (TextView)mDialogView.findViewById(R.id.dialog_confirm_prompt);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.delete_item)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                		getContentResolver().delete(mUri, null, null);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		case DIALOG_CLEAR:
            mDialogView = factory.inflate(R.layout.dialog_confirm, null);
            mDialogText = (TextView)mDialogView.findViewById(R.id.dialog_confirm_prompt);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.clear_list)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                		ContentValues values = new ContentValues();
                    	//Set all items status to "off list"
                    	values.put(ShoppingList.STATUS, ShoppingList.OFF_LIST);
                    	getContentResolver().update(getIntent().getData(), values, null, null);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		case DIALOG_RESET:
            mDialogView = factory.inflate(R.layout.dialog_confirm, null);
            mDialogText = (TextView)mDialogView.findViewById(R.id.dialog_confirm_prompt);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.reset_list)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	//Permanently delete all items from the list
                    	getContentResolver().delete(getIntent().getData(), null, null);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		}
		return null;
	}
    
	/**
	 * Change all items marked as "in trolley" to "off list"
	 */
    private void checkout() {
    	Cursor c = managedQuery(getIntent().getData(), PROJECTION, null, null,
                ShoppingList.DEFAULT_SORT_ORDER);
    	c.moveToFirst();
    	ContentValues values = new ContentValues();
    	values.put(ShoppingList.STATUS, ShoppingList.OFF_LIST);
    	Uri uri;
    	int status;
    	long id;
    	//loop through all items in the list
    	while (!c.isAfterLast()) {
    		status = c.getInt(c.getColumnIndex(ShoppingList.STATUS));
    		//if the item is not in the trolley jump to the next one
    		if (status == ShoppingList.IN_TROLLEY) {
	    		id = c.getLong(c.getColumnIndexOrThrow(ShoppingList._ID));
	    		uri = ContentUris.withAppendedId(getIntent().getData(), id);
	    		//Update the status of this item (in trolley) to "off list"
	    		getContentResolver().update(uri, values, null, null);
    		}
	    	c.moveToNext();
    	}
    }
    
    /**
     * Change the mode between shopping and list mode
     * @param mode
     */
    private void setMode(int mode) {
    	String sortOrder;
    	switch (mode) {
	    	case MODE_SHOPPING:
	    		btn_shopMode.setTextColor(getResources().getColor(R.color.red_text));
				btn_listMode.setTextColor(getResources().getColor(R.color.gray_text));
				icon_mode.setImageResource(R.drawable.shop_mode);
				try {
					sortOrder = mPrefs.getString(getString(R.string.key_sort_shop), ShoppingList.DEFAULT_SORT_ORDER);
					mCursor = managedQuery(getIntent().getData(), 
											PROJECTION, 
											ShoppingList.STATUS + "<>" + ShoppingList.OFF_LIST, 
											null,
											sortOrder);
					mAdapter.changeCursor(mCursor);
				}catch (SQLException e) {
					//Try a safer SQL query  
					mCursor = managedQuery(getIntent().getData(), 
							PROJECTION, 
							ShoppingList.STATUS + "<>" + ShoppingList.OFF_LIST, 
							null,
							ShoppingList.DEFAULT_SORT_ORDER);
					mAdapter.changeCursor(mCursor);
				}
				break;
	    	case MODE_LISTING:
	    		btn_listMode.setTextColor(getResources().getColor(R.color.red_text));
				btn_shopMode.setTextColor(getResources().getColor(R.color.gray_text));
				icon_mode.setImageResource(R.drawable.list_mode);
				sortOrder = mPrefs.getString(getString(R.string.key_sort_list), ShoppingList.DEFAULT_SORT_ORDER);
				try {
					mCursor = managedQuery(getIntent().getData(), 
											PROJECTION, 
											null, 
											null,
											sortOrder);
					mAdapter.changeCursor(mCursor);
				} catch (SQLException e) {
					//Try a safer SQL query
					mCursor = managedQuery(getIntent().getData(), 
							PROJECTION, 
							null, 
							null,
							ShoppingList.DEFAULT_SORT_ORDER);
					mAdapter.changeCursor(mCursor);
				}
	    		break;
    	}
    }
    
    /**
     * Add items received as extras in the intent to the list
     */
    private void addIntentItems() {
    	ArrayList<String> list = getIntent().getStringArrayListExtra(KEY_ITEM);
    	Cursor c;
    	long id;
    	Uri uri;
    	for (String item : list) {
    		c = getContentResolver().query(getIntent().getData(), 
    										PROJECTION, 
    										ShoppingList.ITEM + "='" + item + "'", 
    										null, 
    										null);

    		//If there is no match then just add the item to the list with "on list" status
    		c.moveToFirst();
    		if (c.isBeforeFirst()) {
    			ContentValues values = new ContentValues();
    			values.put(ShoppingList.ITEM, item);
    			values.put(ShoppingList.STATUS, ShoppingList.ON_LIST);
    			getContentResolver().insert(getIntent().getData(), values);
    		} else {
        	//If there is a list item that matches this item...
    			//get status of existing item
    			int status = c.getInt(c.getColumnIndex(ShoppingList.STATUS));
    			if (status == ShoppingList.OFF_LIST) {
        			//move an existing "off list" item to "on list"
        	    	ContentValues values = new ContentValues();
        			values.put(ShoppingList.STATUS, ShoppingList.ON_LIST);
        			id = c.getLong(c.getColumnIndex(ShoppingList._ID));
    				uri = ContentUris.withAppendedId(getIntent().getData(), id);
    				getContentResolver().update(uri, values, null, null);    				
    			} else { 
    				/**If an existing item already has a status of "on list" 
    				 * then create a new (duplicate) item with "on list" status.
    				 * This allows for the case where an item is already on the list
    				 * but is added again from another source.
    				 */
    				ContentValues values = new ContentValues();
        			values.put(ShoppingList.ITEM, item);
        			values.put(ShoppingList.STATUS, ShoppingList.ON_LIST);
        			getContentResolver().insert(getIntent().getData(), values);
    			}
    		}    		
    	}
    }

}





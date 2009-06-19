package captainfanatic.trolly;

import captainfanatic.provider.Trolly.ShoppingList;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class Trolly extends ListActivity {
	
/**
 * TrollyBinder view binder for simplecursoradapter
 * <P>Allows crossing off and fading out of list items.</P>
 * @author Ben
 *
 */
	private class TrollyBinder implements ViewBinder {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
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
			return true;
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
    
    /**
     * Case selections for the type of dialog box displayed
     */
    private static final int DIALOG_INSERT = 1;
    
  //Use private members for dialog textview to prevent weird persistence problem
	private EditText mDialogEdit;
	private View mDialogView;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(ShoppingList.CONTENT_URI);
        }

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
        
        // Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null,
                ShoppingList.DEFAULT_SORT_ORDER);

        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.shoppinglist_item, cursor,
                new String[] { ShoppingList.ITEM}, new int[] { R.id.item});
        adapter.setViewBinder(new TrollyBinder());
        setListAdapter(adapter);
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
		// TODO Auto-generated method stub
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_INSERT, 0, R.string.insert_item)
        .setShortcut('3', 'a')
        .setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_ITEM_CHECKOUT, 0, R.string.checkout)
        .setShortcut('3', 'a')
        .setIcon(android.R.drawable.ic_media_next);
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
                		Uri uri = getContentResolver().insert(ShoppingList.CONTENT_URI,values);
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
    
    
}
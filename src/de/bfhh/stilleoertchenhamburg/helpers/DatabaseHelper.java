package de.bfhh.stilleoertchenhamburg.helpers;

import java.util.ArrayList;
import java.util.List;

import de.bfhh.stilleoertchenhamburg.models.POI;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper { 

	private static DatabaseHelper sInstance;

	private final static String DATABASE_NAME = "toiletFinder";
	private final static int DATABASE_VERSION = 1;
	  
	private final static String TABLE_POIS = "toilets";
	private final static String KEY_ID = "id";
	private final static String KEY_NAME = "name";
	private final static String KEY_ADDRESS = "address";
	private final static String KEY_DESCR = "description";
	private final static String KEY_WEBSITE = "website";
	private final static String KEY_LAT = "latitude";
	private final static String KEY_LNG = "longitude";
	private final static String KEY_CAT_ID = "catId";
	

	public static DatabaseHelper getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new DatabaseHelper(context.getApplicationContext());
		}
		return sInstance;
	}
	
	private DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("DatabaseHelper", "onCreate");
		final String CREATE_POI_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_POIS + "(" + 
				KEY_ID + " INTEGER PRIMARY KEY," + 
				KEY_NAME + " TEXT," + 
				KEY_ADDRESS + " TEXT," + 
				KEY_DESCR + " TEXT," +
				KEY_WEBSITE + " TEXT," +
				KEY_LAT + " REAL," +
				KEY_LNG + " REAL," +
				KEY_CAT_ID + " INTEGER " +
				")";
        db.execSQL(CREATE_POI_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		clearData(db);
		onCreate(db);
	}
	
	public boolean isDataStillFresh(){
		// TODO: how old is my data? Does it need to be refreshed
		return false;
	}
	
	private void clearData(SQLiteDatabase db){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_POIS);
		onCreate(db);
	}
	
	public void refreshAllPOI(List<POI> pois) {
        SQLiteDatabase db = this.getWritableDatabase();
        clearData(db);
 
        for (POI poi : pois){
        	ContentValues values = new ContentValues();
            values.put(KEY_ID, poi.getId());
            values.put(KEY_NAME, poi.getName());
            values.put(KEY_ADDRESS, poi.getAddress());
            values.put(KEY_DESCR, poi.getDescription());
            values.put(KEY_WEBSITE, poi.getWebsite());
            values.put(KEY_LAT, poi.getLat());
            values.put(KEY_LNG, poi.getLng());
            db.insert(TABLE_POIS, null, values);
        }
        
        db.close();
     }
	
	// TODO: This should be used with lat, long and radius
	public List<POI> getAllPOI(){
		List<POI> poiList = new ArrayList<POI>();
		
		final String selectQuery = "SELECT * FROM " + TABLE_POIS + 
				"WHERE " + KEY_CAT_ID + "=127 ORDER BY id DESC";
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		
		if (cursor.moveToFirst()) {
			do {
				POI poi = new POI(
						Integer.parseInt(cursor.getString(0)), 
						cursor.getString(1),
						cursor.getString(2), 
						cursor.getString(3),
						cursor.getString(4),
						Double.parseDouble(cursor.getString(5)), 
						Double.parseDouble(cursor.getString(6))
						);
				poiList.add(poi);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		
		return poiList;
	}
}
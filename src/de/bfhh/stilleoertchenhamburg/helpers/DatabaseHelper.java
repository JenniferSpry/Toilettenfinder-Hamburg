package de.bfhh.stilleoertchenhamburg.helpers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.bfhh.stilleoertchenhamburg.models.POI;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author Jenne
 * To reduce httpRequests the data is stored in a SQLite Database 
 * and only fetched from the server if it is older than a certain time delta.
 * 
 * To fetch data first check {@link #isDataStillFresh}
 * If false first fetch data from server and refresh database with {@link #refreshAllPOI(List, Context)}
 * Once data is fresh use {@link #getAllPOI()} to get data
 */
public class DatabaseHelper extends SQLiteOpenHelper { 

	private static DatabaseHelper sInstance;

	private final static String DATABASE_NAME = "toiletFinder";
	private final static int DATABASE_VERSION = 1;
	
	//refresh Data if older than two hours
	// TODO: set to two days
	private final static Long MAX_TIME_DELTA = 2 * 60 * 60 * 1000L;
		
	private final static String PREF_FILE_NAME = "DataAgeSettings";
	private final static String PREF_KEY_DATA_AGE = "age_of_data";
	private final static String PREF_KEY_POI_AMOUNT = "poi_amount";
	  
	private final static String TABLE_POIS = "toilets";
	private final static String KEY_ID = "id";
	private final static String KEY_NAME = "name";
	private final static String KEY_ADDRESS = "address";
	private final static String KEY_DESCR = "description";
	private final static String KEY_WEBSITE = "website";
	private final static String KEY_LAT = "latitude";
	private final static String KEY_LNG = "longitude";
	private final static String KEY_CAT_ID = "catId";
	
	private final static int QUERY_CAT_ID = 127;
	

	/** use this method to get an instance of the class, don't instantiate it yourself */
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
	
	public boolean isDataStillFresh(Context context){	
		SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF_FILE_NAME, 0); // 0 = private mode
		int poisInDatabase = pref.getInt(PREF_KEY_POI_AMOUNT, 0);
		Log.d("refreshAllPOI", "POI in DB: "+poisInDatabase);
		if (poisInDatabase < 1){
			return false;
		}
		Long lastRefreshTime = pref.getLong(PREF_KEY_DATA_AGE, 0L);
		Long timeDelta = new Date().getTime() - lastRefreshTime;
		Log.i("DatabaseHelper", "Last data refresh " + (timeDelta/(1000*60)) + " minutes ago");
		if ((lastRefreshTime == null) || (timeDelta >= MAX_TIME_DELTA)) {
			return false;
		} else {
			return true;
		}
	}
	
	private void clearData(SQLiteDatabase db){
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_POIS);
		onCreate(db);
	}
	
	public void refreshAllPOI(List<POI> pois, Context context) {
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
            values.put(KEY_CAT_ID, QUERY_CAT_ID);
            db.insert(TABLE_POIS, null, values);
        }
        
        db.close();
        
        // Time of last Update and POI amount in DB tp sharedPreferences
		SharedPreferences pref = context.getApplicationContext().getSharedPreferences(PREF_FILE_NAME, 0); // 0 = private mode
		Editor edit = pref.edit();
		edit.putInt(PREF_KEY_POI_AMOUNT, pois.size());
		Log.d("refreshAllPOI", "POI in DB: "+pois.size());
		edit.putLong(PREF_KEY_DATA_AGE, new Date().getTime());
		edit.apply();
     }
	
	public ArrayList<POI> getAllPOI(){
		ArrayList<POI> poiList = new ArrayList<POI>();
		
		final String selectQuery = "SELECT * FROM " + TABLE_POIS + 
				" WHERE " + KEY_CAT_ID + " = " + QUERY_CAT_ID + " ORDER BY id DESC";
		
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
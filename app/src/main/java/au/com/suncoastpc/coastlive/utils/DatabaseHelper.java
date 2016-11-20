package au.com.suncoastpc.coastlive.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.dao.LruObjectCache;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.suncoastpc.coastlive.db.Artist;
import au.com.suncoastpc.coastlive.db.Genre;
import au.com.suncoastpc.coastlive.db.MusicEvent;
import au.com.suncoastpc.coastlive.db.Venue;

public class DatabaseHelper extends com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper  {
	private static final String DB_NAME = "coastlive.db";
	private static final int DB_VERSION = 1;
	private static final int LRU_CACHE_SIZE = 1024;     //the maximum number of entities to cache, per each entity class
	
	private static DatabaseHelper instance;
	
	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@SuppressWarnings("rawtypes")
	private static final Map<Class, com.j256.ormlite.dao.RuntimeExceptionDao> DAO_MAP = Collections.synchronizedMap(new HashMap<Class, com.j256.ormlite.dao.RuntimeExceptionDao>());
	
	public static synchronized DatabaseHelper getHelper(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        
        return instance;
    }
	
	public static DatabaseHelper getHelper() {
		return getHelper(Environment.getApplicationContext());
	}
	
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		//setup tables
		//XXX:  note that each class needs to be explicitly enumerated here
		try {
			TableUtils.createTable(connectionSource, Artist.class);					//v1
			TableUtils.createTable(connectionSource, Genre.class);					//v1
			TableUtils.createTable(connectionSource, Venue.class);				    //v1
			TableUtils.createTable(connectionSource, MusicEvent.class);		        //v1
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		//XXX:  implement any required database migrations here
		if (oldVersion == 1) {
			try {
				//updates, v1 -> v2
				//TableUtils.createTable(connectionSource, Site.class);
			}
			catch (Exception e) {
				Log.e("db", "Database migration failed:  " + e.getMessage(), e);
				return;
			}
		}
		if (oldVersion <= 2) {
			try {
				//updates, [v1,v2] -> v3
				//TableUtils.createTable(connectionSource, Language.class);
			}
			catch (Exception e) {
				Log.e("db", "Database migration failed:  " + e.getMessage(), e);
				return;
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public com.j256.ormlite.dao.RuntimeExceptionDao getRuntimeDao(Class daoClass) {
		if (! DAO_MAP.containsKey(daoClass)) {
			DAO_MAP.put(daoClass, getRuntimeExceptionDao(daoClass));
			DAO_MAP.get(daoClass).setObjectCache(new LruObjectCache(LRU_CACHE_SIZE));               //XXX:  appears to work well; should automatically maintain everything correctly as long as we don't use 'queryRaw' API's
		}
		
		return DAO_MAP.get(daoClass);
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> findAll(Class<T> entityClass) {
		List<T> result = new ArrayList<>();
		RuntimeExceptionDao<T, String> dao = getRuntimeDao(entityClass);
		
		try {
			String sortKey = entityClass.equals(MusicEvent.class) ? "startTime" : "name";
			result = dao.queryBuilder().orderBy(sortKey, true).query();
		}
		catch (Exception e) {
			Log.e("db", "Unexpected database error:  " + e.getMessage(), e);
		}
		
		return result;
	}

	public List<MusicEvent> findUpcomingEvents() {
		List<MusicEvent> result = new ArrayList<>();
		RuntimeExceptionDao<MusicEvent, String> dao = getRuntimeDao(MusicEvent.class);

		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 1);
			cal.add(Calendar.MILLISECOND, -2);

			long today = cal.getTimeInMillis();

			cal.add(Calendar.MONTH, 6);
			long sixMonths = cal.getTimeInMillis();

			result = dao.queryBuilder().orderBy("startTime", true).where().gt("startTime", today).and().lt("startTime", sixMonths).query();
		}
		catch (Exception e) {
			Log.e("db", "Unexpected database error:  " + e.getMessage(), e);
		}

		return result;
	}

	public List<Artist> findFaveArtists() {
		List<Artist> result = new ArrayList<>();
		RuntimeExceptionDao<Artist, String> dao = getRuntimeDao(Artist.class);

		try {
			result = dao.queryBuilder().orderBy("name", true).where().eq("favorite", true).query();
		}
		catch (Exception e) {
			Log.e("db", "Unexpected database error:  " + e.getMessage(), e);
		}

		return result;
	}

	public List<Venue> findFaveVenues() {
		List<Venue> result = new ArrayList<>();
		RuntimeExceptionDao<Venue, String> dao = getRuntimeDao(Venue.class);

		try {
			result = dao.queryBuilder().orderBy("name", true).where().eq("favorite", true).query();
		}
		catch (Exception e) {
			Log.e("db", "Unexpected database error:  " + e.getMessage(), e);
		}

		return result;
	}

	public List<Genre> findEnabledGenres() {
		List<Genre> result = new ArrayList<>();
		RuntimeExceptionDao<Genre, String> dao = getRuntimeDao(Genre.class);

		try {
			result = dao.queryBuilder().orderBy("name", true).where().eq("enabled", true).query();
		}
		catch (Exception e) {
			Log.e("db", "Unexpected database error:  " + e.getMessage(), e);
		}

		return result;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T findById(Class<T> entityClass, String id) {
		if (entityClass == null || StringUtilities.isEmpty(id)) {
			//invalid call
			return null;
		}

		RuntimeExceptionDao<T, String> dao = getRuntimeDao(entityClass);
		return dao.queryForId(id);
	}
	
	@Override
	public void close() {
		super.close();
		DAO_MAP.clear();
	}
}

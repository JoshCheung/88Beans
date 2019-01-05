package com.example.joshuacheung.beancount;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String COFFEE_TABLE = "COFFEE_TABLE";
    private static final String id_col = "ID";
    private static final String date_col = "date";
    private static final String cups_col= "cups_sold";
    private static final String weight_col = "weight";
    private static final String name_col = "name";

    //private static final String col = "factor";
//    public static String createTable;
//    private static final String HAYES_VALLEY = "HAYES_VALLEY";
//    private static final String BLEND = "BLEND";
//    private static final String SINGLE_ORIGIN_ESPRESSO = "SINGLE_ORIGIN_ESPRESSO";
//    private static final String DECAF = "DECAF";
//    private static final String SINGLE_ORIGIN_DRIP = "SINGLE_ORIGIN_DRIP";


//    String Hayes = "CREATE TABLE " + HAYES_VALLEY + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "date DATE NOT NULL, " + "weight float NOT NULL," + "TYPE TEXT)";
//    String Blend = "CREATE TABLE " + BLEND + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "date DATE NOT NULL, " + "weight float NOT NULL" + ")";
//    String SOE = "CREATE TABLE " + SINGLE_ORIGIN_ESPRESSO + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "date DATE NOT NULL, " + "weight float NOT NULL" + ")";
//    String Decaf = "CREATE TABLE " + DECAF + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "date DATE NOT NULL, " + "weight float NOT NULL" + ")";
//    String SOD = "CREATE TABLE " + SINGLE_ORIGIN_DRIP + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "date DATE NOT NULL, " + "weight float NOT NULL" + ")";


    public DatabaseHelper(Context context) {
        super(context, COFFEE_TABLE, null, 2 );
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String coffeeTable = "CREATE TABLE " + COFFEE_TABLE +
                "(" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "date DATE NOT NULL, " +
                    "cups_sold INT NOT NULL, " +
                    "weight FLOAT NOT NULL," +
                    "name TEXT NOT NULL" +
                ");";
        db.execSQL(coffeeTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + COFFEE_TABLE);
        onCreate(db);
    }

    public boolean addCoffeeType(String date, int cups_sold, double weight, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(date_col, date);
        contentValues.put(cups_col, cups_sold);
        contentValues.put(weight_col, weight);
        contentValues.put(name_col, name);
        String entry = (date + ", " + cups_sold + ", " + weight + ", " + name);

        long result = db.insert(COFFEE_TABLE, null, contentValues);
        Log.d(TAG, "addCoffeeType: adding " + entry + " to " + COFFEE_TABLE);

        if (result ==  -1 ) {
            return false;
        }
        else {

            return true;
        }
    }

    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
//        String query = "show tables;";
        String query = "SELECT * FROM " + COFFEE_TABLE;
        Cursor data = db.rawQuery(query ,null );
        return data;
    }

    public Cursor getItemID(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + id_col + " FROM " + COFFEE_TABLE + " WHERE " + name_col + " = '" + name + "'";
        Cursor data = db.rawQuery(query ,null );
        return data;
    }

    public void updateItem(int id, String oldName, String newDate, int newCups, double newWeight, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "UPDATE " + COFFEE_TABLE +

                    " SET " + date_col + " = '" + newDate + "'," +
                              cups_col + " = '" + newCups + "'," +
                            weight_col + " = '" + newWeight + "'," +
                              name_col + " = '" + newName +
                    "' WHERE " +
                                id_col + " = '" + id + "'" +
                    " AND " + name_col + " = '" + oldName + "'";

        Log.d(TAG, "updateItem: query: " + query);
        db.execSQL(query);
    }

    public void deleteAll(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + COFFEE_TABLE);
    }

    public void deleteName(int id, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + COFFEE_TABLE + " WHERE "
                        + id_col + " = '" + id + "'" + " AND "
                        + name_col + " = '" + name + "'";
        Log.d(TAG, "deleteName: query: " + query);
        Log.d(TAG, "deleteName: Deleting " + name + " from database.");
        db.execSQL(query);
    }
}

package com.codewind.taximeter.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 994856229 on 2019/2/12.
 */

public class DBHelper extends SQLiteOpenHelper{
    public DBHelper(Context context) {
        super(context, "route_history.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建一个数据库
        db.execSQL("CREATE TABLE IF NOT EXISTS  work_route (route_id integer primary key autoincrement ," +
                "work_date text not null ," +
                "work_time text not null ," +
                "work_distance text not null ," +
                "work_price text not null ," +
                "work_points text not null )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}

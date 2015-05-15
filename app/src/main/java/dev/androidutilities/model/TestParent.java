package dev.androidutilities.model;

import android.util.Log;

import alex.bobro.genericdao.annotation.FieldAnnotation;
import alex.bobro.genericdao.annotation.TableAnnotation;
import alex.bobro.genericdao.entities.SQLiteType;

/**
 * Created by alex on 2/11/15.
 */
@TableAnnotation(tableName = "test_table",keyField = "_id")
public class TestParent {

    @FieldAnnotation(dbType = SQLiteType.INTEGER)
    private int _id;

    @FieldAnnotation
    private String name;


    public TestParent() {
    }

    public TestParent(int _id, String name) {
        Log.i("test!", "constructor " + getClass().getSimpleName());
        this._id = _id;
        this.name = name;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

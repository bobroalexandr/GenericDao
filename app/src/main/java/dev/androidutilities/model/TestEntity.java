package dev.androidutilities.model;

import java.util.Date;
import java.util.List;

import alex.bobro.genericdao.annotation.FieldAnnotation;
import alex.bobro.genericdao.annotation.TableAnnotation;
import alex.bobro.genericdao.entities.RelationType;
import alex.bobro.genericdao.entities.SQLiteType;

/**
 * Created by Alex on 28.06.2015.
 */
@TableAnnotation(tableName = "test")
public class TestEntity {

    @FieldAnnotation
    private String field1;

    @FieldAnnotation
    private String field2;

    @FieldAnnotation
    private int field3;

    @FieldAnnotation
    private long field4;

    public TestEntity(String field1, String field2, int field3, long field4) {
        this.field1 = field1;
        this.field2 = field2;
        this.field3 = field3;
        this.field4 = field4;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public int getField3() {
        return field3;
    }

    public void setField3(int field3) {
        this.field3 = field3;
    }

    public long getField4() {
        return field4;
    }

    public void setField4(long field4) {
        this.field4 = field4;
    }
}

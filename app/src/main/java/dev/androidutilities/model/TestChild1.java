package dev.androidutilities.model;


import android.util.Log;

import alex.bobro.genericdao.annotation.FieldAnnotation;
import alex.bobro.genericdao.annotation.TableAnnotation;
import alex.bobro.genericdao.entities.RelationType;
import alex.bobro.genericdao.entities.SQLiteType;

import java.util.Date;
import java.util.List;


public class TestChild1 extends TestParent {

    @FieldAnnotation
    private String field1;

    @FieldAnnotation
    private String field2;

    @FieldAnnotation(relation = RelationType.MANY_TO_ONE)
    private TestChild2 child2;

    @FieldAnnotation(relation = RelationType.ONE_TO_MANY)
    private List<TestChild2> child2s;

    @FieldAnnotation(dbType = SQLiteType.INTEGER)
    private Date date;

    public TestChild1() {
    }


    public TestChild1(int _id, String name, String field1, String field2) {
        super(_id, name);
        this.field1 = field1;
        this.field2 = field2;
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

    public TestChild2 getChild2() {
        return child2;
    }

    public void setChild2(TestChild2 child2) {
        this.child2 = child2;
    }

    public List<TestChild2> getChild2s() {
        return child2s;
    }

    public void setChild2s(List<TestChild2> child2s) {
        this.child2s = child2s;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}

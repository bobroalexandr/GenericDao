package dev.androidutilities.model;

import alex.bobro.genericdao.annotation.FieldAnnotation;
import alex.bobro.genericdao.annotation.TableAnnotation;

/**
 * Created by Alex on 28.06.2015.
 */
@TableAnnotation(tableName = "test_nested", keyField = "field1")
public class TestEntityNested {

    @FieldAnnotation
    public String field1;

    @FieldAnnotation
    public int field3;


    public TestEntityNested() {

    }

    public TestEntityNested(String field1, int field3) {
        this.field1 = field1;
        this.field3 = field3;
    }

}

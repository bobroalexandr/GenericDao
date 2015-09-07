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
@TableAnnotation(tableName = "test", keyField = "field1")
public class TestEntity {

    @FieldAnnotation
    public String field1;

    @FieldAnnotation
    public String field2;

    @FieldAnnotation
    public int field3;

    @FieldAnnotation
    public long field4;

//    @FieldAnnotation
//    public String field5;

    @FieldAnnotation(relation = RelationType.MANY_TO_ONE)
    public TestEntityNested nested;

    public TestEntity() {

    }

    public TestEntity(String field1, String field2, int field3, long field4, TestEntityNested nested) {
        this.field1 = field1;
        this.field2 = field2;
        this.field3 = field3;
        this.field4 = field4;
//        this.field5 = nested.field1;
        this.nested = nested;
    }
}

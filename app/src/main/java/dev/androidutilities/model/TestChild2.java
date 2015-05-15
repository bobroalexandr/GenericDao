package dev.androidutilities.model;


import alex.bobro.genericdao.annotation.FieldAnnotation;
import alex.bobro.genericdao.annotation.TableAnnotation;

public class TestChild2 extends TestParent {

    @FieldAnnotation
    private String field1;

    @FieldAnnotation
    private String field3;

    public TestChild2() {
    }


    public TestChild2(int _id, String name, String field1, String field3) {
        super(_id, name);
        this.field1 = field1;
        this.field3 = field3;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }
}

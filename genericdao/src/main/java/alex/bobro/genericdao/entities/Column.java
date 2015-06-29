package alex.bobro.genericdao.entities;

import android.text.TextUtils;

import alex.bobro.genericdao.Scheme;
import alex.bobro.genericdao.annotation.FieldAnnotation;

import java.lang.reflect.Field;

public class Column {

    String name;
    SQLiteType type;
    String customAdditional;

    RelationType relationType;
    ForeignKeyActions foreignKeyActions;

    Field connectedField;
    Scheme scheme;

    public Column(String name) {
        this.name = name;
        this.type = SQLiteType.TEXT;
    }


    public Column(String name, SQLiteType type, String customAdditional, RelationType relationType, ForeignKeyActions foreignKeyActions) {
        this.name = name;
        this.type = type;
        this.customAdditional = customAdditional;
        this.relationType = relationType;
        this.foreignKeyActions = foreignKeyActions;
    }

    protected Column(FieldAnnotation fieldAnnotation) {
        this(fieldAnnotation.name(), fieldAnnotation.dbType(), fieldAnnotation.additional(), fieldAnnotation.relation(), fieldAnnotation.foreignKeyAction());
    }

    public Column(FieldAnnotation fieldAnnotation, Field connectedField, Scheme scheme) {
        this(fieldAnnotation);
        this.connectedField = connectedField;
        if(TextUtils.isEmpty(name))
            name = connectedField.getName();
        this.scheme = scheme;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public String getFullName() {
        return scheme.getName() + "." + getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SQLiteType getType() {
        return type;
    }

    public void setType(SQLiteType type) {
        this.type = type;
    }

    public String getCustomAdditional() {
        return customAdditional;
    }

    public void setCustomAdditional(String customAdditional) {
        this.customAdditional = customAdditional;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
    }

    public ForeignKeyActions getForeignKeyActions() {
        return foreignKeyActions;
    }

    public void setForeignKeyActions(ForeignKeyActions foreignKeyActions) {
        this.foreignKeyActions = foreignKeyActions;
    }

    public Field getConnectedField() {
        return connectedField;
    }

    public void setConnectedField(Field connectedField) {
        this.connectedField = connectedField;
    }

    public String getColumnsSql() {

        StringBuilder columnsSql = new StringBuilder(getName());
        columnsSql
            .append(" ")
            .append(type.getSqlType());

        if (!TextUtils.isEmpty(customAdditional)) {
            columnsSql
                .append(" ")
                .append(customAdditional);
        }
        return columnsSql.toString();
    }

    public Column as(Column column) {
        return new Column(getName() + " AS " + column.getName());
    }

    public boolean isCorrect() {
        return name != null && name.length() > 0 && type != null;
    }

    @Override
    public String toString() {
        return "Column{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", customAdditional='" + customAdditional + '\'' +
            '}';
    }
}

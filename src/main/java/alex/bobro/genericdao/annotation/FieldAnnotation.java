package alex.bobro.genericdao.annotation;

import alex.bobro.genericdao.entities.ForeignKeyActions;
import alex.bobro.genericdao.entities.RelationType;
import alex.bobro.genericdao.entities.SQLiteType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Alex on 27.01.14.
 */
@Target(ElementType.FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldAnnotation {

    String name() default "";

    SQLiteType dbType() default SQLiteType.TEXT;

    String additional() default "";

    RelationType relation() default RelationType.NONE;

    ForeignKeyActions foreignKeyAction() default ForeignKeyActions.CASCADE;
}
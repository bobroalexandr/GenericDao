import android.content.ContentValues;
import android.content.Context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

import alex.bobro.genericdao.ContextContentProvider;
import alex.bobro.genericdao.GenericContentProviderOperation;
import alex.bobro.genericdao.GenericDao;
import alex.bobro.genericdao.GenericDaoHelper;
import alex.bobro.genericdao.Scheme;
import dev.androidutilities.model.TestChild1;
import dev.androidutilities.model.TestChild2;

@RunWith(MockitoJUnitRunner.class)
public class GenericDaoTest {

    @Mock
    Context mockContext;

    @Before
    public void initDatabase() {
        Scheme.init(TestChild1.class, TestChild2.class);
        ContextContentProvider testContentProvider = new ContextContentProvider(mockContext);
        GenericDao.init(testContentProvider);
    }

    @Test
    public void checkScheme() {
        assertThat(Scheme.getInstances().size(), is(1));
        Scheme scheme = Scheme.getSchemeInstance(TestChild1.class);
        assertThat(scheme, notNullValue());
        scheme = Scheme.getSchemeInstance(TestChild2.class);
        assertThat(scheme, notNullValue());

        assertThat(scheme.getModelClasses().size(), is(2));
    }

    @Test
    public void checkDatabase() {
        assertThat(GenericDao.getInstance(), notNullValue());
        assertThat(GenericDao.getInstance().getDbHelper(), notNullValue());
    }

    @Test
    public void checkToCvMethod() {
        TestChild1 testChild1 = new TestChild1(0, "child1_name", "child1_f1", "child1_f2");

        Scheme scheme = Scheme.getSchemeInstance(TestChild1.class);
        ContentValues contentValues = new ContentValues();
        GenericDaoHelper.fillCvFromEntity(scheme, contentValues, testChild1, new ArrayList<GenericContentProviderOperation>(), null);
        assertThat(contentValues.size(), is(5));
        assertThat(contentValues.get(Scheme.COLUMN_OBJECT_CLASS_NAME).toString(),is(TestChild1.class.getName()));
    }

    @Test
    public void checkSave() {
//        TestChild1 testChild1 = new TestChild1(0, "child1_name", "child1_f1", "child1_f2");
//        GenericDao.getInstance().save(testChild1);
    }
}

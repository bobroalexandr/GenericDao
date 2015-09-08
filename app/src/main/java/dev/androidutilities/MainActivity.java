package dev.androidutilities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import alex.bobro.genericdao.GenericDao;
import alex.bobro.genericdao.RequestParameters;
import alex.bobro.genericdao.Scheme;
import dev.androidutilities.model.TestChild1;
import dev.androidutilities.model.TestChild2;
import dev.androidutilities.model.TestEntity;
import dev.androidutilities.model.TestEntityNested;
import dev.androidutilities.model.TestParent;


public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnSave).setOnClickListener(this);
    }

    public void saveItems() {
        TestChild1 testChild1 = new TestChild1(0, "child1_name", "child1_f1", "child1_f2");
        testChild1.setDate(Calendar.getInstance().getTime());
        TestChild2 testChild2 = new TestChild2(1, "child2_name", "child2_f1", "child2_f3");
        TestChild2 testChild2_1 = new TestChild2(2, "child2_1_name", "child2_1_f1", "child2_1_f3");
        testChild1.setChild2(testChild2_1);

        TestChild2 testChild2_2 = new TestChild2(3, "child2_2_name", "child2_2_f1", "child2_2_f3");
        TestChild2 testChild2_3 = new TestChild2(4, "child2_3_name", "child2_3_f1", "child2_3_f3");
        List<TestChild2> list = Arrays.asList(testChild2_2, testChild2_3);
        testChild1.setChild2s(list);

        GenericDao.getInstance().save(testChild1, null, null, new RequestParameters.Builder().withRequestMode(RequestParameters.RequestMode.JUST_PARENT).withNotificationMode(RequestParameters.NotificationMode.AFTER_ALL).build());
        GenericDao.getInstance().save(testChild1, null, null, new RequestParameters.Builder().withRequestMode(RequestParameters.RequestMode.JUST_NESTED).withNotificationMode(RequestParameters.NotificationMode.AFTER_ALL).build());
        GenericDao.getInstance().save(testChild2);

        RequestParameters.Builder builder = new RequestParameters.Builder()
                .withRequestMode(RequestParameters.RequestMode.JUST_PARENT);
        List<TestChild1> child1s = GenericDao.getInstance().getObjects(builder.build(), TestParent.class);
        GenericDao.getInstance().fillEntityWithNestedObjects(Scheme.getSchemeInstance(TestChild1.class),child1s.get(0), builder.build());

         Log.i("Test!", "test");
    }

    private void benchMarkSave() {
        GenericDao.getInstance().delete(TestEntity.class);
        GenericDao.getInstance().delete(TestEntityNested.class);
        List<TestEntity> entities = generateEnteties();
        long time = System.currentTimeMillis();
//        List<TestEntityNested> entitiesNest = new ArrayList<>();
//        for (TestEntity entity : entities) {
//            entitiesNest.add(entity.nested);
//        }
//        GenericDao.getInstance().saveCollection(entitiesNest, new RequestParameters.Builder().withNotificationMode(RequestParameters.NotificationMode.AFTER_ALL).build(),
//                new QueryParameters.Builder().addParameter(GenericDaoContentProvider.CONFLICT_ALGORITHM, String.valueOf(SQLiteDatabase.CONFLICT_REPLACE)).build());
        GenericDao.getInstance().saveCollection(entities, new RequestParameters.Builder().withNotificationMode(RequestParameters.NotificationMode.AFTER_ALL).build(),
                null);
        Log.i("tEST!", "save = " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        List<TestEntity> objects = GenericDao.getInstance().getObjects(TestEntity.class);
        Log.i("tEST!", "total get = " + (System.currentTimeMillis() - time));
    }

    private List<TestEntity> generateEnteties() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            TestEntityNested nested = new TestEntityNested(Utils.getRandomString(100), Utils.getRandomInt(100));
            entities.add(new TestEntity(Utils.getRandomString(100), Utils.getRandomString(100), Utils.getRandomInt(100),
                    Utils.getRandomLong(), nested));
        }
        return entities;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSave:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        benchMarkSave();
//                        saveItems();
                    }
                }).start();
                break;
        }
    }
}

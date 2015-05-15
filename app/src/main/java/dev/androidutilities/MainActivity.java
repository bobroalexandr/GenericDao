package dev.androidutilities;

import android.app.ActionBar;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;


import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import alex.bobro.genericdao.GenericDao;
import alex.bobro.genericdao.util.Reflection;
import dev.androidutilities.model.TestChild1;
import dev.androidutilities.model.TestChild2;
import dev.androidutilities.model.TestParent;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    PowerManager.WakeLock wakeLock;
    PowerManager powerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getLocalClassName());

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

        GenericDao.getInstance().save(testChild1);
        GenericDao.getInstance().save(testChild2);

        List<TestChild1> child1s = GenericDao.getInstance().getObjects(TestParent.class);

        Log.i("test!","dsa");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSave:
                wakeLock.acquire();
                wakeLock.release();
//                saveItems();
                break;
        }
    }
}

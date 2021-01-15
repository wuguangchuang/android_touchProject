package com.newskyer.meetingpad.fileselector.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.newskyer.meetingpad.R;
import com.newskyer.meetingpad.fileselector.fragment.PathSelectorFragment;

public class PathSelectorActivity extends AppCompatActivity {
    private PathSelectorFragment pathSelectorFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_select);

        pathSelectorFragment = new PathSelectorFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment,pathSelectorFragment);
        transaction.commit();
        findViewById(R.id.fragment).setVisibility(View.VISIBLE);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
    //    private void removePathSelectorLayout(){
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        transaction.remove(pathSelectorFragment);
//        transaction.commit();
//    }
}

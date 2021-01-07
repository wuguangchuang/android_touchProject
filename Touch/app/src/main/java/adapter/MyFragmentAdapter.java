package adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

import fragment_package.About_fragment;
import fragment_package.Setting_fragment;
import fragment_package.Signal_fragment;
import fragment_package.Test_fragment;
import fragment_package.Update_fragment;

public class MyFragmentAdapter extends FragmentPagerAdapter {
    private List<Fragment> list;
    private String[] mTitles = new String[]{
            "升级",
            "测试",
            "信号图",
            "设置",
            "关于"
    };

    public MyFragmentAdapter(FragmentManager fm){
        super(fm);
    }


    @NonNull
    @Override
    public Fragment getItem(int position) {
//
//        if(position == 1)
//            return new About_fragment();
//
//        return new Test_fragment();

        if(position == 1)
            return new Test_fragment();
        if(position == 2)
            return new Signal_fragment();
        else if(position == 3)
            return new Setting_fragment();
        else if(position == 4)
            return new About_fragment();

        return new Update_fragment();
    }

    @Override
    public int getCount() {
        return mTitles.length;
    }

    //ViewPager与TabLayout绑定后，这里获取到PageTitle就是Tab的Text
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }
}

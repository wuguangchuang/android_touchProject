package fragment_package;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.touch.MainActivity;
import com.example.touch.R;
import com.example.touch.TouchManager;

import fragment_interface.Test_fragment_interface;

public class Test_fragment extends Fragment implements Test_fragment_interface {

    private final String TAG = "myText";
    private Button testBtn;
    private ProgressBar testProgressBar;
    private ScrollView testScrollView;
    private TextView testTextView;
    private FrameLayout testFrameLayout;
    private ImageView imageView;
    private TextView imagetext;

    public String getTestText() {
        return testText;
    }

    private String testText = "";
    private boolean btnChecked = true;
    private int testProgress = 0;
    View view;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(view == null)
        {
            Log.d(TAG, "onCreateView: 缓存TestPage");
            ((MainActivity)getActivity()).setTest_fragment_interface(this);
            //加载Fragment
            view = inflater.inflate(R.layout.test_fragment,container,false);
            initTestFragment(view);
            if(((MainActivity)getActivity()).testString != null && !((MainActivity)getActivity()).testString.equals("") )
            {
                setTextViewStr(((MainActivity)getActivity()).testString);
            }

            testBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(btnChecked && testBtn.isEnabled())
                    {
                        if(TouchManager.upgradeRunning)
                        {
                            if(getActivity() != null)
                            {
                                Toast.makeText(getActivity(),"正在升级中,请勿操作！！",Toast.LENGTH_LONG).show();
                            }
                        }
                        else
                        {
                            testBtn.setText(R.string.cancel_test);
                            btnChecked = false;
                            ((MainActivity)getActivity()).test_interface.startTest();
                        }

                    }
                    else
                    {
                        testBtn.setBackgroundColor(getResources().getColor(R.color.gray_color));
                        testBtn.setEnabled(false);
                        ((MainActivity)getActivity()).test_interface.cancelTest();
                    }

                }
            });
            setTestInProgress(0);
        }

        return view;
    }
    public void initTestFragment(View view)
    {
        testBtn = view.findViewById(R.id.startTestBtn);
        testProgressBar = view.findViewById(R.id.test_progressBar);
        testTextView = view.findViewById(R.id.testTextView);
        testFrameLayout = view.findViewById(R.id.testFrameLayout);
        testScrollView = view.findViewById(R.id.testScrollView);
        imageView = view.findViewById(R.id.test_image);
        imagetext = view.findViewById(R.id.image_text);
    }


    @Override
    public void setTextViewStr(String text) {
        testText += text +"\n";
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    testTextView.setText(testText);
                    testScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });

        }
    }

    @Override
    public void setTestInProgress(final int progress) {
        testProgress = progress;
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    testProgressBar.setProgress(progress);
                }
            });

        }

    }

    @Override
    public void setTestBtn(final String text, boolean checked) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    testBtn.setText(text);
                    testBtn.setEnabled(true);
                    testBtn.setBackgroundColor(getResources().getColor(R.color.blue_color));
                }
            });

            btnChecked = checked;

        }
    }

    @Override
    public void setTestImageInfo(final int type, final String text) {
        if (getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (type){
                        case 1:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.success));
                            break;
                        case 2:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.error));
                            break;
                        case 3:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.no_touch));
                            break;
                        case 4:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.touch));
                            break;
                        case 5:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.update));
                            break;
                        case 6:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.disconnect));
                            break;
                        case 7:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.cancel));
                            break;
                        case 8:
                            imageView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.no_permission));
                            break;

                    }
                    imagetext.setText(text);

                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        testTextView.setText(testText);
        testScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        testProgressBar.setProgress(testProgress);
    }
}

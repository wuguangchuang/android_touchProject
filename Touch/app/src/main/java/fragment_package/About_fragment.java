package fragment_package;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.touch.MainActivity;
import com.example.touch.R;

import fragment_interface.About_fragment_interface;

public class About_fragment extends Fragment implements About_fragment_interface {

    private final String TAG = "myText";
    private TextView aboutText;
    private TextView aboutData;
    View view;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if(view == null)
        {
            ((MainActivity)getActivity()).setAbout_fragment_interface(this);
            Log.d(TAG, "onCreateView: 缓存AboutPage");
            view = inflater.inflate(R.layout.about_fragment,container,false);
            aboutData = view.findViewById(R.id.about_data);
            aboutText = view.findViewById(R.id.about_text);
            if(MainActivity.curPage == MainActivity.aboutTab)
                ((MainActivity)getActivity()).refreshAboutData();
        }
        return view;
    }

    @Override
    public void setTextViewText(final String text) {
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    aboutText.setText(text);
                }
            });

        }
    }

    @Override
    public void setTextViewData(final String text) {
        if(getActivity() != null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    aboutData.setText(text);
                }
            });

        }
    }

    @Override
    public void onResume() {
        setTextViewText(MainActivity.getDeviceInfo());
        setTextViewData(MainActivity.getSoftwareInfo());
        super.onResume();
    }
}

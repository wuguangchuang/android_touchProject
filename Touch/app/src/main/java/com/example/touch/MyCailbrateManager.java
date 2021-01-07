package com.example.touch;
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Arrays;


public class MyCailbrateManager extends Activity {



    public final String TAG = "myText";
    private volatile int mouseX;
    private volatile int mouseY;
    MyCalibrateView myCalibrateView;
    private volatile boolean mousePress = false;
    public volatile int[] oldC1 = new int[2]; //左上与右上坐标点X,Y轴坐标点
    public volatile int[] oldC2 = new int[2];
    public volatile int[] oldC3 = new int[2];
    public volatile int[] oldC4 = new int[2];
    public volatile int[] newC1 = new int[2]; //左上与右上坐标点X,Y轴坐标点
    public volatile int[] newC2 = new int[2];
    public volatile int[] newC3 = new int[2];
    public volatile int[] newC4 = new int[2];

    public volatile int oldMouseX ;
    public volatile int oldMouseY ;
    public volatile int moveEdge = 0;//上左下右 ————>1、2、3、4

    public ChangeCircleCoord changeCircleCoord;

    public int upOffset = 0;
    public int leftOffset = 0;
    public int downOffset = 0;
    public int rightOffset = 0;
    public int oldUpYRoller = 0;
    public int oldLeftXRoller = 0;
    public int oldRightXRoller = 0;
    public int oldDownYRoller = 0;


    private int suspension = 0;

    public int getAllowInterval() {
        return allowInterval;
    }

    private int allowInterval = 10;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        //去掉窗口标题
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        //创建MyCalibrateView组件
        final MyCalibrateView myCalibrateView = new MyCalibrateView(this);
        setContentView(myCalibrateView);

        changeCircleCoord = new ChangeCircleCoord();
        changeCircleCoord.start();

        View.OnHoverListener hoverListener = new View.OnHoverListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public boolean onHover(View view, MotionEvent event) {
                //不需要鼠标做任何操作，移动坐标便可以得到
                mouseX = (int)event.getX();
                mouseY = (int)event.getY();
//                Log.d(TAG, "onHover: " + String.format("[%d,%d]",mouseX,mouseY));
                return true;
            }
        };
        myCalibrateView.setOnHoverListener(hoverListener);

    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        int action = ev.getAction();
        //移动事件
        if(action == MotionEvent.ACTION_MOVE)
        {
            synchronized (MyCailbrateManager.this)
            {
                mouseX = (int)ev.getX();
                mouseY = (int)ev.getY();
            }

//            Log.d(TAG, "dispatchTouchEvent: ACTION_MOVE");
        }
        //按下左键并抬起后出发
        if(action == MotionEvent.BUTTON_PRIMARY)
        {
//            Log.d(TAG, "dispatchTouchEvent: BUTTON_PRIMARY");
        }
        //按下事件就触发
        if(action == MotionEvent.ACTION_DOWN)
        {
            Log.d(TAG, "dispatchTouchEvent:按下");
            myCalibrateView.touchManager.stopGetCalibrationCapture();
            mouseX = (int)ev.getX();
            mouseY = (int)ev.getY();
            setOldCircleData();
//            if((mouseX > oldC1[0] && mouseX < oldC2[0] && mouseY > (oldC1[1] - allowInterval) && mouseY < (oldC1[1]+allowInterval)) || //上
//                    (mouseX > oldC1[0] - allowInterval && mouseX < oldC1[0] + allowInterval && mouseY > oldC1[1] && mouseY < oldC3[1]) || //左
//                    (mouseX > oldC3[0] && mouseX < oldC4[0] && mouseY > (oldC3[1] - allowInterval) && mouseY < (oldC3[1]+allowInterval)) || //下
//                    (mouseX > oldC2[0] - allowInterval && mouseX < oldC2[0] + allowInterval && mouseY > oldC2[1] && mouseY < oldC4[1])) //右
//            {
//                Log.e(TAG, "hoverListener: 可以移动");

                if((mouseX > oldC1[0] && mouseX < oldC2[0] && mouseY > (oldC1[1] - allowInterval) && mouseY < (oldC1[1]+allowInterval)))
                {
                    //上边
                    moveEdge = 1;
                    oldUpYRoller = mouseY;
                    mousePress = true;
                }
                if((mouseX > oldC1[0] - allowInterval && mouseX < oldC1[0] + allowInterval && mouseY > oldC1[1] && mouseY < oldC3[1]))
                {
                    //左边
                    moveEdge = 2;
                    oldLeftXRoller = mouseX;
                    mousePress = true;
                }
                if(mouseX > oldC3[0] && mouseX < oldC4[0] && mouseY > (oldC3[1] - allowInterval) && mouseY < (oldC3[1]+allowInterval))
                {
                    //下边
                    moveEdge = 3;
                    oldDownYRoller = mouseY;
                    mousePress = true;
                }
                if(mouseX > oldC2[0] - allowInterval && mouseX < oldC2[0] + allowInterval && mouseY > oldC2[1] && mouseY < oldC4[1])
                {
                    //右边
                    moveEdge = 4;
                    oldRightXRoller = mouseX;
                    mousePress = true;
                }
                if(mouseX > oldC1[0] +  2 * allowInterval && mouseX < oldC2[0] - 2 * allowInterval &&
                mouseY > oldC1[1] + 2 * allowInterval && mouseY < oldC3[1] - 2 * allowInterval)
                {
                    //同时移动四边
                    moveEdge = 5;
                    oldUpYRoller = mouseY;
                    oldLeftXRoller = mouseX;
                    oldDownYRoller = mouseY;
                    oldRightXRoller = mouseX;
                    mousePress = true;
                }

//            }
        }
        //抬起触发
        if(action == MotionEvent.ACTION_UP)
        {

            myCalibrateView.upgradeTargetData();
            if(mousePress)
            {
                mousePress = false;
                myCalibrateView.touchManager.startGetCalibrationCapture();
            }
            Log.d(TAG, "dispatchTouchEvent: 抬起按键");
        }

        return true;
    }

    public void setView(MyCalibrateView view)
    {
        this.myCalibrateView = view;
        MainActivity.setCalibtareContext(this,view);
        registerForContextMenu(view);
    }
    class ChangeCircleCoord extends Thread{
        public void setRunning(boolean running) {
            this.running = running;
        }

        private boolean running = true;
        @Override
        public void run() {
            while (running)
            {
                synchronized (MyCailbrateManager.this)
                {
                    if((mouseX > oldC1[0] && mouseX < oldC2[0] && mouseY > (oldC1[1] - allowInterval) && mouseY < (oldC1[1]+allowInterval)))
                    {
                        //上边
                        suspension = 1;
                    }
                    else if((mouseX > oldC1[0] - allowInterval && mouseX < oldC1[0] + allowInterval && mouseY > oldC1[1] && mouseY < oldC3[1]))
                    {
                        //左边
                        suspension = 2;
                    }
                    else if(mouseX > oldC3[0] && mouseX < oldC4[0] && mouseY > (oldC3[1] - allowInterval) && mouseY < (oldC3[1]+allowInterval))
                    {
                        //下边
                        suspension = 3;
                    }
                    else if(mouseX > oldC2[0] - allowInterval && mouseX < oldC2[0] + allowInterval && mouseY > oldC2[1] && mouseY < oldC4[1])
                    {
                        //右边
                        suspension = 4;
                    }
                    else if(mouseX > oldC1[0] + allowInterval && mouseX < oldC2[0] - allowInterval &&
                            mouseY > oldC1[1] + allowInterval && mouseY < oldC3[1] - allowInterval)
                    {
                        suspension = 5;
                    }
                    if(mousePress)
                    {
                        if(moveEdge == 1)
                        {
                            suspension = 1;
                            upOffset = mouseY - oldUpYRoller;
                            leftOffset = 0;
                            downOffset = 0;
                            rightOffset = 0;
                            Log.e(TAG, "hoverListener: 上边移动距离 = " + upOffset);
                        }
                        else if(moveEdge == 2)
                        {
                            suspension = 2;
                            leftOffset = mouseX - oldLeftXRoller;
                            upOffset = 0;
                            downOffset = 0;
                            rightOffset = 0;
                            Log.e(TAG, "hoverListener: 左边移动距离 = " + leftOffset);
                        }
                        else if(moveEdge == 3)
                        {
                            suspension = 3;
                            downOffset = mouseY - oldDownYRoller;
                            upOffset = 0;
                            leftOffset = 0;
                            rightOffset = 0;
                            Log.e(TAG, "hoverListener: 下边移动距离 = " + downOffset);
                        }
                        else if(moveEdge == 4)
                        {
                            suspension = 4;
                            rightOffset = mouseX - oldRightXRoller;
                            upOffset = 0;
                            leftOffset = 0;
                            downOffset = 0;
                            Log.e(TAG, "hoverListener: 右边移动距离 = " + rightOffset);
                        }
                        else if(moveEdge == 5)
                        {
                            //同时移动四边
                            upOffset = mouseY - oldUpYRoller;
                            leftOffset = mouseX - oldLeftXRoller;
                            downOffset = mouseY - oldDownYRoller;
                            rightOffset = mouseX - oldRightXRoller;
                        }
                    }
                    if(mousePress)
                    {
                        myCalibrateView.setOffset(upOffset,leftOffset,downOffset,rightOffset);
                        oldUpYRoller = mouseY;
                        oldLeftXRoller = mouseX;
                        oldDownYRoller = mouseY;
                        oldRightXRoller = mouseX;
                    }
                    myCalibrateView.setSuspension(suspension);
                    suspension = 0;
                    MyCailbrateManager.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myCalibrateView.postInvalidate();
                        }
                    });
                }
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            initPopWindow(myCalibrateView);
            return false;
        }
        return false;
    }
    private void initPopWindow(View v) {

        View view = LayoutInflater.from(this).inflate(R.layout.popup_item, null, false);
        Button recalibrateBtn = (Button) view.findViewById(R.id.recalibrateBtn);
        Button exitBtn = (Button) view.findViewById(R.id.exitBtn);
        //1.构造一个PopupWindow，参数依次是加载的View，宽高
        final PopupWindow popWindow = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

//        popWindow.setAnimationStyle(R.anim.anim_pop);  //设置加载动画

        //这些为了点击非PopupWindow区域，PopupWindow会消失的，如果没有下面的
        //代码的话，你会发现，当你把PopupWindow显示出来了，无论你按多少次后退键
        //PopupWindow并不会关闭，而且退不出程序，加上下述代码可以解决这个问题
        popWindow.setTouchable(true);
        popWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });
        popWindow.setBackgroundDrawable(new ColorDrawable(0xffffffff));    //要为popWindow设置一个背景才有效

        Log.e(TAG, String.format("onKeyDown: mouseX = %d,mouseY = %d",mouseX,mouseY));
        //设置popupWindow显示的位置，参数依次是参照View，x轴的偏移量，y轴的偏移量
        int  offsetX = mouseX > (MyCalibrateView.width - 80)?(MyCalibrateView.width - 80) : mouseX;
        int  offsetY = mouseY > (MyCalibrateView.height - 60)?(MyCalibrateView.width - 60) :mouseY;
        Log.e(TAG, String.format("onKeyDown: offsetX = %d,offsetX = %d",offsetX,offsetX));
//        popWindow.showAsDropDown(v, offsetX, offsetY);
        popWindow.showAtLocation(v, Gravity.TOP | Gravity.LEFT, offsetX,offsetY);

        //设置popupWindow里的按钮的事件
        recalibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCalibrateView.setActivityNum(0);
//                Toast.makeText(MyCailbrateManager.this, "你点击了重新校准项", Toast.LENGTH_SHORT).show();
                popWindow.dismiss();
            }
        });
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCalibrateView.setExitCalibrate();
//                Log.e(TAG, "initPopWindow: 你点击了退出");
//                Toast.makeText(MyCailbrateManager.this, "你点击了退出~", Toast.LENGTH_SHORT).show();
                popWindow.dismiss();
            }
        });
    }

    public void setOldCircleData(){
        int []tmp = new int[4];
        tmp = myCalibrateView.getCircle1();
        oldC1 = Arrays.copyOf(tmp,2);
        tmp = myCalibrateView.getCircle2();
        oldC2 = Arrays.copyOf(tmp,2);
        tmp = myCalibrateView.getCircle3();
        oldC3 = Arrays.copyOf(tmp,2);
        tmp = myCalibrateView.getCircle4();
        oldC4 = Arrays.copyOf(tmp,2);
        initNewCircleData();
    }
    public void initNewCircleData(){
        newC1 = Arrays.copyOf(oldC1,2);
        newC2 = Arrays.copyOf(oldC2,2);
        newC3 = Arrays.copyOf(oldC3,2);
        newC4 = Arrays.copyOf(oldC4,2);

    }

    static void copyIntArray(int[] dest,int[] src,int length)
    {
        for(int i = 0;i < length && i < dest.length;i++)
        {
            dest[i] = src[i];
        }
    }
}

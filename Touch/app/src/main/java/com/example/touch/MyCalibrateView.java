package com.example.touch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import dataInformation.CalibrationCapture;
import dataInformation.CalibrationData;
import dataInformation.CalibrationSettings;
import fragment_package.Setting_fragment;


public class MyCalibrateView  extends View implements TouchManager.Calibrate_Interface{
    public final String TAG = "myText";
//    public CalibrateMode calibrateMode;
//    public CollectionSchedule collectionSchedule;
    public TouchManager touchManager;
    private Context context;
    private CalibrationSettings oldSettings;
    private List<CalibrationData> oldListCalibrateData;

    public int[] getCircle1() {
        return circle1;
    }

    public int[] getCircle2() {
        return circle2;
    }

    public int[] getCircle3() {
        return circle3;
    }

    public int[] getCircle4() {
        return circle4;
    }

    private int[] circle1 = new int[4];//x,y,bigR,smallR
    private int[] circle2 = new int[4];
    private int[] circle3 = new int[4];
    private int[] circle4 = new int[4];
    private float[] rect = new float[4]; //左上角(x,y),右下角(x,y)
    private boolean drawActivity = false;

    public void setActivityNum(int activityNum) {
        this.activityNum = activityNum;
        ininCalibrateData();
        postInvalidate();
        touchManager.calibrationCaptureThread.setRecalirate(true);
    }
    public void setExitCalibrate()
    {
        touchManager.calibrationCaptureThread.setExitCalibrate(true);
    }

    private int activityNum = 0;
    private float[] drawLines1 = new float[8];//直线组：直线1与直线2的X轴Y轴坐标 // 横线与竖线顺序
    private float[] drawLines2 = new float[8];//
    private float[] drawLines3 = new float[8];//
    private float[] drawLines4 = new float[8];//

    private int[] activityProgress = new int[4];

    private int suspension = 0;

    int bigRad;
    int smallRad;
    public static int width = 0;
    public static int height = 0;
    private int allowInterval = 0;

    private long autoExitTimer = 0;
    private CountDownTimer timer;



    public MyCalibrateView(final Context context) {
        super(context);
        this.context = context;
//        calibrateMode = MainActivity.touchManager;
        touchManager = MainActivity.touchManager;

        ((MyCailbrateManager)context).setView(this);
        ininCalibrateData();
        touchManager.setCalibrate_interface(this);
        touchManager.initCalibrateData(circle1,circle2,circle3,circle4,rect,drawLines1,drawLines2,
                drawLines3,drawLines4,width,height);
        touchManager.startGetCalibrationCapture();
        ((MyCailbrateManager)context).setOldCircleData();
//        copyOldCalibrateData();
        timer = new CountDownTimer(3000,2800){
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish()
            {
                touchManager.exitCalibrationMode();
                Setting_fragment.calibrateFinish = true;
                ((MyCailbrateManager)context).finish();
            }
        };
        ((MyCailbrateManager)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,"长按蓝色圈圈完成校准,无操作60s后自动退出",Toast.LENGTH_SHORT).show();
            }
        });
        allowInterval = ((MyCailbrateManager)context).getAllowInterval();
//        startWork();

        Log.d(TAG, "MyCalibrateView: 宽 = " + width  + "高 = " + height);
    }
    @Override
    public void calibrateFinshed() {
//        ((MyCailbrateManager)context).changeCircleCoord.setRunning(false);
        timer.start();
        Log.d(TAG, "dispatchTouchEvent: 开始计时");
    }
    @Override
    public void overCalivrate() {
        Log.e(TAG, "overCalivrate: 强制关闭校准界面" );
        ((MyCailbrateManager)context).changeCircleCoord.setRunning(false);
        Setting_fragment.calibrateFinish = true;
        ((MyCailbrateManager)context).finish();
    }

//    public void startWork(){
//
//        boolean retBool = calibrateMode.enterCalibrationMode();
//        if(!retBool)
//        {
//            if(collectionSchedule != null)
//                collectionSchedule.setRunning(false);
//            Log.e(TAG, "startWork: 进入校准模式失败");
//            ((MyCailbrateManager)context).finish();
//        }
//        Toast.makeText(context,"长按蓝色圈圈完成校准,无操作60s后自动退出",Toast.LENGTH_SHORT).show();
//        setPointActive(0);
//
//    }
    public void ininCalibrateData(){
        rect[0] = width / 4;
        rect[1] = height / 4;
        rect[2] = width / 4 * 3;
        rect[3] = height / 4 * 3;
        bigRad = (width > height ? height : width) / 16;
        smallRad = (width > height ? height : width) / 32;
        circle1[0] = width / 4;
        circle1[1] = height / 4;
        circle1[2] = bigRad;
        circle1[3] = smallRad;

        circle2[0] = width / 4 * 3;
        circle2[1] = height / 4;
        circle2[2] = bigRad;
        circle2[3] = smallRad;

        circle3[0] = width / 4;
        circle3[1] = height / 4 * 3;
        circle3[2] = bigRad;
        circle3[3] = smallRad;

        circle4[0] = width / 4 * 3;
        circle4[1] = height / 4 * 3;
        circle4[2] = bigRad;
        circle4[3] = smallRad;

        drawLines1[0] = (width / 4 - (bigRad + smallRad) > 0) ? width / 4 - (bigRad + smallRad):0;
        drawLines1[1] = height / 4;
        drawLines1[2] = (width / 4 + (bigRad + smallRad) > width) ? width:width / 4 + (bigRad + smallRad);
        drawLines1[3] = height / 4;
        drawLines1[4] = width / 4;
        drawLines1[5] = (height / 4 - (bigRad + smallRad) > 0) ? height / 4 - (bigRad + smallRad):0;
        drawLines1[6] = width / 4;
        drawLines1[7] = (height / 4 + (bigRad + smallRad) > height) ? height:height / 4 + (bigRad + smallRad);

        drawLines2[0] = (width / 4 * 3 - (bigRad + smallRad) > 0) ? width / 4 * 3 - (bigRad + smallRad):0;
        drawLines2[1] = height / 4;
        drawLines2[2] = (width / 4 * 3 + (bigRad + smallRad) > width) ? width:width / 4*3 + (bigRad + smallRad);
        drawLines2[3] = height / 4;
        drawLines2[4] = width / 4  * 3;
        drawLines2[5] = (height / 4 - (bigRad + smallRad) > 0) ? height / 4 - (bigRad + smallRad):0;
        drawLines2[6] = width / 4 * 3;
        drawLines2[7] = (height / 4 + (bigRad + smallRad) > height) ? height:height / 4 + (bigRad + smallRad);

        drawLines3[0] = (width / 4 - (bigRad + smallRad) > 0) ? width / 4 - (bigRad + smallRad):0;
        drawLines3[1] = height / 4 * 3;
        drawLines3[2] = (width / 4 + (bigRad + smallRad) > width) ? width:width / 4 + (bigRad + smallRad);
        drawLines3[3] = height / 4 * 3;
        drawLines3[4] = width / 4;
        drawLines3[5] = (height / 4 * 3 - (bigRad + smallRad) > 0) ? height / 4 * 3 - (bigRad + smallRad):0;
        drawLines3[6] = width / 4;
        drawLines3[7] = (height / 4 * 3 + (bigRad + smallRad) > height) ? height:height / 4 * 3 + (bigRad + smallRad);

        drawLines4[0] = (width / 4 * 3 - (bigRad + smallRad) > 0) ? width / 4 * 3 - (bigRad + smallRad):0;
        drawLines4[1] = height / 4 * 3;
        drawLines4[2] = (width / 4 * 3 + (bigRad + smallRad) > width) ? width:width / 4*3 + (bigRad + smallRad);
        drawLines4[3] = height / 4 * 3;
        drawLines4[4] = width / 4  * 3;
        drawLines4[5] = (height / 4 * 3 - (bigRad + smallRad) > 0) ? height / 4 * 3 - (bigRad + smallRad):0;
        drawLines4[6] = width / 4 * 3;
        drawLines4[7] = (height / 4 * 3 + (bigRad + smallRad) > height) ? height:height / 4 * 3 + (bigRad + smallRad);

//        invalidate();
        drawActivity = true;
        activityNum = 0;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);                  //蓝色背景
        Paint mPaint = new Paint();
        mPaint.setAntiAlias(true);                       //设置画笔为无锯齿

        // 描边
        mPaint.setStrokeWidth((float) 1.0);              //线宽
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.gray_color));
        canvas.drawRect(rect[0],rect[1],rect[2],rect[3],mPaint);

        mPaint.setStrokeWidth((float) 2.0);              //线宽

        if(drawActivity && activityNum == 0)
        {

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.blue_color)); //设置画笔颜色
            canvas.drawCircle(circle1[0], circle1[1], circle1[2], mPaint);           //绘制圆形
            RectF rectF = new RectF(circle1[0]-circle1[2],circle1[1] - circle1[2],circle1[0]+circle1[2],circle1[1] + circle1[2]);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setColor(Color.GREEN); //设置画笔颜色
            canvas.drawArc(rectF,270,(int)(activityProgress[0] / 100.0 * 360),true,mPaint);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.white_color)); //设置画笔颜色
            canvas.drawCircle(circle1[0], circle1[1], circle1[3], mPaint);           //绘制圆形
        }

        mPaint.setStrokeWidth((float) 2.0);              //线宽
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle1[0], circle1[1], circle1[2], mPaint);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle1[0], circle1[1], circle1[3], mPaint);

        if(drawActivity && activityNum == 1)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.blue_color)); //设置画笔颜色
            canvas.drawCircle(circle2[0], circle2[1], circle2[2], mPaint);           //绘制圆形
            RectF rectF = new RectF(circle2[0]-circle2[2],circle2[1] - circle2[2],circle2[0]+circle2[2],circle2[1] + circle2[2]);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setColor(Color.GREEN); //设置画笔颜色
            canvas.drawArc(rectF,270,(int)(activityProgress[1] / 100.0 * 360),true,mPaint);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.white_color)); //设置画笔颜色
            canvas.drawCircle(circle2[0], circle2[1], circle2[3], mPaint);           //绘制圆形
        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle2[0], circle2[1], circle2[2], mPaint);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle2[0], circle2[1], circle2[3], mPaint);

        if(drawActivity && activityNum == 2)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.blue_color)); //设置画笔颜色
            canvas.drawCircle(circle3[0], circle3[1], circle3[2], mPaint);           //绘制圆形
            RectF rectF = new RectF(circle3[0]-circle3[2],circle3[1] - circle3[2],circle3[0]+circle3[2],circle3[1] + circle3[2]);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setColor(Color.GREEN); //设置画笔颜色
            canvas.drawArc(rectF,270,(int)(activityProgress[2] / 100.0 * 360),true,mPaint);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.white_color)); //设置画笔颜色
            canvas.drawCircle(circle3[0], circle3[1], circle3[3], mPaint);           //绘制圆形
        }

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle3[0], circle3[1], circle3[2], mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle3[0], circle3[1], circle3[3], mPaint);

        if(drawActivity && activityNum == 3)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.blue_color)); //设置画笔颜色
            canvas.drawCircle(circle4[0], circle4[1], circle4[2], mPaint);           //绘制圆形
            RectF rectF = new RectF(circle4[0]-circle4[2],circle4[1] - circle4[2],circle4[0]+circle4[2],circle4[1] + circle4[2]);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setColor(Color.GREEN); //设置画笔颜色
            canvas.drawArc(rectF,270,(int)(activityProgress[3] / 100.0 * 360),true,mPaint);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.white_color)); //设置画笔颜色
            canvas.drawCircle(circle4[0], circle4[1], circle4[3], mPaint);           //绘制圆形
        }

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle4[0], circle4[1], circle4[2], mPaint);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawCircle(circle4[0], circle4[1], circle4[3], mPaint);

        // 描边+填充
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setColor(getResources().getColor(R.color.black_color));
        canvas.drawLines(drawLines1,mPaint);
        canvas.drawLines(drawLines2,mPaint);
        canvas.drawLines(drawLines3,mPaint);
        canvas.drawLines(drawLines4,mPaint);
        //画悬浮矩形
        switch (suspension)
        {
            case 1:
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setColor(getResources().getColor(R.color.suspension_rect_color));
                canvas.drawRect(circle1[0],circle1[1] - allowInterval,circle2[0],circle2[1] + allowInterval,mPaint);
                break;
            case 2:
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setColor(getResources().getColor(R.color.suspension_rect_color));
                canvas.drawRect(circle1[0] - allowInterval,circle1[1] ,circle3[0] + allowInterval,circle3[1],mPaint);
                break;
            case 3:
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setColor(getResources().getColor(R.color.suspension_rect_color));
                canvas.drawRect(circle3[0],circle3[1] - allowInterval,circle4[0],circle4[1] + allowInterval,mPaint);
                break;
            case 4:
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setColor(getResources().getColor(R.color.suspension_rect_color));
                canvas.drawRect(circle2[0] - allowInterval,circle2[1] ,circle4[0] + allowInterval,circle4[1],mPaint);
                break;
            case 5:
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint.setColor(getResources().getColor(R.color.suspension_rect_color));
                canvas.drawRect(circle1[0],circle1[1] ,circle4[0],circle4[1],mPaint);
        }
        canvas.save();
    }
    public boolean setPointActive(){
        ((MyCailbrateManager)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });

        return true;
    }
    public void refreshCollectionSchedule(CalibrationCapture calibrationCapture)
    {
        activityNum = calibrationCapture.index;
        ((MyCailbrateManager)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (calibrationCapture.count > 0 && calibrationCapture.finished == calibrationCapture.count) {
                    activityProgress[calibrationCapture.index] = 100;
                    invalidate();
                    if(activityNum == 3)
                    {
                        ((MyCailbrateManager)context).changeCircleCoord.setRunning(false);
                        Toast.makeText(context,"校准完成，3秒后自动退出",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    activityNum++;
                    invalidate();
                }
                else
                {
                    if(calibrationCapture.finished != 0)
                    {
                        activityProgress[calibrationCapture.index] = (int) (calibrationCapture.finished / (float)(calibrationCapture.count) * 100);
                        Log.e(TAG, "refreshCollectionSchedule: 采集进度 = " + activityProgress[calibrationCapture.index]);
                        postInvalidate();
                        //invalidate();
                    }
                    if(calibrationCapture.finished == 0)
                    {
                        activityProgress[calibrationCapture.index] = 0;
                        postInvalidate();
                    }
                }
            }
        });

    }

    @Override
    public void saveOldCalibratedata(List<CalibrationData> _oldListCalibrateData) {
        this.oldListCalibrateData = new ArrayList<>();
       for(int i = 0;i < _oldListCalibrateData.size();i++)
       {
           this.oldListCalibrateData.add(_oldListCalibrateData.get(i));
       }
    }
    public void setOffset(int upOffset,int leftOffset,int downOffset,int rightOffset)
    {
        synchronized (this)
        {
        activityNum = 0;
        int circleInterval = 10;
        if(upOffset != 0)
        {
            circle1[1] = Math.max(0,Math.min(circle1[1] + upOffset,circle3[1] - circleInterval));
            circle2[1] = circle1[1];

            drawLines1[1] = Math.max(0,Math.min(drawLines1[1] + upOffset,circle3[1] - circleInterval));
            drawLines1[3] = drawLines1[1];
            if(circle1[1] >= 0 && circle1[1] <= bigRad + smallRad)
            {
                drawLines1[5] = 0;
                drawLines1[7] = circle1[1] + bigRad + smallRad;
            }
            if(circle1[1] > bigRad + smallRad && circle1[1] < height - (bigRad + smallRad))
            {
                drawLines1[5] = circle1[1] - (bigRad + smallRad);
                drawLines1[7] = circle1[1] + smallRad + bigRad;
            }
            if(circle1[1] >= height - (bigRad + smallRad))
            {
                drawLines1[5] = height - bigRad - smallRad;
                drawLines1[7] = height;
            }

            drawLines2[1] = drawLines1[1];
            drawLines2[3] = drawLines1[3];
            drawLines2[5] = drawLines1[5];
            drawLines2[7] = drawLines1[7];
            rect[1] = circle1[1];
        }
        if(leftOffset != 0)
        {
            circle1[0] = Math.max(0,Math.min(circle1[0] + leftOffset,circle2[0] - circleInterval));
            circle3[0] = circle1[0];

            drawLines1[4] = Math.max(0,Math.min(drawLines1[4] + leftOffset,circle2[0] - circleInterval));
            drawLines1[6] = drawLines1[4];
            if(circle1[0] >= 0 && circle1[0] <= bigRad + smallRad)
            {
                drawLines1[0] = 0;
                drawLines1[2] = circle1[0] + bigRad + smallRad;
            }
            if(circle1[0] > bigRad + smallRad && circle1[0] < width - (bigRad + smallRad))
            {
                drawLines1[0] = circle1[0] - (bigRad + smallRad);
                drawLines1[2] = circle1[0] + smallRad + bigRad;
            }
            if(circle1[0] >= width - (bigRad + smallRad))
            {
                drawLines1[0] = width - bigRad - smallRad;
                drawLines1[2] = width;
            }

//            drawLines3[4] = Math.max(bigRad,Math.min(drawLines3[4] + leftOffset,circle2[0] - circleInterval));
            drawLines3[4] = drawLines1[4];
            drawLines3[6] = drawLines1[6];
            drawLines3[0] = drawLines1[0];
            drawLines3[2] = drawLines1[2];
            rect[0] = circle1[0];
        }
        if(downOffset != 0)
        {
            circle3[1] = Math.min(height,Math.max(circle3[1] + downOffset,circle1[1] + circleInterval));
            circle4[1] = circle3[1];


            drawLines3[1] = Math.min(height,Math.max(drawLines3[1] + downOffset,circle1[1] + circleInterval));
            drawLines3[3] = drawLines3[1];
            if(circle3[1] >= height - (bigRad + smallRad))
            {
                drawLines3[5] = circle3[1] - bigRad - smallRad;
                drawLines3[7] = height;
            }
            if(circle3[1] < height - (bigRad + smallRad) && circle3[1] > circle1[1] + circleInterval )
            {
                drawLines3[5] = circle3[1] - (bigRad + smallRad);
                drawLines3[7] = circle3[1] + smallRad + bigRad;
            }
            if(circle3[1]  < circle1[1] + circleInterval)
            {
                drawLines3[5] = circle3[1] - smallRad - bigRad;
                drawLines3[7] = circle3[1] + bigRad + smallRad;
            }

            drawLines4[1] = drawLines3[1];
            drawLines4[3] = drawLines3[3];
            drawLines4[5] = drawLines3[5];
            drawLines4[7] = drawLines3[7];
            rect[3] = circle4[1];
        }
        if(rightOffset != 0)
        {
            circle2[0] = Math.max(circle1[0] + circleInterval,Math.min(width,circle2[0] + rightOffset));
            circle4[0] = circle2[0];

            if(circle2[0] <= bigRad + smallRad)
            {
                drawLines2[0] = 0;
                drawLines2[2] = circle2[0] + smallRad + bigRad;
            }
            if(circle2[0] > bigRad + smallRad && circle2[0] < width - smallRad - bigRad )
            {
                drawLines2[0] = circle2[0] - smallRad - bigRad;
                drawLines2[2] = circle2[0] + smallRad + bigRad;
            }
            if(circle2[0] > width - smallRad - bigRad)
            {
                drawLines2[0] = circle2[0] - smallRad - bigRad;
                drawLines2[2] = width;
            }
            drawLines2[4] = circle2[0];
            drawLines2[6] = circle2[0];

            drawLines4[0] = drawLines2[0];
            drawLines4[2] = drawLines2[2];
            drawLines4[4] = drawLines2[4];
            drawLines4[6] = drawLines2[6];
            rect[2] = circle4[0];
        }
        }
        ((MyCailbrateManager)context).setOldCircleData();
        postInvalidate();
    }
    public void setSuspension(int suspension)
    {
        this.suspension = suspension;
//        postInvalidate();
    }
    public void upgradeTargetData()
    {
        synchronized (this)
        {
            touchManager.initCalibrateData(circle1,circle2,circle3,circle4,rect,drawLines1,drawLines2,
                    drawLines3,drawLines4,width,height);
        }

    }

//    class CollectionSchedule extends Thread{
//        public void setRunning(boolean running) {
//            this.running = running;
//        }
//
//        private boolean running = true;
//        @Override
//        public void run() {
//            while (running)
//            {
//                //访问设备采集进度
//                CalibrationCapture calibrationCapture = new CalibrationCapture();
//                int ret = calibrateMode.getCalibrationCapture(calibrationCapture);
//                if(ret < 0)
//                {
//                    Log.e(TAG, "setPointActive: 访问设备采集进度失败" );
//                    break;
//                }
//                if (calibrationCapture.count > 0 && calibrationCapture.finished == calibrationCapture.count) {
//                    activityProgress[calibrationCapture.index] = 100;
//                    ((MyCailbrateManager)context).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            invalidate();
//                        }
//                    });
//                    timer.cancel();
//                    timer.start();
//                    if(activityNum == 3)
//                    {
//                        calibrateMode.saveCalibrationData();
//                        ((MyCailbrateManager)context).runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(context,"校准完成，3秒后自动退出",Toast.LENGTH_SHORT).show();
//                            }
//                        });
//
//                        try {
//                            sleep(3000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        running = false;
//                        restoreOldCalibrateData();
//                        calibrateMode.exitCalibrationMode();
//                        ((MyCailbrateManager)context).finish();
//                        return;
//                    }
//
//                    activityNum = calibrationCapture.index + 1;
//                    setPointActive(activityNum);
//                    break;
//                }
//                else
//                {
//                    if(calibrationCapture.finished != 0)
//                    {
//                        timer.cancel();
//                        timer.start();
//                        activityProgress[calibrationCapture.index] = (int) (calibrationCapture.finished / (float)(calibrationCapture.count) * 100);
//                        Log.e(TAG, "run: activityProgress[calibrationCapture.index] = " + activityProgress[calibrationCapture.index]);
//                        postInvalidate();
//                        //invalidate();
//                    }
//                    if(calibrationCapture.finished == 0)
//                    {
//                        activityProgress[calibrationCapture.index] = 0;
//                        postInvalidate();
//                    }
//                }
//                try {
//                    sleep(50);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//    public void copyOldCalibrateData()
//    {
//        oldSettings = new CalibrationSettings();
//        int ret = calibrateMode.getCalibrationSettings(oldSettings);
//        if(ret < 0)
//        {
//            Log.e(TAG, "copyOldCalibrateData fail");
//            return;
//        }
//
//        oldListCalibrateData = new ArrayList<>();
//        for (int i = 0; i < oldSettings.pointCount; i++) {
//            CalibrationData data = new CalibrationData();
//            ret = calibrateMode.getCalibrationPointData((byte)1,(byte)i,data);
//            if(ret < 0)
//                return;
//            oldListCalibrateData.add(data);
//        }
//    }
//    public void restoreOldCalibrateData(){
//        for (int i = 0; i < oldSettings.pointCount; i++) {
//            calibrateMode.setCalibrationPointData((byte)i,oldListCalibrateData.get(i));
//        }
//        calibrateMode.saveCalibrationData();
//    }
//    public void recalibrate(){
//        restoreOldCalibrateData();
//        ininCalibrateData();
//        timer.cancel();
//        timer.start();
//        setPointActive(0);
//    }
//
//
//    public interface CalibrateMode{
//        boolean enterCalibrationMode();
//        boolean exitCalibrationMode();
//        int setCalibrationPointData(byte index,CalibrationData data);
//        int startCalibrationCapture(byte index);
//        int getCalibrationCapture(CalibrationCapture data);
//        int saveCalibrationData();
//        int getCalibrationSettings(CalibrationSettings settings);
//        int getCalibrationPointData(byte where,byte i,CalibrationData data);
//    }
}

package com.example.touch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import dataInformation.SignalData;
import fragment_package.Signal_fragment;

public class SignalCanvasView extends View {

    private final String TAG = "myText";
    private Context context;
    private float viewHeight;
    private float viewWidth;
    private int maxCount = 100;
    private int signalItemCount = 1;
    private float itemHeight;
    private float itemWidth;
    private float itemSignalHeight;
    private float itemSignalWidth;
    private float YcoordInterval = 15;
    private float YactualInterval = 15;
    private float XcoordInterval = 5;
    private float XactualInterval = 5;
    private List<SignalData> signalDataList;

    private int leftMargin = 30;
    private int bottomMargin = 10;
    public static volatile boolean refreshInterface = false;

    public SignalCanvasView(Context context) {
        super(context);
        this.context = context;
        signalDataList = new ArrayList<>();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
            refreshInterface = true;
            if(signalDataList.size() >=0 && signalDataList.size() <= 2)
            {
                YcoordInterval = 5;
                YactualInterval = 5;
            }
            else
            {
                YcoordInterval = 15;
                YactualInterval = 15;
            }

            viewHeight = Signal_fragment.canvasFrame.getHeight();
            viewWidth = Signal_fragment.canvasFrame.getWidth();
//        Log.e(TAG, "refreshCancasData: viewHeight = " + viewHeight);
//        Log.e(TAG, "refreshCancasData: viewWidth = " + viewWidth);
            itemHeight = viewHeight / signalItemCount;
            itemWidth = viewWidth;
            itemSignalHeight = itemHeight - bottomMargin;
            YactualInterval = itemSignalHeight / (255 + YcoordInterval) * YcoordInterval;
            itemSignalWidth = viewWidth - 100;
            XactualInterval = (itemSignalWidth - leftMargin) / (maxCount + 1) * XcoordInterval;
            float YeveryDataInterval = itemSignalHeight / (255 + YcoordInterval);
            float XeveryDataInterval = (itemSignalWidth - leftMargin) / (maxCount + 1);

            canvas.drawColor(Color.WHITE);                  //白色背景
            Paint mPaint = new Paint();
            mPaint.setAntiAlias(true);                       //设置画笔为无锯齿

            mPaint.setStrokeWidth((float) 0.25);              //线宽
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);            // 描边
            mPaint.setColor(getResources().getColor(R.color.shallow_black_color));//画笔颜色
            for (int i = 0; i < signalItemCount; i++) {
                //画背景
                mPaint.setStrokeWidth((float) 1);              //线宽
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);            // 描边
                mPaint.setColor(getResources().getColor(R.color.font_black_color));//画笔颜色
                canvas.drawLine(leftMargin, i * itemHeight, leftMargin, itemSignalHeight + i * itemHeight, mPaint);
                canvas.drawLine(leftMargin, itemSignalHeight + i * itemHeight,
                        itemSignalWidth, itemSignalHeight + i * itemHeight, mPaint);
                int YdrawLine = 0;
                for (int j = 0; j <= 255; j += YcoordInterval) {

                    mPaint.setStrokeWidth((float) 0.25);              //线宽
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);            // 描边
                    mPaint.setColor(getResources().getColor(R.color.line_gray_color));//画笔颜色
                    canvas.drawLine(leftMargin, itemSignalHeight + i * itemHeight - YactualInterval * YdrawLine,
                            itemSignalWidth, itemSignalHeight + i * itemHeight - YactualInterval * YdrawLine, mPaint);
                    mPaint.setStrokeWidth((float) 0.5);              //线宽
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);            // 描边
                    mPaint.setColor(getResources().getColor(R.color.font_black_color));//画笔颜色
                    canvas.drawText(Integer.toString(j), 3, itemSignalHeight + i * itemHeight - YactualInterval * YdrawLine, mPaint);
                    YdrawLine++;
                }
                int XdrawLine = 0;
                for (int j = 1; j <= maxCount; j += XcoordInterval) {
                    mPaint.setStrokeWidth((float) 0.25);              //线宽
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);            // 描边
                    mPaint.setColor(getResources().getColor(R.color.line_gray_color));//画笔颜色
                    canvas.drawLine(leftMargin + XactualInterval * XdrawLine, i * itemHeight + YactualInterval / 2,
                            leftMargin + XactualInterval * XdrawLine, itemSignalHeight + i * itemHeight, mPaint);
                    mPaint.setStrokeWidth((float) 0.5);              //线宽
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);            // 描边
                    mPaint.setColor(getResources().getColor(R.color.font_black_color));//画笔颜色
                    canvas.drawText(Integer.toString(j), leftMargin + XactualInterval * XdrawLine, (i + 1) * itemHeight, mPaint);
                    XdrawLine++;
                }
                if (signalDataList != null && signalDataList.size() > 0) {

                    mPaint.setStrokeWidth((float) 1);              //线宽
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);            // 描边
                    mPaint.setColor(Signal_fragment.signalColorS.getColor(signalDataList.get(i).colorNum, 0));//画笔颜色
                    canvas.drawText(signalDataList.get(i).itemInfo, itemSignalWidth + 10, (float) ((i + 1 / 2.0) * itemHeight), mPaint);

                    //画数据线
                    float coordY = i * itemHeight + itemSignalHeight;
                    float coordX = leftMargin;
                    float[] point1 = new float[2];
                    float[] point2 = new float[2];

                    for (int j = 0; j < signalDataList.get(i).count - 1 && j < signalDataList.get(i).datas.size(); j++) {
                        point1[0] = coordX + j * XeveryDataInterval;
                        point1[1] = coordY - signalDataList.get(i).datas.get(j) * YeveryDataInterval;
                        mPaint.setStrokeWidth((float) 3);
                        if (signalDataList.get(i).datas.get(j) < signalDataList.get(i).min ||
                                signalDataList.get(i).datas.get(j) > signalDataList.get(i).max) {
                            mPaint.setColor(getResources().getColor(R.color.errorSignalColors));
                        } else {
                            mPaint.setColor(getResources().getColor(R.color.point_color));
                        }
                        canvas.drawPoint(point1[0], point1[1], mPaint);

                        point2[0] = coordX + (j + 1) * XeveryDataInterval;
                        point2[1] = coordY - signalDataList.get(i).datas.get(j + 1) * YeveryDataInterval;
                        if (signalDataList.get(i).datas.get(j + 1) < signalDataList.get(i).min ||
                                signalDataList.get(i).datas.get(j + 1) > signalDataList.get(i).max) {
                            mPaint.setColor(getResources().getColor(R.color.errorSignalColors));
                        } else {
                            mPaint.setColor(getResources().getColor(R.color.point_color));
                        }
                        canvas.drawPoint(point2[0], point2[1], mPaint);

                        mPaint.setStrokeWidth((float) 0.5);
                        mPaint.setColor(Signal_fragment.signalColorS.getColor(signalDataList.get(i).colorNum, 0));
                        canvas.drawLine(point1[0], point1[1], point2[0], point2[1], mPaint);
                    }
                }
            }
            refreshInterface = false;
    }
    public void refreshCancasData(final List<SignalData> signalDataList){
            this.signalDataList.clear();
            for (int j = 0; j < signalDataList.size(); j++) {
                this.signalDataList.add(signalDataList.get(j));
            }
//            this.signalDataList = signalDataList;
            for (int i = 0; i < signalDataList.size(); i++) {
                maxCount = Math.max(maxCount, signalDataList.get(i).count);
            }
            signalItemCount = signalDataList.size();
            if (signalItemCount == 0)
                signalItemCount = 1;
            postInvalidate();
        }

}

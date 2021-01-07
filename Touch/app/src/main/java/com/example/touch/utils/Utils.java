package com.example.touch.utils;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Utils {
    public static void runInUi(Consumer consumer) {
        //Observable.just(1).observeOn(AndroidSchedulers.mainThread())
        //Observable.interval()
        Observable.just(1).observeOn(Schedulers.newThread())
                .subscribe(consumer);
    }
}

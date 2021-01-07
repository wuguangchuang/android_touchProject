//
// Created by wuguangchuang on 2020/11/24.
//
#include "com_example_touch_HidrawManager.h"
#include <linux/input.h>
#include <linux/hidraw.h>
#include <fcntl.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>
#include "HidrawManager.h"
#include <string.h>
#include <sys/stat.h>
#include <time.h>
#include <android/log.h>

#define TAG "myDemo-jni" // 这个是自定义的LOG的标识
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型


char *jByteArrayToChar();

jbyteArray charToJByteArray();

int initMyWork();

void addWorkHidraw(int fd, jint vid, jint pid);

void removeWorkHidraw();

JNIEXPORT jboolean JNICALL
Java_com_example_touch_HidrawManager_exist(JNIEnv *env, jclass clazz, jstring string){
    return 1;
}
JNIEXPORT int initMyWork() {
    myWorkHidraw = malloc(sizeof(struct WorkHidraw));
    myWorkHidraw->next = NULL;
    return 0;
}

//jint JNICALL Java_com_example_touch_HidrawManager_openHidraw
//        (JNIEnv *env, jclass clazz, jstring file_path, jint vid , jint pid){
//    char *fileName = (*env)->GetStringUTFChars(env,file_path,0);
//    fd = open(fileName,O_RDWR|O_NONBLOCK);
//    if(fd < 0)
//    {
//        return -1;
//    }
//
//    struct hidraw_devinfo raw_info;
//    struct hidraw_report_descriptor rep_des;
//    int desc_size = 0;
//
//    if(-1 == ioctl(fd, HIDIOCGRAWINFO, &raw_info)){
//        close(fd);
//        return -2;
//    }
//
//    if(raw_info.vendor != vid || raw_info.product != pid)
//    {
//        close(fd);
//        return -3;
//    }
////    if(myWorkHidraw == NULL)
////    {
////        int ret = initMyWork();
////        addWorkHidraw(fd,vid,pid);
////    }
//    return 1;
//}

void addWorkHidraw(int fd, jint vid, jint pid) {
    struct WorkHidraw *node = NULL;
    node = malloc(sizeof(struct WorkHidraw));
    node->next = NULL;
    node->fd = fd;
    node->vid = vid;
    node->pid = pid;
    struct WorkHidraw *p = myWorkHidraw;
    while (p->next != NULL)
    {
        p = p->next;
    }
    p->next = node;
}

JNIEXPORT jint JNICALL
Java_com_example_touch_HidrawManager_openHidraw(JNIEnv *env, jclass clazz, jstring file_path) {
    // TODO: implement openHidraw()

    char *fileName = (*env)->GetStringUTFChars(env,file_path,0);
    fd = open(fileName,O_RDWR|O_NONBLOCK );
    if(fd < 0)
    {
        return -1;
    }

    struct hidraw_devinfo raw_info;
    struct hidraw_report_descriptor rep_des;
    int desc_size = 0;

    if(-1 == ioctl(fd, HIDIOCGRAWINFO, &raw_info)){
        return -2;
    }

    if(ioctl(fd, HIDIOCGRDESCSIZE, &desc_size) < 0) {
        return -3;
    }

    int vid = raw_info.vendor & 0xffff;
    int pid = raw_info.product & 0xffff;

    if(vid == 0xAED7 && pid == 0x0013)
    {
        return 0;
    }
    else if(vid == 0x14E1 && pid == 0x3500)
    {
        return 0;
    }
    else if(vid == 0x14E1 && pid == 0x3400)
    {
        return 0;
    }
    else if(vid == 0x14E1 && pid == 0x2500)
    {
        return 0;
    }
    else if(vid == 0x1FF7 && pid == 0x0013)
    {
        return 0;
    }
    else if(vid == 0xAED7 && pid == 0xFEDC)
    {
        return 1;
    }
    else if(vid == 0x24B8 && pid == 0x0040)
    {
        return 0;
    }
    else if(vid == 0x1101 && pid == 0x0010)
    {
        return 0;
    }
    else if(vid == 0x1FF7 && pid == 0x001D)
    {
        return 0;
    }
    else
    {
        close(fd);
        fd = -1;
        return -4;
    }
}

JNIEXPORT char *jByteArrayToChar(JNIEnv *env, jbyteArray buf) {
    char *chars = NULL;
    jbyte *bytes;
    bytes = (*env)->GetByteArrayElements(env,buf, 0);
    int chars_len = (*env)->GetArrayLength(env,buf);
    chars = malloc(chars_len + 1);
    memset(chars, 0, chars_len + 1);
    memcpy(chars, bytes, chars_len);
    chars[chars_len] = 0;
    (*env)->ReleaseByteArrayElements(env,buf, bytes, 0);
    return chars;
}

jint JNICALL
Java_com_example_touch_HidrawManager_writeToHidraw(JNIEnv *env, jclass clazz,
                                                   jbyteArray send_data) {
    // TODO: implement writeToHidraw()
    int ret = -1;
    char *buf = jByteArrayToChar(env,send_data);
    ret = write(fd,buf,(*env)->GetArrayLength(env,send_data));
    return ret;
}

JNIEXPORT jbyteArray charToJByteArray(JNIEnv *env, unsigned char *buf, int len) {
    jbyteArray array = (*env)->NewByteArray(env,len);
    (*env)->SetByteArrayRegion(env,array, 0, len, buf);
    return array;
}


jstring charToJString(JNIEnv *env, char *pat) {
    jclass strClass = (*env)->FindClass(env,"java/lang/String");
    jmethodID ctorID = (*env)->GetMethodID(env,strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = (*env)->NewByteArray(env,strlen(pat));
    (*env)->SetByteArrayRegion(env,bytes, 0, strlen(pat), (jbyte *) pat);
    jstring encoding = (*env)->NewStringUTF(env,"utf-8");
    return (jstring) (*env)->NewObject(strClass, ctorID, bytes, encoding);

}


JNIEXPORT jint JNICALL
Java_com_example_touch_HidrawManager_closeFd(JNIEnv *env, jclass clazz, jint vid, jint pid) {
    // TODO: implement closeFd()
    close(fd);
//    struct WorkHidraw *p = myWorkHidraw->next;
//    if(p == NULL)
//        return 0;
//    while (p != NULL)
//    {
//        if(p->vid == vid && p->pid == pid)
//        {
//            close(p->fd);
//            return 0;
//        }
//        p = p->next;
//    }
}

JNIEXPORT jint JNICALL
Java_com_example_touch_HidrawManager_removeHidRaw(JNIEnv *env, jclass clazz, jint vid, jint pid) {
    // TODO: implement removeHidRaw()
    struct WorkHidraw *p = myWorkHidraw->next;
    if(p == NULL)
        return 0;
    struct WorkHidraw *q = myWorkHidraw;
    while (p != NULL)
    {
        if(p->vid == vid && p->pid == pid)
        {
            close(p->fd);
            q->next = p->next;
            free(p);
            return 0;
        }
        q->next = p;
        p = p->next;
    }
}
jbyteArray read_data;
JNIEXPORT jbyteArray JNICALL
Java_com_example_touch_HidrawManager_readForHidraw(JNIEnv *env, jclass clazz) {
    // TODO: implement readForHidraw()
    int ret = -1;
    char buf[256];

    ret = read(fd,buf, sizeof(buf));
    if(ret > 0)
        read_data = charToJByteArray(env,buf,ret);
    else
        read_data = NULL;
    return read_data;
}

JNIEXPORT jbyteArray JNICALL
Java_com_example_touch_HidrawManager_readWrite(JNIEnv *env, jclass clazz, jbyteArray send_data) {
    // TODO: implement readWrite()
    LOGD("开始发送命令");
    char buf[256];
    int ret = -1;
    reading = true;
    time_t t_start,t_end;
    t_start = time(NULL);
    read_data = NULL;
    ret = Java_com_example_touch_HidrawManager_writeToHidraw(env,clazz,send_data);
    if(ret < 0)
    {
        LOGD("发送命令失败");
    }
    int retRead = -1;
    while (reading && ret >= 0) {
        retRead = read(fd,buf, sizeof(buf));
        if (retRead < 0) {
            t_end = time(NULL);
            if (difftime(t_start, t_end) > 20000) {
                LOGE("等待数据超时");
                return NULL;
            }
            continue;
        } else{
            if(buf[0] == 205)
            {
                read_data = charToJByteArray(env,buf,ret);
                return read_data;
            }
            continue;
        }
    }

    return NULL;
}
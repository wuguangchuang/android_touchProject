//
// Created by wuguangchuang on 2020/11/24.
//

#ifndef TOUCH_HIDRAWMANAGER_H
#define TOUCH_HIDRAWMANAGER_H


struct WorkHidraw{
    int fd;
    int vid;
    int pid;
    struct WorkHidraw *next;
};

int fd = -1;
bool reading = true;
struct WorkHidraw *myWorkHidraw ;
#endif //TOUCH_HIDRAWMANAGER_H

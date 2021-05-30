@[TOC]


<br>
<br>
<br>
<br>

# 一、 双进程守护保活 + JobScheduler 原理

---

<br>

[【Android 进程保活】应用进程拉活 ( JobScheduler 拉活 | JobScheduler 使用流程 | JobService 服务 | 不同版本兼容 | 源码资源 )](https://hanshuliang.blog.csdn.net/article/details/115584240) 博客中介绍了 JobScheduler 的用法 ; 

[【Android 进程保活】应用进程拉活 ( 双进程守护保活 )](https://hanshuliang.blog.csdn.net/article/details/115604667) 博客中介绍了双进程守护保活用法 ; 

**使用 " 双进程守护保活 + JobScheduler " 机制 , 成功率最高 ;** 

<br>

**" 双进程守护保活 + JobScheduler " 整合方法 :** 

在 JobService 的 onStartJob 方法中 , 判定 " 双进程守护保活 " 中的双进程是否挂了 , 如果这两个进程挂了 , 就重新将挂掉的进程重启 ; 

**判定 Service 进程是否运行 :** 

```java
    public static boolean isServiceRunning(Context context, String serviceName){
        if(TextUtils.isEmpty(serviceName)) return false;

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        // 最多获取 200 个正在运行的 Service
        List<ActivityManager.RunningServiceInfo> infos =
                activityManager.getRunningServices(200);

        // 遍历当前运行的 Service 信息, 如果找到相同名称的服务 , 说明某进程正在运行
        for (ActivityManager.RunningServiceInfo info: infos){
            if (TextUtils.equals(info.service.getClassName(), serviceName)){
                return true;
            }
        }
        
        return false;
    }
```


<br>
<br>
<br>
<br>

# 二、 双进程守护保活 + JobScheduler 源码 

---

<br>

大部分代码与 [【Android 进程保活】应用进程拉活 ( 双进程守护保活 )](https://hanshuliang.blog.csdn.net/article/details/115604667) 博客中重复 , 这里只贴出 JobScheduler 相关源码 ; 

<br>
<br>

## 1、JobService 代码 

<br>

```java
package kim.hsl.two_progress_alive;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class KeepAliveJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i("KeepAliveJobService", "JobService onStartJob 开启");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            // 如果当前设备大于 7.0 , 延迟 5 秒 , 再次执行一次
            startJob(this);
        }

        // 判定本地前台进程是否正在运行
        boolean isLocalServiceRunning =
                ServiceUtils.isServiceRunning(this, LocalForegroundService.class.getName());
        if (!isLocalServiceRunning){
            startService(new Intent(this, LocalForegroundService.class));
        }

        // 判定远程前台进程是否正在运行
        boolean isRemoteServiceRunning =
                ServiceUtils.isServiceRunning(this, RemoteForegroundService.class.getName());
        if (!isRemoteServiceRunning){
            startService(new Intent(this, RemoteForegroundService.class));
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i("KeepAliveJobService", "JobService onStopJob 关闭");
        return false;
    }

    public static void startJob(Context context){
        // 创建 JobScheduler
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // 第一个参数指定任务 ID
        // 第二个参数指定任务在哪个组件中执行
        // setPersisted 方法需要 android.permission.RECEIVE_BOOT_COMPLETED 权限
        // setPersisted 方法作用是设备重启后 , 依然执行 JobScheduler 定时任务
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(10,
                new ComponentName(context.getPackageName(), KeepAliveJobService.class.getName()))
                .setPersisted(true);

        // 7.0 以下的版本, 可以每隔 5000 毫秒执行一次任务
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
            jobInfoBuilder.setPeriodic(5_000);

        }else{
            // 7.0 以上的版本 , 设置延迟 5 秒执行
            // 该时间不能小于 JobInfo.getMinLatencyMillis 方法获取的最小值
            jobInfoBuilder.setMinimumLatency(5_000);
        }

        // 开启定时任务
        jobScheduler.schedule(jobInfoBuilder.build());

    }
}

```

<br>
<br>

## 2、判定服务运行工具类

<br>

```java
package kim.hsl.two_progress_alive;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import org.w3c.dom.Text;

import java.util.List;

public class ServiceUtils {
    /**
     * 判定 Service 是否在运行
     * @param context
     * @return
     */
    public static boolean isServiceRunning(Context context, String serviceName){
        if(TextUtils.isEmpty(serviceName)) return false;

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        // 最多获取 200 个正在运行的 Service
        List<ActivityManager.RunningServiceInfo> infos =
                activityManager.getRunningServices(200);

        // 遍历当前运行的 Service 信息, 如果找到相同名称的服务 , 说明某进程正在运行
        for (ActivityManager.RunningServiceInfo info: infos){
            if (TextUtils.equals(info.service.getClassName(), serviceName)){
                return true;
            }
        }

        return false;
    }
}

```

<br>
<br>

## 3、清单文件

<br>

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="kim.hsl.two_progress_alive">

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Two_Progress_Alive">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- 本地提权前台服务 Service -->
        <service
            android:name=".LocalForegroundService"
            android:enabled="true"
            android:exported="true"></service>

        <!-- 本地服务 , API 18 ~ 25 以上的设备, 关闭通知到专用服务 -->
        <service
            android:name=".LocalForegroundService$CancelNotificationService"
            android:enabled="true"
            android:exported="true"></service>

        <!-- 远程提权前台服务 Service -->
        <service
            android:name=".RemoteForegroundService"
            android:enabled="true"
            android:exported="true"
            android:process=":remote"></service>

        <!-- 远程服务 , API 18 ~ 25 以上的设备, 关闭通知到专用服务 -->
        <service
            android:name=".RemoteForegroundService$CancelNotificationService"
            android:enabled="true"
            android:exported="true"
            android:process=":remote"></service>

        <!-- JobScheduler 拉活 -->
        <service
            android:name=".KeepAliveJobService"
            android:enabled="true"
            android:exported="true"
            android:permission="android.permission.BIND_JOB_SERVICE"></service>

    </application>

</manifest>
```

<br>
<br>

## 4、MainActivity 代码

<br>

```java
package kim.hsl.two_progress_alive;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 通过前台 Service 提升应用权限
        // 启动普通 Service , 但是在该 Service 的 onCreate 方法中执行了 startForeground
        // 变成了前台 Service 服务
        startService(new Intent(this, LocalForegroundService.class));
        startService(new Intent(this, RemoteForegroundService.class));

        // JobScheduler 拉活
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeepAliveJobService.startJob(this);
        }
    }
}
```

<br>
<br>

## 5、运行效果 

<br>

运行后 , 两个进程成功运行 ; 

即使将启动双进程的代码注释掉 , 也可以成功拉起双进程 ; 

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210411223844928.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2hhbjEyMDIwMTI=,size_16,color_FFFFFF,t_70)




<br>
<br>
<br>
<br>

# 三、 源码资源 

---

<br>


**源码资源 :** 

 - **GitHub 地址 :** [https://github.com/han1202012/Two_Process_Alive](https://github.com/han1202012/Two_Process_Alive)
 - **CSDN 源码快照 :** [https://download.csdn.net/download/han1202012/16623594](https://download.csdn.net/download/han1202012/16623594)

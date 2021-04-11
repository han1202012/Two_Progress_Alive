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

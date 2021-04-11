package kim.hsl.two_progress_alive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.core.app.NotificationCompat;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

/**
 * 前台服务提权
 */
public class LocalForegroundService extends Service {

    /**
     * 远程调用 Binder 对象
     */
    private MyBinder myBinder;

    /**
     * 连接对象
     */
    private Connection connection;

    /**
     * AIDL 远程调用接口
     * 其它进程调与该 RemoteForegroundService 服务进程通信时 , 可以通过 onBind 方法获取该 myBinder 成员
     * 通过调用该成员的 basicTypes 方法 , 可以与该进程进行数据传递
     */
    class MyBinder extends IMyAidlInterface.Stub {
        @Override
        public void basicTypes(
                int anInt, long aLong, boolean aBoolean, float aFloat,
                double aDouble, String aString) throws RemoteException {
            // 通信内容
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 创建 Binder 对象
        myBinder = new MyBinder();

        // 启动前台进程
        startService();
    }

    private void startService(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            // startForeground();

            // 创建通知通道
            NotificationChannel channel = new NotificationChannel("service",
                    "service", NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // 正式创建
            service.createNotificationChannel(channel);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "service");
            Notification notification = builder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(PRIORITY_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            // 开启前台进程 , API 26 以上无法关闭通知栏
            startForeground(10, notification);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            startForeground(10, new Notification());
            // API 18 ~ 25 以上的设备 , 启动相同 id 的前台服务 , 并关闭 , 可以关闭通知
            startService(new Intent(this, CancelNotificationService.class));

        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
            // 将该服务转为前台服务
            // 需要设置 ID 和 通知
            // 设置 ID 为 0 , 就不显示已通知了 , 但是 oom_adj 值会变成后台进程 11
            // 设置 ID 为 1 , 会在通知栏显示该前台服务
            // 8.0 以上该用法报错
            startForeground(10, new Notification());
        }
    }

    /**
     * 绑定 另外一个 服务
     * LocalForegroundService 与 RemoteForegroundService 两个服务互相绑定
     */
    private void bindService(){
        // 绑定 另外一个 服务
        // LocalForegroundService 与 RemoteForegroundService 两个服务互相绑定

        // 创建连接对象
        connection = new Connection();

        // 创建本地前台进程组件意图
        Intent bindIntent = new Intent(this, RemoteForegroundService.class);
        // 绑定进程操作
        bindService(bindIntent, connection, BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 绑定另外一个服务
        bindService();
        return super.onStartCommand(intent, flags, startId);
    }

    class Connection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 服务绑定成功时回调
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 再次启动前台进程
            startService();
            // 绑定另外一个远程进程
            bindService();
        }
    }

    /**
     * API 18 ~ 25 以上的设备, 关闭通知到专用服务
     */
    public static class CancelNotificationService extends Service {
        public CancelNotificationService() {
        }

        @Override
        public void onCreate() {
            super.onCreate();
            startForeground(10, new Notification());
            stopSelf();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

    }

}
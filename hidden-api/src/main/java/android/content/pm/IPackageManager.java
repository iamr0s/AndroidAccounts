package android.content.pm;

import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.RequiresApi;

public interface IPackageManager extends IInterface {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    PackageInfo getPackageInfo(String packageName, long flags, int userId);

    @DeprecatedSinceApi(api = Build.VERSION_CODES.TIRAMISU)
    PackageInfo getPackageInfo(String packageName, int flags, int userId);

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

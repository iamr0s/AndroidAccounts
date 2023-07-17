package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IPackageManager extends IInterface {
    PackageInfo getPackageInfo(String packageName, long flags, int userId);

    PackageInfo getPackageInfo(String packageName, int flags, int userId);

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

package android.os;

import android.content.pm.UserInfo;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.RequiresApi;

import java.util.List;

public interface IUserManager extends IInterface {
    @RequiresApi(Build.VERSION_CODES.R)
    List<UserInfo> getUsers(boolean excludePartial, boolean excludeDying, boolean excludePreCreated);

    @DeprecatedSinceApi(api = Build.VERSION_CODES.R)
    List<UserInfo> getUsers(boolean excludeDying);

    boolean removeUser(int userHandle);

    abstract class Stub extends Binder implements IUserManager {
        public static IUserManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

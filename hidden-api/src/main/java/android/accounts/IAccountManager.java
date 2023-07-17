package android.accounts;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IAccountManager extends IInterface {
    AuthenticatorDescription[] getAuthenticatorTypes(int userId);

    abstract class Stub extends Binder implements IAccountManager {
        public static IAccountManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

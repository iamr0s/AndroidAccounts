package android.os;

public interface IUserManager extends IInterface {
    boolean removeUser(int userHandle);

    abstract class Stub extends Binder implements IUserManager {
        public static IUserManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}

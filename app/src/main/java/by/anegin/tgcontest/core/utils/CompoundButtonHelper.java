package by.anegin.tgcontest.core.utils;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.CompoundButton;

import java.lang.reflect.Field;

public class CompoundButtonHelper {

    private static final String TAG = CompoundButtonHelper.class.getSimpleName();

    private static Field sButtonDrawableField;
    private static boolean sButtonDrawableFieldFetched;

    public static Drawable getButtonDrawable(CompoundButton compoundButton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return compoundButton.getButtonDrawable();
        } else {
            return getButtonDrawableGingerbread(compoundButton);
        }
    }

    /**
     * Based on android.support.v4.widget.CompoundButtonCompatGingerbread sources
     */
    private static Drawable getButtonDrawableGingerbread(CompoundButton compoundButton) {
        if (!sButtonDrawableFieldFetched) {
            try {
                //noinspection JavaReflectionMemberAccess
                sButtonDrawableField = CompoundButton.class.getDeclaredField("mButtonDrawable");
                sButtonDrawableField.setAccessible(true);
            } catch (Throwable e) {
                Log.i(TAG, "Failed to retrieve mButtonDrawable field", e);
            }
            sButtonDrawableFieldFetched = true;
        }
        if (sButtonDrawableField != null) {
            try {
                return (Drawable) sButtonDrawableField.get(compoundButton);
            } catch (Throwable e2) {
                Log.i(TAG, "Failed to get button drawable via reflection", e2);
                sButtonDrawableField = null;
            }
        }
        return null;
    }

}

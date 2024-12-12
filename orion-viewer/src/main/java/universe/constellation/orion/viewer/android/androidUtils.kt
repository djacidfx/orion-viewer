package universe.constellation.orion.viewer.android;

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build

fun isAtJellyBean() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN

fun isAtLeastKitkat() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

fun isAtLeastLollipop() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

fun isAtLeastAndroidM() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

fun isAtLeastAndroidN() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

fun isAtLeastAndroidR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

fun isAtLeastTiramisu() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun Intent.isContentScheme(): Boolean {
    return ContentResolver.SCHEME_CONTENT == scheme
}

val Uri.isContentUri: Boolean
    get() {
        return ContentResolver.SCHEME_CONTENT == scheme
    }

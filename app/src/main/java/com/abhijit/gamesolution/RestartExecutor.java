package com.abhijit.gamesolution;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import java.io.DataOutputStream;

public final class RestartExecutor {
    private RestartExecutor() {}

    /**
     * On a normal Android device, an ordinary app cannot silently force-stop another app.
     * If root is available, we use am force-stop and then launch the target.
     * Otherwise we fall back to relaunching the target's launcher activity.
     */
    public static boolean restart(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;

        boolean forceStopped = tryRootForceStop(packageName);
        try {
            PackageManager pm = context.getPackageManager();
            Intent launch = pm.getLaunchIntentForPackage(packageName);
            if (launch == null) return false;
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launch);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean tryRootForceStop(String packageName) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su"});
            DataOutputStream output = new DataOutputStream(process.getOutputStream());
            output.writeBytes("am force-stop " + packageName + "\n");
            output.writeBytes("exit\n");
            output.flush();
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}

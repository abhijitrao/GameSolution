package com.abhijit.gamesolution;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

public final class ApkShareProvider extends ContentProvider {
    private static final String ROOT = "shared_apk";

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        return new AssetFileDescriptor(openFile(uri, mode), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = resolve(uri);
            String[] cols = projection == null ? new String[]{"_display_name", "_size"} : projection;
            MatrixCursor cursor = new MatrixCursor(cols, 1);
            Object[] row = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                if ("_display_name".equals(cols[i])) row[i] = file.getName();
                else if ("_size".equals(cols[i])) row[i] = file.length();
            }
            cursor.addRow(row);
            return cursor;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }

    public static Uri getUri(Context context, File file) {
        if (context == null) throw new IllegalArgumentException("Context must not be null");
        if (file == null) throw new IllegalArgumentException("File must not be null");
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".apkshare")
                .appendPath(ROOT)
                .appendPath(file.getName())
                .build();
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        String path = uri.getPath();
        if (path == null || !path.startsWith("/" + ROOT + "/")) throw new FileNotFoundException("Invalid APK path");
        String name = path.substring((ROOT + "/").length() + 1);
        if (name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) throw new FileNotFoundException("Invalid APK file");

        Context context = getContext();
        if (context == null) throw new IllegalStateException("Provider not attached");

        File root = new File(context.getCacheDir(), ROOT);
        File file = new File(root, name);
        if (!file.isFile()) throw new FileNotFoundException("APK not found");
        return file;
    }
}

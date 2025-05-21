/*
 *  Copyright (c) 2015 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Dennis Lang  (Dec-2015)
 *  @see <a href="https://LanDenLabs.com">https://LanDenLabs.com</a>
 *
 */

package com.landenlabs.all_encrypnotes.code;

/*
 * (c) 2009.-2010. Ivan Voras <ivoras@fer.hr>
 * Released under the 2-clause BSDL.
 */

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.landenlabs.all_encrypnotes.ui.LogIt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Original version by ivoras
 *
 * @author ivoras
 * <p>
 * <p>
 * Updated and rewritten by Dennis Lang 2015/2016
 * @author Dennis Lang
 * @see <a href="https://LanDenLabs.com">https://LanDenLabs.com</a>
 */
public class Util {

    /**
     * @return Current date.
     */
    public static Date getCurrentDate() {
        return new Date(System.currentTimeMillis());
    }

    /**
     * @return {@code true} if file exists.
     */
    @SuppressWarnings("SameParameterValue")
    public  static boolean fileExists(String filename) {
        return new File(filename).exists();
    }


    /**
     * Returns a binary MD5 hash of the given string.
     */
    @NonNull
    @SuppressWarnings("unused")
    public static byte[] md5hash(@NonNull String s) {
        return hash(s, "MD5");
    }

    /**
     * Returns a binary MD5 hash of the given binary buffer.
     */
    @NonNull
    @SuppressWarnings("unused")
    public static byte[] md5hash(@NonNull byte[] buf) {
        return hash(buf, "MD5");
    }

    /**
     * Returns a binary SHA1 hash of the given string.
     */
    @NonNull
    public static byte[] sha1hash(@NonNull String s) {
        return hash(s, "SHA1");
    }


    /**
     * Returns a binary SHA1 hash of the given buffer.
     */
    @NonNull
    public static byte[] sha1hash(@NonNull byte[] buf) {
        return hash(buf, "SHA1");
    }


    /**
     * Returns a binary hash calculated with the specified algorithm of the
     * given string.
     */
    @NonNull
    private static byte[] hash(@NonNull String s, @NonNull String hashAlg) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        return hash(b, hashAlg);
    }

    /**
     * Converts a binary buffer to a string of lowercase hexadecimal characters.
     */
    @SuppressWarnings("unused")
    public static String bytea2hex(@NonNull byte[] h) {
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    /**
     * Returns a binary hash calculated with the specified algorithm of the
     * given input buffer.
     */
    @NonNull
    private static byte[] hash(@NonNull byte[] buf, @NonNull String hashAlg) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(hashAlg);
        } catch (NoSuchAlgorithmException ex) {
            LogIt.log(Util.class, LogIt.ERROR, null, ex);
            System.exit(1);
        }
        return (md != null) ? md.digest(buf) : new byte[]{0};
    }

    /**
     * Concatenates two byte arrays and returns the result.
     */
    public static byte[] concat(byte[] src1, byte[] src2) {
        byte[] dst = new byte[src1.length + src2.length];
        System.arraycopy(src1, 0, dst, 0, src1.length);
        System.arraycopy(src2, 0, dst, src1.length, src2.length);
        return dst;
    }

    public static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private static final String ERROR_REPORT_FORMAT = "yyyy.MM.dd HH:mm:ss z";
        @SuppressLint("SimpleDateFormat")
        private SimpleDateFormat format = new SimpleDateFormat(ERROR_REPORT_FORMAT);

        private final Thread.UncaughtExceptionHandler originalHandler;

        /**
         * Creates a reporter instance
         *
         * @throws NullPointerException if the parameter is null
         */
        public UncaughtExceptionHandler() throws NullPointerException {
            originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {

            String stackTrace = Log.getStackTraceString(ex);
            Log.d("UncaughtException", stackTrace);
            Log.e("UncaughtException", ex.getLocalizedMessage(), ex);

            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, ex);
            }
        }

    }
}

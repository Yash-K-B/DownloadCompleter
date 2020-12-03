package com.yash.download.completer.converter;

import java.text.DecimalFormat;

public class ConverterUtil {
    public static String bytesToHighest(long bytes) {
        DecimalFormat df = new DecimalFormat("0.0");
        if (bytes >= 1073741824) {          //GB
            return df.format((double) bytes / 1073741824) + "GB";
        } else if (bytes >= 1048576) {      //MB
            return df.format((double) bytes / 1048576) + "MB";
        } else if (bytes >= 1024) {         //KB
            return df.format((double) bytes / 1024) + "KB";
        } else {                            //B
            return bytes + "B";
        }
    }
}

package com.duytry.smarttraffic.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Common {
    public static final String PREFERENCES = "UserInformationPreferences";
    public static final String NAME_PREFERENCES_KEY = "Name";
    public static final String SHORT_NAME_ROAD_PREFERENCES_KEY = "Short_Name_Road";
    public static final String LONG_NAME_ROAD_PREFERENCES_KEY = "Long_Name_Road";
    public static final String UNDEFINED = "UNDEFINED";

    public static final String SHOCK_POINT_ACTION = "shockPoint";
    public static final String SPEED_UP_ACTION = "speedUp";
    public static final String BRAKE_DOWN_ACTION = "brakeDown";
    public static final String PARKING_ACTION = "parking";
    public static final String AUTO_ACTION = "auto";

    public static final String UNDERLINED_CHARACTER = "_";
    public static final String SLASH_CHARACTER = "/";
    public static final String DASH_CHARACTER = "-";
    public static final String SPACE_CHARACTER = " ";
    public static final String SEMICOLON_CHARACTER = ";";
    public static final String FILENAME_DIRECTORY = "accelerometer_data";

    public static final String FILENAME_EXTENSION = ".txt";

    public static final String ERROR_MESSAGE = "Error!!!";
    public static final String OPEN_FILE_ERROR_MESSAGE = "Unexpected error when open file!";

    public static final String DATE_INPUT_FORMAT = "yyyyMMddhhmmss";
    public static final String DATE_OUTPUT_FORMAT = "hh:mm:ss dd/MM/yyyy";

    public static final int VIEW_DATA_RESULT_CODE = 2001;
    public static final int VIEW_DATA_REQUEST_CODE = 2000;

    public static final int CONNECTION_TIME_OUT = 15000;

    public static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}

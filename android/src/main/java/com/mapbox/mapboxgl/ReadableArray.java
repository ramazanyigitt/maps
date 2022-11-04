package com.mapbox.mapboxgl;

import androidx.annotation.NonNull;
import java.util.ArrayList;

/**
 * Interface for an array that allows typed access to its members. Used to pass parameters from JS
 * to Java.
 */
public interface ReadableArray {

    int size();

    boolean isNull(int index);

    boolean getBoolean(int index);

    double getDouble(int index);

    int getInt(int index);

    @NonNull
    String getString(int index);

    @NonNull
    ReadableArray getArray(int index);

    @NonNull
    ReadableMap getMap(int index);

    @NonNull
    Dynamic getDynamic(int index);

    @NonNull
    ReadableType getType(int index);

    @NonNull
    ArrayList<Object> toArrayList();
}
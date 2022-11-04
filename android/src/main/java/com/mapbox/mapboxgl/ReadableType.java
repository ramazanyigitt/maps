package com.mapbox.mapboxgl;
/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

/** Defines the type of an object stored in a {@link ReadableArray} or {@link ReadableMap}. */
public enum ReadableType {
    Null,
    Boolean,
    Number,
    String,
    Map,
    Array,
}
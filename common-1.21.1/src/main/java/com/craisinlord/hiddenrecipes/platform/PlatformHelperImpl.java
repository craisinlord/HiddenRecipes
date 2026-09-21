package com.craisinlord.hiddenrecipes.platform;

import java.util.ServiceLoader;

final class PlatformHelperImpl {
    static final PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No PlatformHelper implementation found! Ensure your loader module provides one."));

    private PlatformHelperImpl() {
    }
}

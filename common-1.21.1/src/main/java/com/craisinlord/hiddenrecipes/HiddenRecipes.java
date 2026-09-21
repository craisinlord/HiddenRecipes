package com.craisinlord.hiddenrecipes;

import com.craisinlord.hiddenrecipes.platform.PlatformHelper;

public final class HiddenRecipes {

    public static void init() {
        Constants.LOGGER.info("{} {} initializing on {}", Constants.MOD_NAME,
            PlatformHelper.getInstance().getModVersion(),
            PlatformHelper.getInstance().getPlatformName());
    }

    private HiddenRecipes() {
    }
}

package traben.stuck_arrows_for_all.fabric;

import traben.stuck_arrows_for_all.Stuck_arrows_for_all;
import net.fabricmc.api.ModInitializer;

public final class Stuck_arrows_for_allFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Stuck_arrows_for_all.init();
    }
}

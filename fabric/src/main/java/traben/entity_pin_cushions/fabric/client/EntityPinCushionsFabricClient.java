package traben.entity_pin_cushions.fabric.client;

import net.fabricmc.api.ClientModInitializer;

public final class EntityPinCushionsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        traben.entity_pin_cushions.EntityPinCushions.init();
    }
}

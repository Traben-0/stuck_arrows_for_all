package traben.entity_pin_cushions.neoforge;

import traben.entity_pin_cushions.EntityPinCushions;
import net.neoforged.fml.common.Mod;

@Mod(EntityPinCushions.MOD_ID)
public final class EntityPinCushionsNeoForge {
    public EntityPinCushionsNeoForge() {
        // Run our common setup.
        traben.entity_pin_cushions.EntityPinCushions.init();
    }
}

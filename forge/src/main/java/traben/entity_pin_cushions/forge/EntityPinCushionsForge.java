package traben.entity_pin_cushions.forge;

import net.minecraftforge.fml.common.Mod;
import traben.entity_pin_cushions.EntityPinCushions;


@Mod(traben.entity_pin_cushions.EntityPinCushions.MOD_ID)
public final class EntityPinCushionsForge {
    public EntityPinCushionsForge() {
        // Run our common setup.
        EntityPinCushions.init();
    }
}

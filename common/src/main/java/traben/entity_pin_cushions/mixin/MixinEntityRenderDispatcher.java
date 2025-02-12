package traben.entity_pin_cushions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_pin_cushions.EntityPinCushions;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At("HEAD"))
    private <E extends Entity, S extends EntityRenderState> void renderMixin(final E entity, final double xOffset, final double yOffset, final double zOffset, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final EntityRenderer<? super E, S> renderer, final CallbackInfo ci) {
        EntityPinCushions.PINCUSHION_ID = entity.getId();
        if (entity instanceof LivingEntity alive){
            EntityPinCushions.PINCUSHION_COUNT_ARROW = alive.getArrowCount();
            EntityPinCushions.PINCUSHION_COUNT_STINGER = alive.getStingerCount();
        }else {
            EntityPinCushions.PINCUSHION_COUNT_ARROW = 0;
            EntityPinCushions.PINCUSHION_COUNT_STINGER = 0;

        }
    }
}

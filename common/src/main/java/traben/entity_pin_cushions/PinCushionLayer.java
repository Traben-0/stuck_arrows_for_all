package traben.entity_pin_cushions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArrowModel;
import net.minecraft.client.model.BeeStingerModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class PinCushionLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends RenderLayer<T, M> {

    protected StuckInBodyLayer.PlacementStyle placementStyle;
    private final Model model;
    private final ResourceLocation texture;

    public PinCushionLayer(final RenderLayerParent<T, M> renderer, final Model model, final ResourceLocation texture, final StuckInBodyLayer.PlacementStyle placementStyle) {
        super(renderer);
        this.model = model;
        this.texture = texture;
        this.placementStyle = placementStyle;
    }

    protected abstract int numStuck();

    private void renderStuckItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float x, float y, float z) {
        float f = Mth.sqrt(x * x + z * z);
        float g = (float)(Math.atan2((double)x, (double)z) * (double)(180F / (float)Math.PI));
        float h = (float)(Math.atan2((double)y, (double)f) * (double)(180F / (float)Math.PI));
        poseStack.mulPose(Axis.YP.rotationDegrees(g - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(h));
        this.model.renderToBuffer(poseStack, bufferSource.getBuffer(this.model.renderType(this.texture)), packedLight, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public void render(final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final T renderState, final float yRot, final float xRot) {
        int i = this.numStuck();
        RandomSource randomSource = RandomSource.create(EntityPinCushions.PINCUSHION_ID);
        if (i > 0) {
            for (int j = 0; j < i; ++j) {
                Random partRand = new Random(j);
                poseStack.pushPose();
                M model = getParentModel();

                Pair<ModelPart,Runnable> modelPart = bestFromListMutable(new ArrayList<>(model.root().children.values()), partRand, poseStack, true);

                if (modelPart == null) {
                    poseStack.popPose();
                    return;
                }

                modelPart.getSecond().run();//transforms

                float f = randomSource.nextFloat();
                float g = randomSource.nextFloat();
                float h = randomSource.nextFloat();

                if (this.placementStyle == StuckInBodyLayer.PlacementStyle.ON_SURFACE) {
                    int n = randomSource.nextInt(3);
                    switch (n) {
                        case 0 -> f = snapToFace(f);
                        case 1 -> g = snapToFace(g);
                        default -> h = snapToFace(h);
                    }
                }

                if (!modelPart.getFirst().cubes.isEmpty()) {
                    ModelPart.Cube cube = modelPart.getFirst().getRandomCube(randomSource);
                    float k = Mth.lerp(f, cube.minX, cube.maxX) / 16.0F;
                    float l = Mth.lerp(g, cube.minY, cube.maxY) / 16.0F;
                    float m = Mth.lerp(h, cube.minZ, cube.maxZ) / 16.0F;
                    poseStack.translate(k, l, m);
                }

                f = -1.0F * (f * 2.0F - 1.0F);
                g = -1.0F * (g * 2.0F - 1.0F);
                h = -1.0F * (h * 2.0F - 1.0F);
                this.renderStuckItem(poseStack, bufferSource, packedLight, f, g, h);
                poseStack.popPose();
            }

        }
    }

    private static float snapToFace(float value) {
        return value > 0.5F ? 1.0F : 0.5F;
    }

//    @Nullable
//    private Pair<ModelPart,Runnable> bestFromList(Iterable<ModelPart> part1, Iterable<ModelPart> part2, Random randomSource, PoseStack poseStack) {
//        List<ModelPart> list = new ArrayList<>();
//        part1.forEach(list::add);
//        part2.forEach(list::add);
//        return bestFromListMutable(list, randomSource, poseStack, true);
//    }



    @Nullable
    private Pair<ModelPart,Runnable> bestFromListMutable(List<ModelPart> partsMutable, Random randomSource, PoseStack poseStack, boolean firstIteration) {
        Collections.shuffle(partsMutable, randomSource);
        //try children instead
        for (ModelPart modelPart : partsMutable) {
            if (modelPart.visible) {
                if (!modelPart.cubes.isEmpty() && !modelPart.skipDraw) {
                    return Pair.of(modelPart, () -> modelPart.translateAndRotate(poseStack));
                }
                if (modelPart.children.isEmpty())continue;

                var child = bestFromListMutable(new ArrayList<>(modelPart.children.values()), randomSource, poseStack, false);
                if (child != null){
                    var runnable = child.getSecond();
                    return Pair.of(child.getFirst(), () -> {
                        modelPart.translateAndRotate(poseStack);
                        runnable.run();
                    });
                }
            }
        }
        if (firstIteration && !partsMutable.isEmpty()) {
            //noinspection SequencedCollectionMethodCanBeUsed
            var part = partsMutable.get(0);
            return Pair.of(part, () -> part.translateAndRotate(poseStack));
        }
        return null;
    }

    public static class ArrowLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends PinCushionLayer<T, M> {

        public ArrowLayer(EntityRendererProvider.Context context, LivingEntityRenderer<?,T, M> renderer) {
            super(renderer,
                    new ArrowModel(context.bakeLayer(ModelLayers.ARROW)),
                    TippableArrowRenderer.NORMAL_ARROW_LOCATION,
                    StuckInBodyLayer.PlacementStyle.IN_CUBE);
        }

        protected int numStuck() {
            return EntityPinCushions.PINCUSHION_COUNT_ARROW;
        }
    }

    public static class BeeStingerLayer<T extends LivingEntityRenderState, M extends EntityModel<T>> extends PinCushionLayer<T, M> {
        private static final ResourceLocation BEE_STINGER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_stinger.png");

        public BeeStingerLayer(EntityRendererProvider.Context context, LivingEntityRenderer<?,T, M> renderer) {
            super(renderer,
                    new BeeStingerModel(context.bakeLayer(ModelLayers.BEE_STINGER)),
                    BEE_STINGER_LOCATION,
                    StuckInBodyLayer.PlacementStyle.ON_SURFACE);
        }

        protected int numStuck() {
            return EntityPinCushions.PINCUSHION_COUNT_STINGER;
        }
    }
}

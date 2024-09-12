package traben.stuck_arrows_for_all;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.FrogModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.*;
import java.util.random.RandomGenerator;

public abstract class AllStuckInBodyLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public AllStuckInBodyLayer(LivingEntityRenderer<T, M> renderer) {
        super(renderer);
    }

    protected abstract int numStuck(T entity);

    protected abstract void renderStuckItem(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Entity entity, float x, float y, float z, float partialTick);

    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        int i = this.numStuck(livingEntity);
        RandomSource randomSource = RandomSource.create(livingEntity.getId());
        if (i > 0) {
            for (int j = 0; j < i; ++j) {
                RandomGenerator partRand = new Random(j);
                poseStack.pushPose();
                M model = getParentModel();
                Pair<ModelPart,Runnable> modelPart = switch (model) {
                    //case PlayerModel<?> playerModel -> playerModel.getRandomModelPart(randomSource);
                    case AgeableListModel<?> animal ->
                            bestFromList(animal.headParts(), animal.bodyParts(), partRand, poseStack);
                    case FrogModel<?> frogModel ->
                            bestFromListMutable(new ArrayList<>(Collections.singleton(frogModel.root())), partRand, poseStack,true);
                    case HierarchicalModel<?> hierarchicalModel ->
                            bestFromListMutable(new ArrayList<>(hierarchicalModel.root().children.values()), partRand, poseStack, true);
                    default -> null;
                };

                if (modelPart == null) {
                    poseStack.popPose();
                    return;
                }

                modelPart.getSecond().run();//transforms
//                modelPart.translateAndRotate(poseStack);

                float f = randomSource.nextFloat();
                float g = randomSource.nextFloat();
                float h = randomSource.nextFloat();

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
                this.renderStuckItem(poseStack, buffer, packedLight, livingEntity, f, g, h, partialTicks);
                poseStack.popPose();
            }

        }
    }

    @Nullable
    private Pair<ModelPart,Runnable> bestFromList(Iterable<ModelPart> part1, Iterable<ModelPart> part2, RandomGenerator randomSource, PoseStack poseStack) {
        List<ModelPart> list = new ArrayList<>();
        part1.forEach(list::add);
        part2.forEach(list::add);
        return bestFromListMutable(list, randomSource, poseStack, true);
    }



    @Nullable
    private Pair<ModelPart,Runnable> bestFromListMutable(List<ModelPart> partsMutable, RandomGenerator randomSource, PoseStack poseStack, boolean firstIteration) {
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
            var part = partsMutable.getFirst();
            return Pair.of(part, () -> part.translateAndRotate(poseStack));
        }
        return null;
    }

//    @Nullable
//    private ModelPart bestFromListMutable(List<ModelPart> partsMutable, RandomGenerator randomSource, PoseStack poseStack) {
//        Collections.shuffle(partsMutable, randomSource);
//        //try children instead
//        for (ModelPart modelPart : partsMutable) {
//            if (modelPart.visible) {
//                if (!modelPart.cubes.isEmpty() && !modelPart.skipDraw) {
//                    return modelPart;
//                }
//                for (ModelPart part : modelPart.children.values()) {
//                    if (part.visible) {
//                        if (!part.cubes.isEmpty() && !part.skipDraw) {
//                            modelPart.translateAndRotate(poseStack);
//                            return part;
//                        }
//                    }
//                }
//            }
//        }
//        if (!partsMutable.isEmpty())
//            return partsMutable.getFirst();
//        return null;
//    }

    public static class ArrowLayer<T extends LivingEntity, M extends EntityModel<T>> extends AllStuckInBodyLayer<T, M> {
        private final EntityRenderDispatcher dispatcher;

        public ArrowLayer(EntityRendererProvider.Context context, LivingEntityRenderer<T, M> renderer) {
            super(renderer);
            this.dispatcher = context.getEntityRenderDispatcher();
        }

        protected int numStuck(T entity) {
            return entity.getArrowCount();
        }

        protected void renderStuckItem(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Entity entity, float x, float y, float z, float partialTick) {
            float f = Mth.sqrt(x * x + z * z);
            Arrow arrow = new Arrow(entity.level(), entity.getX(), entity.getY(), entity.getZ(), ItemStack.EMPTY, null){
                @Override
                public boolean isInWall() {
                    return true;
                }

                @Override
                public boolean onGround() {
                    return true;
                }
            };
            arrow.setYRot((float)(Math.atan2(x, z) * 57.2957763671875));
            arrow.setXRot((float)(Math.atan2(y, f) * 57.2957763671875));
            arrow.yRotO = arrow.getYRot();
            arrow.xRotO = arrow.getXRot();
            this.dispatcher.render(arrow, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, buffer, packedLight);
        }
    }

    public static class BeeStingerLayer<T extends LivingEntity, M extends EntityModel<T>> extends AllStuckInBodyLayer<T, M> {
        private static final ResourceLocation BEE_STINGER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_stinger.png");

        public BeeStingerLayer(LivingEntityRenderer<T, M> renderer) {
            super(renderer);
        }

        protected int numStuck(T entity) {
            return entity.getStingerCount();
        }

        protected void renderStuckItem(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Entity entity, float x, float y, float z, float partialTick) {
            float f = Mth.sqrt(x * x + z * z);
            float g = (float)(Math.atan2((double)x, (double)z) * 57.2957763671875);
            float h = (float)(Math.atan2((double)y, (double)f) * 57.2957763671875);
            poseStack.translate(0.0F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(g - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(h));
            float i = 0.0F;
            float j = 0.125F;
            float k = 0.0F;
            float l = 0.0625F;
            float m = 0.03125F;
            poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
            poseStack.scale(0.03125F, 0.03125F, 0.03125F);
            poseStack.translate(2.5F, 0.0F, 0.0F);
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(BEE_STINGER_LOCATION));

            for(int n = 0; n < 4; ++n) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                PoseStack.Pose pose = poseStack.last();
                vertex(vertexConsumer, pose, -4.5F, -1, 0.0F, 0.0F, packedLight);
                vertex(vertexConsumer, pose, 4.5F, -1, 0.125F, 0.0F, packedLight);
                vertex(vertexConsumer, pose, 4.5F, 1, 0.125F, 0.0625F, packedLight);
                vertex(vertexConsumer, pose, -4.5F, 1, 0.0F, 0.0625F, packedLight);
            }

        }

        private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, int y, float u, float v, int packedLight) {
            consumer.addVertex(pose, x, (float)y, 0.0F).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }
}

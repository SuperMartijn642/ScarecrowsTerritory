package com.supermartijn642.scarecrowsterritory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.core.block.BlockShape;
import com.supermartijn642.core.gui.ScreenUtils;
import com.supermartijn642.core.render.RenderUtils;
import com.supermartijn642.core.render.RenderWorldEvent;
import com.supermartijn642.core.render.TextureAtlases;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaternionf;

import java.util.Collection;

/**
 * Created 03/11/2023 by SuperMartijn642
 */
public class ScarecrowTrophyHighlighter {

    public static final ResourceLocation CONFIRMATION_SPRITE = new ResourceLocation("scarecrowsterritory", "confirmation");
    private static final BlockShape HIGHLIGHT_SHAPE = BlockShape.fullCube().shrink(0.2f);

    public static void registerListeners(){
        MinecraftForge.EVENT_BUS.addListener(ScarecrowTrophyHighlighter::renderWorld);
    }

    private static void renderWorld(RenderWorldEvent e){
        HitResult hitResult = ClientUtils.getMinecraft().hitResult;
        if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK || !(hitResult instanceof BlockHitResult))
            return;

        Level level = ClientUtils.getWorld();
        BlockEntity entity = level.getBlockEntity(((BlockHitResult)hitResult).getBlockPos());
        if(!(entity instanceof ScarecrowBlockEntity))
            return;

        ScarecrowBlockEntity scarecrow = ((ScarecrowBlockEntity)entity);
        BlockPos scarecrowPos = scarecrow.getBlockPos();
        if(!scarecrow.getBlockState().getValue(ScarecrowBlock.BOTTOM))
            scarecrowPos = scarecrowPos.below();
        Collection<BlockPos> trophies = scarecrow.getTrophyPositions();

        PoseStack poseStack = e.getPoseStack();
        Vec3 camera = RenderUtils.getCameraPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        if(trophies.size() > 0){
            // Highlight all the trophies
            boolean isValidConfiguration = scarecrow.satisfiesTrophyConditions();
            float red = isValidConfiguration ? 0 : 1, green = isValidConfiguration ? 1 : 0, blue = 0;
            for(BlockPos trophyPosition : trophies){
                BlockState trophyState = level.getBlockState(trophyPosition);
                VoxelShape shape = trophyState.getVisualShape(level, trophyPosition, CollisionContext.empty());

                poseStack.pushPose();
                poseStack.translate(trophyPosition.getX(), trophyPosition.getY(), trophyPosition.getZ());
                RenderUtils.renderShape(poseStack, shape, red, green, blue, 0.8f, true);
                poseStack.popPose();
            }
        }else{
            // Highlight spaces for trophies
            int range = ScarecrowsTerritoryConfig.trophyCheckRange.get();
            for(int x = -range; x <= range; x++){
                for(int z = -range; z <= range; z++){
                    if(x == 0 && z == 0)
                        continue;
                    poseStack.pushPose();
                    poseStack.translate(scarecrowPos.getX() + x, scarecrowPos.getY(), scarecrowPos.getZ() + z);
                    RenderUtils.renderShapeSides(poseStack, HIGHLIGHT_SHAPE, 0.6f, 0.6f, 0.6f, 0.5f, true);
                    RenderUtils.renderShape(poseStack, HIGHLIGHT_SHAPE, 0.7f, 0.7f, 0.7f, 0.6f, true);
                    poseStack.popPose();
                }
            }
        }

        // Render whether the setup is valid
        boolean valid = scarecrow.satisfiesTrophyConditions();
        TextureAtlasSprite sprite = ClientUtils.getMinecraft().getTextureAtlas(TextureAtlases.getBlocks()).apply(CONFIRMATION_SPRITE);
        float width = sprite.getU1() - sprite.getU0(), height = sprite.getV1() - sprite.getV0();
        poseStack.translate(scarecrowPos.getX() + 0.5, scarecrowPos.getY() + 3, scarecrowPos.getZ() + 0.5);
        poseStack.mulPose(new Quaternionf().rotateY((float)(Math.PI / 2 - Math.atan2(camera.z - scarecrowPos.getZ() - 0.5, camera.x - scarecrowPos.getX() - 0.5))));
        poseStack.translate(- 0.5, 0, 0);
        poseStack.scale(1, -1, 1);
        ScreenUtils.drawTexture(poseStack, 0, 0, 1, 1, valid ? sprite.getU0() : sprite.getU0() + width / 2, sprite.getV0(), width / 2, height);

        poseStack.popPose();
    }
}

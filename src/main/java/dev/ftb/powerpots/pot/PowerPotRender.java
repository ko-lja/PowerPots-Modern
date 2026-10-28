package dev.ftb.powerpots.pot;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkhax.botanypots.BotanyPotHelper;
import net.darkhax.botanypots.data.displaystate.DisplayState;
import net.darkhax.botanypots.data.displaystate.render.DisplayStateRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PowerPotRender implements BlockEntityRenderer<PowerPotTileBase> {
    public PowerPotRender(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(PowerPotTileBase tile, float partial, PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {
        if (tile.getSoil() == null) {
            return;
        }

        Level level = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        int maxGrowth = tile.getInventory().getRequiredGrowthTime();
        float partialOffset = tile.getGrowthTime() < maxGrowth ? partial : 0f;
        float growthProgress = Math.max((tile.getGrowthTime() + partialOffset) / maxGrowth, 0f);

        matrix.pushPose();
        matrix.translate(0.251, 0.0635, 0.251);
        matrix.scale(0.498f, 0.3f, 0.498f);
        DisplayStateRenderer.renderState(tile.getSoil().getDisplayState(level, pos, tile), matrix, level, pos, buffer, light, overlay, growthProgress);
        matrix.popPose();

        if (tile.getCrop() != null && BotanyPotHelper.canCropGrow(level, pos, tile, tile.getSoil(), tile.getCrop())) {
            matrix.pushPose();
            matrix.translate(0.5, 0.36, 0.5);

            float growth = Mth.clamp((0.25f + growthProgress * 0.75f) * 0.55f, 0.13f, 0.55f);
            matrix.scale(growth, growth, growth);
            matrix.translate(-0.5, 0, -0.5);

            int previousBlocks = 0;
            for (DisplayState state : tile.getCrop().getDisplayState(level, pos, tile)) {
                if (previousBlocks > 0) {
                    matrix.translate(0, 1, 0);
                }

                matrix.pushPose();
                DisplayStateRenderer.renderState(state, matrix, level, pos, buffer, light, overlay, growthProgress);
                matrix.popPose();
                previousBlocks++;
            }

            matrix.popPose();
        }
    }
}
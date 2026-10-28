package dev.ftb.powerpots.pot;

import dev.ftb.powerpots.PowerPots;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class PowerPotTile {
    public static class MK1Tile extends PowerPotTileBase {
        public MK1Tile(BlockPos pos, BlockState state) {
            super(PowerPots.POWER_TILE_MK1.get(), pos, state, PotTier.MK1);
        }
    }

    public static class MK2Tile extends PowerPotTileBase {
        public MK2Tile(BlockPos pos, BlockState state) {
            super(PowerPots.POWER_TILE_MK2.get(), pos, state, PotTier.MK2);
        }
    }

    public static class MK3Tile extends PowerPotTileBase {
        public MK3Tile(BlockPos pos, BlockState state) {
            super(PowerPots.POWER_TILE_MK3.get(), pos, state, PotTier.MK3);
        }
    }

    public static class MK4Tile extends PowerPotTileBase {
        public MK4Tile(BlockPos pos, BlockState state) {
            super(PowerPots.POWER_TILE_MK4.get(), pos, state, PotTier.MK4);
        }
    }
}

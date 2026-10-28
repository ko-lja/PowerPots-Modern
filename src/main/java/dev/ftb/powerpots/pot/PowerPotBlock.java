package dev.ftb.powerpots.pot;

import dev.ftb.powerpots.Config;
import dev.ftb.powerpots.PowerPots;
import net.darkhax.botanypots.BotanyPotHelper;
import net.darkhax.botanypots.block.BlockBotanyPot;
import net.darkhax.botanypots.block.inv.BotanyPotContainer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

public class PowerPotBlock extends BlockBotanyPot {
    private final VoxelShape NORTH = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(3, 1, 3, 13, 6, 4), Block.box(3, 1, 12, 13, 6, 13), Block.box(12, 1, 4, 13, 6, 12), Block.box(3, 1, 4, 4, 6, 12), Block.box(2, 0, 5, 3, 2, 11), Block.box(3, 0, 5, 4, 1, 11), Block.box(2, 2, 6, 3, 4, 11)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private final VoxelShape SOUTH = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(3, 1, 12, 13, 6, 13), Block.box(3, 1, 3, 13, 6, 4), Block.box(3, 1, 4, 4, 6, 12), Block.box(12, 1, 4, 13, 6, 12), Block.box(13, 0, 5, 14, 2, 11), Block.box(12, 0, 5, 13, 1, 11), Block.box(13, 2, 5, 14, 4, 10)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private final VoxelShape EAST = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(12, 1, 3, 13, 6, 13), Block.box(3, 1, 3, 4, 6, 13), Block.box(4, 1, 12, 12, 6, 13), Block.box(4, 1, 3, 12, 6, 4), Block.box(5, 0, 2, 11, 2, 3), Block.box(5, 0, 3, 11, 1, 4), Block.box(5, 2, 2, 10, 4, 3)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private final VoxelShape WEST = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(3, 1, 3, 4, 6, 13), Block.box(12, 1, 3, 13, 6, 13), Block.box(4, 1, 3, 12, 6, 4), Block.box(4, 1, 12, 12, 6, 13), Block.box(5, 0, 13, 11, 2, 14), Block.box(5, 0, 12, 11, 1, 13), Block.box(6, 2, 13, 11, 4, 14)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    private final PotTier tier;

    public PowerPotBlock(PotTier tier) {
        // Power pots always behave like hopper pots
        super(true);
        this.tier = tier;
    }

    public PotTier getTier() {
        return this.tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        String key = "tooltip." + PowerPots.MOD_ID + ".";
        Config.PotMkConfig config = this.tier.config;

        tooltip.add(Component.translatable(key + "desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + "speed",
                Component.literal("-" + Math.round(config.speedModifier.get() * 100) + "%").withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + "energy",
                Component.literal(String.format("%,d FE/t", config.perTickEnergy.get())).withStyle(ChatFormatting.RED),
                Component.literal(String.format("%,d FE", config.maxEnergy.get())).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + "output",
                Component.literal(config.itemsPerOutput.get() + "x").withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + "export").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            state = defaultBlockState();
        }

        return state.setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER).setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        Direction value = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (value == Direction.NORTH) return NORTH;
        if (value == Direction.EAST) return EAST;
        if (value == Direction.SOUTH) return SOUTH;
        return WEST;
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (stateIn.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(stateIn, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof PowerPotTileBase pot)) {
            return super.use(state, world, pos, player, hand, hit);
        }

        final BotanyPotContainer inventory = pot.getInventory();

        // Attempt removal
        if (player.isShiftKeyDown()) {
            if (!inventory.getCropStack().isEmpty()) {
                if (!world.isClientSide) {
                    popResource(world, pos, inventory.removeItemNoUpdate(BotanyPotContainer.CROP_SLOT));

                    for (int slot : BotanyPotContainer.STORAGE_SLOT) {
                        if (!inventory.getItem(slot).isEmpty()) {
                            popResource(world, pos, inventory.removeItemNoUpdate(slot));
                        }
                    }

                    inventory.setChanged();
                }

                return InteractionResult.sidedSuccess(world.isClientSide);
            }

            if (!inventory.getSoilStack().isEmpty()) {
                if (!world.isClientSide) {
                    popResource(world, pos, inventory.removeItemNoUpdate(BotanyPotContainer.SOIL_SLOT));
                    inventory.setChanged();
                }

                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        final ItemStack heldItem = player.getItemInHand(hand);

        // Attempt to insert, soil first then crop. If nothing can be inserted fall back to the
        // default pot behaviour, which opens the pot GUI.
        if (!heldItem.isEmpty()) {
            // Power pots only grow from energy, so fertilizers are not allowed to speed them up.
            if (BotanyPotHelper.findFertilizer(state, world, pos, player, hand, heldItem, pot) != null) {
                return InteractionResult.PASS;
            }

            int slot = -1;
            if (inventory.getSoilStack().isEmpty()) {
                slot = BotanyPotContainer.SOIL_SLOT;
            } else if (inventory.getCropStack().isEmpty()) {
                slot = BotanyPotContainer.CROP_SLOT;
            }

            if (slot != -1 && inventory.canPlaceItem(slot, heldItem)) {
                if (!world.isClientSide) {
                    inventory.setItem(slot, heldItem.copyWithCount(1));

                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }

                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (this.tier) {
            case MK1 -> new PowerPotTile.MK1Tile(pos, state);
            case MK2 -> new PowerPotTile.MK2Tile(pos, state);
            case MK3 -> new PowerPotTile.MK3Tile(pos, state);
            case MK4 -> new PowerPotTile.MK4Tile(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (tickLevel, pos, tickState, tile) -> {
            if (tile instanceof PowerPotTileBase pot) {
                PowerPotTileBase.tick(tickLevel, pos, tickState, pot);
            }
        };
    }
}

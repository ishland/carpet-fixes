package carpetfixes.mixins.blockUpdates;

import carpet.CarpetSettings;
import carpetfixes.CarpetFixesSettings;
import carpetfixes.helpers.Utils;
import net.minecraft.block.*;
import net.minecraft.block.enums.RailShape;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PoweredRailBlock.class)
public abstract class PoweredRailBlock_fasterMixin extends AbstractRailBlock {

    protected PoweredRailBlock_fasterMixin(boolean allowCurves, Settings settings) {super(allowCurves, settings);}

    @Shadow @Final public static EnumProperty<RailShape> SHAPE;
    @Shadow @Final public static BooleanProperty POWERED;

    @Shadow protected boolean isPoweredByOtherRails(World world, BlockPos pos, BlockState state, boolean bl, int distance) { return false;}

    PoweredRailBlock self = (PoweredRailBlock)(Object)this;


    @Inject(
            method = "updateBlockState(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void updateBlockState(BlockState state, World world, BlockPos pos, Block neighbor, CallbackInfo ci) {
        if (CarpetFixesSettings.optimizedPoweredRails) {
            boolean shouldBePowered = world.isReceivingRedstonePower(pos) || this.isPoweredByOtherRails(world, pos, state, true, 0) || this.isPoweredByOtherRails(world, pos, state, false, 0);
            if (shouldBePowered != state.get(POWERED)) {
                RailShape railShape = state.get(SHAPE);
                if (!shouldBePowered || railShape.isAscending()) {
                    world.setBlockState(pos, state.with(POWERED, shouldBePowered), 3);
                    if (CarpetFixesSettings.uselessSelfBlockUpdateFix) {
                        world.updateNeighborsExcept(pos.down(),self, Direction.UP);
                        if ((state.get(SHAPE)).isAscending()) {
                            world.updateNeighborsExcept(pos.down(),self, Direction.DOWN);
                        }
                    } else {
                        world.updateNeighborsAlways(pos.down(), self);
                        if (railShape.isAscending()) {
                            world.updateNeighborsAlways(pos.up(), self);
                        }
                    }
                } else {
                    powerLane(world, pos, state, railShape); //no ascending rails & should be powered. So very small performance change
                }
            }
            ci.cancel();
        }
    }

    public void powerLane(World world, BlockPos pos, BlockState mainstate, RailShape railShape) {
        boolean wasOn = CarpetSettings.impendingFillSkipUpdates.get();
        CarpetSettings.impendingFillSkipUpdates.set(true);
        world.setBlockState(pos, mainstate.with(POWERED, true), 18);
        if (railShape == RailShape.NORTH_SOUTH) { //Order: +z, -z
            int z11 = 0, z22 = 0;
            for (int z1 = 1; z1 < 9; z1++) {
                BlockState state = world.getBlockState(pos.south(z1));
                if (!state.isOf(Blocks.POWERED_RAIL) || state.get(POWERED)) break;
                world.setBlockState(pos.south(z1), state.with(POWERED, true), 18);
                z11++;
            }
            for (int z2 = 1; z2 < 9; z2++) {
                BlockState state = world.getBlockState(pos.north(z2));
                if (!state.isOf(Blocks.POWERED_RAIL) || state.get(POWERED)) break;
                world.setBlockState(pos.north(z2), state.with(POWERED, true), 18);
                z22++;
            }
            if (z11 != 0) {
                Block block = mainstate.getBlock();
                for (int zu1 = z11; zu1 >= 0; zu1--) {
                    BlockPos pos1 = pos.south(zu1);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.west(), block, pos, Direction.WEST);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.east(), block, pos, Direction.EAST);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.down(), block, pos, Direction.DOWN);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.up(), block, pos, Direction.UP);
                    if (zu1 == z11) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.south(zu1+1), block, pos, Direction.SOUTH);
                    }
                    if (zu1 == 0 && z22 == 0) {
                        Utils.updateNeighborWithShape(world, mainstate, pos1.north(), block, pos, Direction.NORTH);
                    }
                    BlockPos pos2 = pos.south(zu1).down();
                    Utils.updateNeighborWithShape(world, mainstate, pos2.west(), block, pos, Direction.WEST);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.east(), block, pos, Direction.EAST);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.down(), block, pos, Direction.DOWN);
                    if (zu1 == z11) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.south(zu1+1).down(), block, pos, Direction.DOWN);
                    }
                }
            }
            if (z22 != 0) {
                Block block = mainstate.getBlock();
                for (int zu2 = z22; zu2 >= 0; zu2--) {
                    BlockPos pos1 = pos.north(zu2);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.west(), block, pos, Direction.WEST);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.east(), block, pos, Direction.EAST);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.down(), block, pos, Direction.DOWN);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.up(), block, pos, Direction.UP);
                    if (zu2 == z22) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.north(zu2+1), block, pos, Direction.NORTH);
                    }
                    if (zu2 == 0 && z11 == 0) {
                        Utils.updateNeighborWithShape(world, mainstate, pos1.south(), block, pos, Direction.SOUTH);
                    }
                    BlockPos pos2 = pos.north(zu2).down();
                    Utils.updateNeighborWithShape(world, mainstate, pos2.west(), block, pos, Direction.WEST);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.east(), block, pos, Direction.EAST);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.down(), block, pos, Direction.DOWN);
                    if (zu2 == z22) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.north(zu2+1).down(), block, pos, Direction.DOWN);
                    }
                }
            }
        } else if (railShape == RailShape.EAST_WEST) { //Order: -x, +x
            int x11 = 0, x22 = 0;
            for (int x1 = 1; x1 < 9; x1++) {
                BlockState state = world.getBlockState(pos.west(x1));
                if (!state.isOf(Blocks.POWERED_RAIL) || state.get(POWERED)) break;
                world.setBlockState(pos.west(x1), state.with(POWERED, true), 18);
                x11++;
            }
            for (int x2 = 1; x2 < 9; x2++) {
                BlockState state = world.getBlockState(pos.east(x2));
                if (!state.isOf(Blocks.POWERED_RAIL) || state.get(POWERED)) break;
                world.setBlockState(pos.east(x2), state.with(POWERED, true), 18);
                x22++;
            }
            if (x11 != 0) {
                Block block = mainstate.getBlock();
                for (int xu1 = x11; xu1 >= 0; xu1--) {
                    BlockPos pos1 = pos.west(xu1);
                    if (xu1 == x11) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.west(xu1+1), block, pos, Direction.WEST);
                    }
                    if (xu1 == 0 && x22 == 0) {
                        Utils.updateNeighborWithShape(world, mainstate, pos1.east(), block, pos, Direction.EAST);
                    }
                    Utils.updateNeighborWithShape(world, mainstate, pos1.down(), block, pos, Direction.DOWN);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.up(), block, pos, Direction.UP);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.north(), block, pos, Direction.NORTH);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.south(), block, pos, Direction.SOUTH);
                    BlockPos pos2 = pos.west(xu1).down();
                    Utils.updateNeighborWithShape(world, mainstate, pos2.down(), block, pos, Direction.DOWN);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.north(), block, pos, Direction.NORTH);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.south(), block, pos, Direction.SOUTH);
                    if (xu1 == x11) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.west(xu1+1).down(), block, pos, Direction.DOWN);
                    }
                }
            }
            if (x22 != 0) {
                Block block = mainstate.getBlock();
                for (int xu2 = x22; xu2 >= 0; xu2--) {
                    BlockPos pos1 = pos.east(xu2);
                    if (xu2 == 0 && x11 == 0) {
                        Utils.updateNeighborWithShape(world, mainstate, pos1.west(), block, pos, Direction.WEST);
                    }
                    if (xu2 == x22) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.east(xu2+1), block, pos, Direction.EAST);
                    }
                    Utils.updateNeighborWithShape(world, mainstate, pos1.down(), block, pos, Direction.DOWN);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.up(), block, pos, Direction.UP);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.north(), block, pos, Direction.NORTH);
                    Utils.updateNeighborWithShape(world, mainstate, pos1.south(), block, pos, Direction.SOUTH);
                    BlockPos pos2 = pos.east(xu2).down();
                    Utils.updateNeighborWithShape(world, mainstate, pos2.down(), block, pos, Direction.DOWN);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.north(), block, pos, Direction.NORTH);
                    Utils.updateNeighborWithShape(world, mainstate, pos2.south(), block, pos, Direction.SOUTH);
                    if (xu2 == x22) {
                        Utils.updateNeighborWithShape(world, mainstate, pos.east(xu2+1).down(), block, pos, Direction.DOWN);
                    }
                }
            }
        }
        CarpetSettings.impendingFillSkipUpdates.set(wasOn);
    }
}

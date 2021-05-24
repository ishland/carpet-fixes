package carpetfixes.mixins.entityFixes;

import carpetfixes.CarpetFixesSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class Entity_blockCollisionMixin {

    private boolean first = true;

    @Shadow protected void checkBlockCollision() {}

    @Redirect(
            method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;checkBlockCollision()V")
    )
    protected void onEntityCollision(Entity entity) {
        first = true;
        this.checkBlockCollision();
    }

    @Inject(
            method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/Entity;adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;")
    )
    protected void InjectOnEntityCollisionHere(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (CarpetFixesSettings.blockCollisionCheckFix) {
            first = false;
            this.checkBlockCollision();
        }
    }

    public boolean shouldCheckCollision(Block block) {
        return block != Blocks.END_PORTAL && block != Blocks.NETHER_PORTAL && block != Blocks.CACTUS && block != Blocks.FIRE && block != Blocks.LAVA;
    }

    @Redirect(
            method = "checkBlockCollision()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V")
    )
    public void checkBlockCollisionBetter(BlockState blockState, World world, BlockPos pos, Entity entity) {
        boolean pass = shouldCheckCollision(blockState.getBlock());
        if (entity instanceof PlayerEntity && (!CarpetFixesSettings.blockCollisionCheckFix || (first && pass) || (!first && !pass))) {
            blockState.onEntityCollision(world, pos, entity);
        }
    }
}
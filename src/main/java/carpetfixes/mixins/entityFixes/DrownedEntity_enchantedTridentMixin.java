package carpetfixes.mixins.entityFixes;

import carpetfixes.CarpetFixesSettings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(DrownedEntity.class)
public class DrownedEntity_enchantedTridentMixin extends ZombieEntity {
    public DrownedEntity_enchantedTridentMixin(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = "attack", at = @At(value = "NEW", target = "net/minecraft/item/ItemStack"))
    private ItemStack createItemStack(ItemConvertible item) {
        if (CarpetFixesSettings.drownedEnchantedTridentsFix) {
            ItemStack holding = this.getActiveItem();
            ItemStack trident = new ItemStack(item);
            if (holding.getItem() != Items.TRIDENT) return trident;
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(holding);
            enchantments.remove(Enchantments.LOYALTY);
            EnchantmentHelper.set(enchantments, trident);
            return trident;
        }
        return new ItemStack(item);
    }
}

package dev.ftb.powerpots;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.darkhax.botanypots.addons.jei.JEIPlugin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class PowerPotsJei implements IModPlugin {
    public PowerPotsJei() {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(PowerPots.MOD_ID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK1.get()), JEIPlugin.CROP);
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK2.get()), JEIPlugin.CROP);
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK3.get()), JEIPlugin.CROP);
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK4.get()), JEIPlugin.CROP);
    }
}

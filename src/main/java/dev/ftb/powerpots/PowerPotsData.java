package dev.ftb.powerpots;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = PowerPots.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PowerPotsData {
    @SubscribeEvent
    public static void dataGenEvent(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new LangGen(output, PowerPots.MOD_ID, "en_us"));
        gen.addProvider(event.includeClient(), new ItemModelGens(output, PowerPots.MOD_ID, existingFileHelper));
        gen.addProvider(event.includeClient(), new BlockStateGen(output, PowerPots.MOD_ID, existingFileHelper));

        gen.addProvider(event.includeServer(), new BlockTagGen(output, event.getLookupProvider(), existingFileHelper));
        gen.addProvider(event.includeServer(), new RecipesGen(output));
        gen.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLootGen::new, LootContextParamSets.BLOCK)
        )));
    }

    private static class LangGen extends LanguageProvider {
        public LangGen(PackOutput output, String modid, String locale) {
            super(output, modid, locale);
        }

        @Override
        protected void addTranslations() {
            this.add("itemGroup." + PowerPots.MOD_ID, "Power Pots");

            this.addBlock(PowerPots.POWER_POT_MK1, "PowerPot MK1");
            this.addBlock(PowerPots.POWER_POT_MK2, "PowerPot MK2");
            this.addBlock(PowerPots.POWER_POT_MK3, "PowerPot MK3");
            this.addBlock(PowerPots.POWER_POT_MK4, "PowerPot MK4");

            this.add("tooltip." + PowerPots.MOD_ID + ".desc", "Grows crops using Forge Energy");
            this.add("tooltip." + PowerPots.MOD_ID + ".speed", "Growth time: %s");
            this.add("tooltip." + PowerPots.MOD_ID + ".energy", "Uses %s, stores %s");
            this.add("tooltip." + PowerPots.MOD_ID + ".output", "Yield: %s per harvest");
            this.add("tooltip." + PowerPots.MOD_ID + ".export", "Exports into the inventory below");
        }
    }

    private static class BlockStateGen extends BlockStateProvider {
        public BlockStateGen(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
            super(output, modid, existingFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            block(PowerPots.POWER_POT_MK1, modLoc("block/t1"));
            block(PowerPots.POWER_POT_MK2, modLoc("block/t2"));
            block(PowerPots.POWER_POT_MK3, modLoc("block/t3"));
            block(PowerPots.POWER_POT_MK4, modLoc("block/t4"));
        }

        private void block(RegistryObject<Block> block, ResourceLocation texture) {
            horizontalBlock(block.get(), models().getBuilder(block.getId().getPath())
                    .parent(models().getExistingFile(modLoc("block/power-pot")))
                    .texture("base", texture)
                    .texture("particle", texture)
                    .renderType("cutout"));
        }
    }

    private static class ItemModelGens extends ItemModelProvider {
        public ItemModelGens(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
            super(output, modid, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            this.simpleItem(PowerPots.POWER_POT_MK1_ITEM);
            this.simpleItem(PowerPots.POWER_POT_MK2_ITEM);
            this.simpleItem(PowerPots.POWER_POT_MK3_ITEM);
            this.simpleItem(PowerPots.POWER_POT_MK4_ITEM);
        }

        private void simpleItem(RegistryObject<Item> item) {
            String path = item.getId().getPath();
            getBuilder(path).parent(new ModelFile.UncheckedModelFile(this.modLoc("block/" + path)));
        }
    }

    private static class RecipesGen extends RecipeProvider {
        public RecipesGen(PackOutput output) {
            super(output);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
            TagKey<Item> hopperPots = ItemTags.create(ResourceLocation.fromNamespaceAndPath("botanypots", "hopper_botany_pots"));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PowerPots.POWER_POT_MK1_ITEM.get())
                    .group(PowerPots.MOD_ID + ":pot")
                    .pattern("CRC")
                    .pattern("IPI")
                    .pattern("III")
                    .define('C', Tags.Items.INGOTS_COPPER)
                    .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                    .define('I', Tags.Items.INGOTS_IRON)
                    .define('P', hopperPots)
                    .unlockedBy("has_hopper_pot", has(hopperPots))
                    .save(consumer);

            upgrade(consumer, PowerPots.POWER_POT_MK2_ITEM, PowerPots.POWER_POT_MK1_ITEM, Tags.Items.INGOTS_GOLD, Tags.Items.DUSTS_REDSTONE);
            upgrade(consumer, PowerPots.POWER_POT_MK3_ITEM, PowerPots.POWER_POT_MK2_ITEM, Tags.Items.GEMS_DIAMOND, Tags.Items.INGOTS_GOLD);
            upgrade(consumer, PowerPots.POWER_POT_MK4_ITEM, PowerPots.POWER_POT_MK3_ITEM, Tags.Items.ENDER_PEARLS, Tags.Items.INGOTS_NETHERITE);
        }

        private void upgrade(Consumer<FinishedRecipe> consumer, RegistryObject<Item> result, RegistryObject<Item> previous, TagKey<Item> main, TagKey<Item> secondary) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result.get())
                    .group(PowerPots.MOD_ID + ":pot")
                    .pattern(" M ")
                    .pattern("MPM")
                    .pattern("SMS")
                    .define('M', main)
                    .define('S', secondary)
                    .define('P', previous.get())
                    .unlockedBy("has_previous_pot", has(previous.get()))
                    .save(consumer);
        }
    }

    private static class BlockTagGen extends BlockTagsProvider {
        public BlockTagGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
            super(output, lookupProvider, PowerPots.MOD_ID, existingFileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            TagKey<Block> botanyPots = BlockTags.create(ResourceLocation.fromNamespaceAndPath("botanypots", "all_botany_pots"));
            tag(botanyPots)
                    .add(PowerPots.POWER_POT_MK1.get(), PowerPots.POWER_POT_MK2.get(), PowerPots.POWER_POT_MK3.get(), PowerPots.POWER_POT_MK4.get());

            tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .add(PowerPots.POWER_POT_MK1.get(), PowerPots.POWER_POT_MK2.get(), PowerPots.POWER_POT_MK3.get(), PowerPots.POWER_POT_MK4.get());
        }
    }

    public static class BlockLootGen extends BlockLootSubProvider {
        public BlockLootGen() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            this.dropSelf(PowerPots.POWER_POT_MK1.get());
            this.dropSelf(PowerPots.POWER_POT_MK2.get());
            this.dropSelf(PowerPots.POWER_POT_MK3.get());
            this.dropSelf(PowerPots.POWER_POT_MK4.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return PowerPots.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
        }
    }
}

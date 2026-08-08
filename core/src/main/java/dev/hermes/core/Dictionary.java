package dev.hermes.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hermes' vocabulary of Minecraft things. Every name is stored in friendly
 * English ("diamond ore", "iron golem", "fire resistance") and resolved
 * case-insensitively, with plurals and simple aliases tolerated.
 *
 * <p>When a name is unknown, {@link #suggest} finds the closest known word so
 * errors can say "did you mean 'diamond ore'?".
 */
public final class Dictionary {

    private static final Map<String, String> ITEMS = new HashMap<>();
    private static final Map<String, String> MOBS = new HashMap<>();
    private static final Map<String, String> EFFECTS = new HashMap<>();
    private static final Map<String, String> DIMS = new HashMap<>();
    private static final Map<String, String> WEATHER = new HashMap<>();
    private static final Map<String, String> DAYPARTS = new HashMap<>();
    private static final Map<String, String> SOUNDS = new HashMap<>();
    private static final Map<String, String> PARTICLES = new HashMap<>();
    private static final Map<String, String> BIOMES = new HashMap<>();
    private static final Map<String, String> GAMEMODES = new HashMap<>();

    private Dictionary() {}

    private static void put(Map<String, String> m, String... names) {
        String canonical = names[0];
        for (String n : names) m.put(n, canonical);
    }

    static {
        // ---------------- items & blocks ----------------
        // ores and minerals
        put(ITEMS, "coal ore", "coal_ore");
        put(ITEMS, "iron ore", "iron_ore");
        put(ITEMS, "gold ore", "gold_ore");
        put(ITEMS, "diamond ore", "diamond_ore");
        put(ITEMS, "emerald ore", "emerald_ore");
        put(ITEMS, "redstone ore", "redstone_ore");
        put(ITEMS, "lapis ore", "lapis_ore", "lapis lazuli ore");
        put(ITEMS, "quartz ore", "quartz_ore", "nether quartz ore");
        put(ITEMS, "copper ore", "copper_ore");
        put(ITEMS, "deepslate diamond ore", "deepslate_diamond_ore");
        put(ITEMS, "ancient debris", "ancient_debris");
        put(ITEMS, "diamond", "diamonds", "diamond item");
        put(ITEMS, "emerald", "emeralds");
        put(ITEMS, "iron ingot", "iron_ingot", "iron", "irons");
        put(ITEMS, "gold ingot", "gold_ingot", "gold", "golds");
        put(ITEMS, "copper ingot", "copper_ingot", "copper");
        put(ITEMS, "netherite ingot", "netherite_ingot", "netherite");
        put(ITEMS, "netherite scrap", "netherite_scrap");
        put(ITEMS, "coal", "coals");
        put(ITEMS, "charcoal");
        put(ITEMS, "redstone", "redstone dust", "redstone_dust");
        put(ITEMS, "lapis lazuli", "lapis_lazuli", "lapis");
        put(ITEMS, "quartz", "nether quartz", "nether_quartz");
        // food
        put(ITEMS, "apple", "apples");
        put(ITEMS, "golden apple", "golden_apples");
        put(ITEMS, "enchanted golden apple", "enchanted_golden_apple", "enchanted golden apples");
        put(ITEMS, "bread", "breads");
        put(ITEMS, "cooked beef", "cooked_beef", "steak", "beef");
        put(ITEMS, "raw beef", "raw_beef");
        put(ITEMS, "porkchop", "porkchops", "raw porkchop");
        put(ITEMS, "cooked porkchop", "cooked_porkchop");
        put(ITEMS, "chicken", "chickens");
        put(ITEMS, "cooked chicken", "cooked_chicken");
        put(ITEMS, "salmon");
        put(ITEMS, "cooked salmon", "cooked_salmon");
        put(ITEMS, "fish", "raw fish", "cod");
        put(ITEMS, "cooked fish", "cooked_cod");
        put(ITEMS, "carrot", "carrots");
        put(ITEMS, "golden carrot", "golden_carrots");
        put(ITEMS, "potato", "potatoes");
        put(ITEMS, "baked potato", "baked_potatoes");
        put(ITEMS, "pumpkin pie", "pumpkin_pies");
        put(ITEMS, "cookie", "cookies");
        put(ITEMS, "cake", "cakes");
        put(ITEMS, "melon slice", "melon_slice", "melon", "melons");
        put(ITEMS, "sweet berries", "sweet_berries");
        put(ITEMS, "sugar", "sugars");
        put(ITEMS, "mushroom stew", "mushroom_stew");
        put(ITEMS, "rotten flesh", "rotten_flesh");
        put(ITEMS, "spider eye", "spider_eyes");
        put(ITEMS, "fermented spider eye", "fermented_spider_eye");
        put(ITEMS, "chorus fruit", "chorus_fruit");
        put(ITEMS, "honey bottle", "honey_bottle");
        put(ITEMS, "milk bucket", "milk_bucket");
        // materials
        put(ITEMS, "stick", "sticks");
        put(ITEMS, "string", "strings");
        put(ITEMS, "feather", "feathers");
        put(ITEMS, "bone", "bones");
        put(ITEMS, "bone meal", "bone_meal");
        put(ITEMS, "gunpowder", "gunpowders");
        put(ITEMS, "leather", "leathers");
        put(ITEMS, "slime ball", "slime_ball", "slime");
        put(ITEMS, "ender pearl", "ender_pearls", "enderpearl");
        put(ITEMS, "eye of ender", "eye_of_ender");
        put(ITEMS, "blaze rod", "blaze_rods");
        put(ITEMS, "blaze powder", "blaze_powder");
        put(ITEMS, "magma cream", "magma_cream");
        put(ITEMS, "ghast tear", "ghast_tears");
        put(ITEMS, "phantom membrane", "phantom_membrane");
        put(ITEMS, "prismarine shard", "prismarine_shards");
        put(ITEMS, "prismarine crystals", "prismarine_crystals");
        put(ITEMS, "nether star", "nether_stars");
        put(ITEMS, "dragon egg", "dragon_eggs");
        put(ITEMS, "snowball", "snowballs");
        put(ITEMS, "egg", "eggs");
        put(ITEMS, "paper", "papers");
        put(ITEMS, "book", "books");
        put(ITEMS, "book and quill", "book_and_quill");
        put(ITEMS, "written book", "written_book");
        put(ITEMS, "enchanted book", "enchanted_book");
        put(ITEMS, "map", "maps");
        put(ITEMS, "compass", "compasses");
        put(ITEMS, "clock", "clocks");
        put(ITEMS, "fishing rod", "fishing_rods");
        put(ITEMS, "carrot on a stick", "carrot_on_a_stick");
        put(ITEMS, "name tag", "name_tags", "nametag");
        put(ITEMS, "lead", "leads");
        put(ITEMS, "saddle", "saddles");
        put(ITEMS, "totem of undying", "totem_of_undying");
        put(ITEMS, "firework rocket", "firework_rocket", "firework");
        put(ITEMS, "experience bottle", "experience_bottle");
        put(ITEMS, "glass bottle", "glass_bottle");
        put(ITEMS, "potion", "potions");
        put(ITEMS, "splash potion", "splash_potion");
        put(ITEMS, "tnt", "tnt block");
        // tools & weapons
        put(ITEMS, "wooden sword", "wooden_sword", "wood sword");
        put(ITEMS, "stone sword", "stone_sword");
        put(ITEMS, "iron sword", "iron_sword");
        put(ITEMS, "golden sword", "golden_sword", "gold sword");
        put(ITEMS, "diamond sword", "diamond_sword");
        put(ITEMS, "netherite sword", "netherite_sword");
        put(ITEMS, "wooden pickaxe", "wooden_pickaxe");
        put(ITEMS, "stone pickaxe", "stone_pickaxe");
        put(ITEMS, "iron pickaxe", "iron_pickaxe");
        put(ITEMS, "diamond pickaxe", "diamond_pickaxe");
        put(ITEMS, "golden pickaxe", "golden_pickaxe");
        put(ITEMS, "wooden axe", "wooden_axe");
        put(ITEMS, "iron axe", "iron_axe");
        put(ITEMS, "diamond axe", "diamond_axe");
        put(ITEMS, "iron shovel", "iron_shovel");
        put(ITEMS, "diamond shovel", "diamond_shovel");
        put(ITEMS, "bow", "bows");
        put(ITEMS, "arrow", "arrows");
        put(ITEMS, "shield", "shields");
        put(ITEMS, "trident", "tridents");
        put(ITEMS, "crossbow", "crossbows");
        put(ITEMS, "elytra");
        put(ITEMS, "flint and steel", "flint_and_steel");
        put(ITEMS, "shears", "shears");
        put(ITEMS, "bucket", "buckets");
        put(ITEMS, "water bucket", "water_bucket");
        put(ITEMS, "lava bucket", "lava_bucket");
        // armor
        put(ITEMS, "leather helmet", "leather_helmet");
        put(ITEMS, "leather chestplate", "leather_chestplate");
        put(ITEMS, "leather leggings", "leather_leggings");
        put(ITEMS, "leather boots", "leather_boots");
        put(ITEMS, "iron helmet", "iron_helmet");
        put(ITEMS, "iron chestplate", "iron_chestplate");
        put(ITEMS, "iron leggings", "iron_leggings");
        put(ITEMS, "iron boots", "iron_boots");
        put(ITEMS, "diamond helmet", "diamond_helmet");
        put(ITEMS, "diamond chestplate", "diamond_chestplate");
        put(ITEMS, "diamond leggings", "diamond_leggings");
        put(ITEMS, "diamond boots", "diamond_boots");
        put(ITEMS, "netherite helmet", "netherite_helmet");
        put(ITEMS, "netherite chestplate", "netherite_chestplate");
        put(ITEMS, "netherite leggings", "netherite_leggings");
        put(ITEMS, "netherite boots", "netherite_boots");
        // blocks
        put(ITEMS, "oak door", "oak_door");
        put(ITEMS, "iron door", "iron_door");
        put(ITEMS, "oak trapdoor", "oak_trapdoor");
        put(ITEMS, "iron trapdoor", "iron_trapdoor");
        put(ITEMS, "chest", "chests");
        put(ITEMS, "trapped chest", "trapped_chest");
        put(ITEMS, "ender chest", "ender_chest");
        put(ITEMS, "barrel", "barrels");
        put(ITEMS, "furnace", "furnaces");
        put(ITEMS, "blast furnace", "blast_furnace");
        put(ITEMS, "smoker", "smokers");
        put(ITEMS, "crafting table", "crafting_table", "workbench");
        put(ITEMS, "anvil", "anvils");
        put(ITEMS, "enchanting table", "enchanting_table");
        put(ITEMS, "beacon", "beacons");
        put(ITEMS, "hopper", "hoppers");
        put(ITEMS, "dispenser", "dispensers");
        put(ITEMS, "dropper", "droppers");
        put(ITEMS, "piston", "pistons");
        put(ITEMS, "sticky piston", "sticky_piston");
        put(ITEMS, "redstone lamp", "redstone_lamp");
        put(ITEMS, "redstone block", "redstone_block");
        put(ITEMS, "redstone torch", "redstone_torch");
        put(ITEMS, "torch", "torches");
        put(ITEMS, "soul torch", "soul_torch");
        put(ITEMS, "lantern", "lanterns");
        put(ITEMS, "soul lantern", "soul_lantern");
        put(ITEMS, "campfire", "campfires");
        put(ITEMS, "iron block", "iron_block");
        put(ITEMS, "gold block", "gold_block");
        put(ITEMS, "diamond block", "diamond_block");
        put(ITEMS, "emerald block", "emerald_block");
        put(ITEMS, "coal block", "coal_block");
        put(ITEMS, "lapis block", "lapis_block");
        put(ITEMS, "quartz block", "quartz_block");
        put(ITEMS, "netherite block", "netherite_block");
        put(ITEMS, "iron bars", "iron_bars");
        put(ITEMS, "obsidian", "obsidians");
        put(ITEMS, "crying obsidian", "crying_obsidian");
        put(ITEMS, "bedrock", "bedrocks");
        put(ITEMS, "stone", "stones");
        put(ITEMS, "cobblestone", "cobblestones");
        put(ITEMS, "mossy cobblestone", "mossy_cobblestone");
        put(ITEMS, "deepslate", "deepslates");
        put(ITEMS, "tuff", "tuffs");
        put(ITEMS, "calcite", "calcites");
        put(ITEMS, "dirt", "dirts");
        put(ITEMS, "grass block", "grass_block");
        put(ITEMS, "grass", "grasses", "short grass");
        put(ITEMS, "mycelium");
        put(ITEMS, "sand", "sands");
        put(ITEMS, "red sand", "red_sand");
        put(ITEMS, "gravel", "gravels");
        put(ITEMS, "clay", "clays");
        put(ITEMS, "snow block", "snow_block");
        put(ITEMS, "ice", "ices");
        put(ITEMS, "packed ice", "packed_ice");
        put(ITEMS, "blue ice", "blue_ice");
        put(ITEMS, "water", "waters");
        put(ITEMS, "lava", "lavas");
        put(ITEMS, "oak log", "oak_log");
        put(ITEMS, "spruce log", "spruce_log");
        put(ITEMS, "birch log", "birch_log");
        put(ITEMS, "oak planks", "oak_planks");
        put(ITEMS, "spruce planks", "spruce_planks");
        put(ITEMS, "birch planks", "birch_planks");
        put(ITEMS, "oak leaves", "oak_leaves");
        put(ITEMS, "glass", "glasses");
        put(ITEMS, "glass pane", "glass_pane");
        put(ITEMS, "wool", "wools", "white wool", "white_wool");
        put(ITEMS, "black wool", "black_wool");
        put(ITEMS, "terracotta", "terracottas", "hardened clay");
        put(ITEMS, "white terracotta", "white_terracotta");
        put(ITEMS, "brick", "bricks", "brick block");
        put(ITEMS, "stone bricks", "stone_bricks");
        put(ITEMS, "mossy stone bricks", "mossy_stone_bricks");
        put(ITEMS, "cracked stone bricks", "cracked_stone_bricks");
        put(ITEMS, "netherrack", "netherracks");
        put(ITEMS, "nether brick", "nether_brick", "nether bricks");
        put(ITEMS, "soul sand", "soul_sands");
        put(ITEMS, "glowstone", "glowstones");
        put(ITEMS, "end stone", "end_stone");
        put(ITEMS, "end stone bricks", "end_stone_bricks");
        put(ITEMS, "purpur block", "purpur_block");
        put(ITEMS, "spawner", "spawners", "mob spawner");
        put(ITEMS, "bookshelf", "bookshelves");
        put(ITEMS, "rail", "rails");
        put(ITEMS, "powered rail", "powered_rail");
        put(ITEMS, "detector rail", "detector_rail");
        put(ITEMS, "cactus", "cacti", "cactuses");
        put(ITEMS, "pumpkin", "pumpkins");
        put(ITEMS, "carved pumpkin", "carved_pumpkin");
        put(ITEMS, "jack o lantern", "jack_o_lantern");
        put(ITEMS, "sugarcane", "sugar cane", "sugar_cane");
        put(ITEMS, "ladder", "ladders");
        put(ITEMS, "oak fence", "oak_fence");
        put(ITEMS, "oak fence gate", "oak_fence_gate");
        put(ITEMS, "cobweb", "cobwebs");
        put(ITEMS, "sea lantern", "sea_lantern");
        put(ITEMS, "prismarine", "prismarines");
        put(ITEMS, "dark prismarine", "dark_prismarine");
        put(ITEMS, "slime block", "slime_block");
        put(ITEMS, "honey block", "honey_block");
        put(ITEMS, "honeycomb block", "honeycomb_block");
        put(ITEMS, "hay bale", "hay_bale");
        put(ITEMS, "note block", "note_block");
        put(ITEMS, "jukebox", "jukeboxes");
        put(ITEMS, "sign", "signs", "oak sign");
        put(ITEMS, "flower pot", "flower_pot");
        put(ITEMS, "cauldron", "cauldrons");
        put(ITEMS, "brewing stand", "brewing_stand");
        put(ITEMS, "bed", "beds", "red bed");
        put(ITEMS, "white bed", "white_bed");
        put(ITEMS, "stone button", "stone_button");
        put(ITEMS, "oak button", "oak_button");
        put(ITEMS, "lever", "levers");
        put(ITEMS, "stone pressure plate", "stone_pressure_plate");
        put(ITEMS, "oak pressure plate", "oak_pressure_plate");
        put(ITEMS, "observer", "observers");
        put(ITEMS, "comparator", "comparators");
        put(ITEMS, "repeater", "repeaters");
        put(ITEMS, "daylight sensor", "daylight_sensor");
        put(ITEMS, "target block", "target_block");
        put(ITEMS, "bell", "bells");
        put(ITEMS, "conduit", "conduits");
        put(ITEMS, "shulker box", "shulker_box");
        put(ITEMS, "respawn anchor", "respawn_anchor");
        put(ITEMS, "lodestone", "lodestones");
        put(ITEMS, "amethyst block", "amethyst_block");
        put(ITEMS, "amethyst shard", "amethyst_shards");
        put(ITEMS, "copper block", "copper_block");
        put(ITEMS, "raw iron", "raw_iron");
        put(ITEMS, "raw gold", "raw_gold");
        put(ITEMS, "raw copper", "raw_copper");
        put(ITEMS, "candle", "candles");
        put(ITEMS, "scaffold", "scaffolding", "scaffolds");
        put(ITEMS, "dripstone block", "dripstone_block");
        put(ITEMS, "glow berries", "glow_berries");
        put(ITEMS, "glow lichen", "glow_lichen");
        put(ITEMS, "moss block", "moss_block");
        put(ITEMS, "mud", "muds");
        put(ITEMS, "packed mud", "packed_mud");
        put(ITEMS, "chain", "chains");
        put(ITEMS, "lightning rod", "lightning_rod");
        put(ITEMS, "bamboo", "bamboos");
        put(ITEMS, "vine", "vines");
        put(ITEMS, "nether wart", "nether_wart");
        put(ITEMS, "shroomlight", "shroomlights");
        put(ITEMS, "basalt", "basalts");
        put(ITEMS, "blackstone", "blackstones");
        put(ITEMS, "polished blackstone", "polished_blackstone");
        put(ITEMS, "gilded blackstone", "gilded_blackstone");
        // plants & flowers
        put(ITEMS, "poppy", "poppies");
        put(ITEMS, "dandelion", "dandelions");
        put(ITEMS, "blue orchid", "blue_orchid");
        put(ITEMS, "allium", "alliums");
        put(ITEMS, "azure bluet", "azure_bluet");
        put(ITEMS, "lily of the valley", "lily_of_the_valley");
        put(ITEMS, "cornflower", "cornflowers");
        put(ITEMS, "sunflower", "sunflowers");
        put(ITEMS, "rose bush", "rose_bush");
        put(ITEMS, "peony", "peonies");
        put(ITEMS, "lilac", "lilacs");
        put(ITEMS, "wither rose", "wither_rose");
        put(ITEMS, "oak sapling", "oak_sapling");
        put(ITEMS, "spruce sapling", "spruce_sapling");
        put(ITEMS, "birch sapling", "birch_sapling");
        put(ITEMS, "dead bush", "dead_bush");
        put(ITEMS, "fern", "ferns");
        put(ITEMS, "seagrass", "sea grass");
        put(ITEMS, "kelp", "kelps");

        // ---------------- mobs ----------------
        put(MOBS, "zombie", "zombies");
        put(MOBS, "zombie villager", "zombie_villager");
        put(MOBS, "zombie pigman", "zombie_pigman", "zombified piglin");
        put(MOBS, "husk", "husks");
        put(MOBS, "drowned", "drowneds");
        put(MOBS, "skeleton", "skeletons");
        put(MOBS, "wither skeleton", "wither_skeleton");
        put(MOBS, "stray", "strays");
        put(MOBS, "creeper", "creepers");
        put(MOBS, "spider", "spiders");
        put(MOBS, "cave spider", "cave_spider");
        put(MOBS, "enderman", "endermen");
        put(MOBS, "endermite", "endermites");
        put(MOBS, "blaze", "blazes");
        put(MOBS, "ghast", "ghasts");
        put(MOBS, "magma cube", "magma_cube");
        put(MOBS, "slime", "slimes");
        put(MOBS, "silverfish", "silverfishes");
        put(MOBS, "shulker", "shulkers");
        put(MOBS, "guardian", "guardians");
        put(MOBS, "elder guardian", "elder_guardian");
        put(MOBS, "phantom", "phantoms");
        put(MOBS, "pillager", "pillagers");
        put(MOBS, "vindicator", "vindicators");
        put(MOBS, "evoker", "evokers");
        put(MOBS, "vex", "vexes");
        put(MOBS, "ravager", "ravagers");
        put(MOBS, "witch", "witches");
        put(MOBS, "piglin", "piglins");
        put(MOBS, "piglin brute", "piglin_brute");
        put(MOBS, "hoglin", "hoglins");
        put(MOBS, "zoglin", "zoglins");
        put(MOBS, "villager", "villagers");
        put(MOBS, "wandering trader", "wandering_trader");
        put(MOBS, "iron golem", "iron_golem", "golem");
        put(MOBS, "snow golem", "snow_golem");
        put(MOBS, "wolf", "wolves");
        put(MOBS, "cat", "cats");
        put(MOBS, "ocelot", "ocelots");
        put(MOBS, "fox", "foxes");
        put(MOBS, "cow", "cows");
        put(MOBS, "pig", "pigs");
        put(MOBS, "sheep", "sheeps");
        put(MOBS, "chicken", "chickens");
        put(MOBS, "rabbit", "rabbits");
        put(MOBS, "horse", "horses");
        put(MOBS, "skeleton horse", "skeleton_horse");
        put(MOBS, "zombie horse", "zombie_horse");
        put(MOBS, "donkey", "donkeys");
        put(MOBS, "mule", "mules");
        put(MOBS, "llama", "llamas");
        put(MOBS, "turtle", "turtles");
        put(MOBS, "panda", "pandas");
        put(MOBS, "bee", "bees");
        put(MOBS, "parrot", "parrots");
        put(MOBS, "dolphin", "dolphins");
        put(MOBS, "squid", "squids");
        put(MOBS, "glow squid", "glow_squid");
        put(MOBS, "bat", "bats");
        put(MOBS, "axolotl", "axolotls");
        put(MOBS, "goat", "goats");
        put(MOBS, "frog", "frogs");
        put(MOBS, "allay", "allays");
        put(MOBS, "ender dragon", "ender_dragon", "dragon");
        put(MOBS, "wither", "withers");
        put(MOBS, "warden", "wardens");

        // ---------------- effects ----------------
        put(EFFECTS, "speed", "swiftness");
        put(EFFECTS, "slowness");
        put(EFFECTS, "haste", "mining speed");
        put(EFFECTS, "mining fatigue");
        put(EFFECTS, "strength", "power");
        put(EFFECTS, "instant health", "healing");
        put(EFFECTS, "instant damage", "harming");
        put(EFFECTS, "jump boost", "jumping");
        put(EFFECTS, "nausea");
        put(EFFECTS, "regeneration", "regen");
        put(EFFECTS, "resistance", "armor");
        put(EFFECTS, "fire resistance", "fireproof");
        put(EFFECTS, "water breathing", "breathing");
        put(EFFECTS, "invisibility", "invisible");
        put(EFFECTS, "blindness");
        put(EFFECTS, "night vision", "nightvision");
        put(EFFECTS, "hunger");
        put(EFFECTS, "weakness");
        put(EFFECTS, "poison", "poisoning");
        put(EFFECTS, "wither");
        put(EFFECTS, "glowing");
        put(EFFECTS, "levitation", "levitate");
        put(EFFECTS, "slow falling", "slowfall");
        put(EFFECTS, "luck", "good luck");
        put(EFFECTS, "unluck", "bad luck");
        put(EFFECTS, "darkness");
        put(EFFECTS, "saturation");
        put(EFFECTS, "absorption", "extra hearts");
        put(EFFECTS, "conduit power");
        put(EFFECTS, "dolphins grace", "dolphin grace");
        put(EFFECTS, "bad omen");
        put(EFFECTS, "hero of the village");
        // adjectives for "make player stronger"
        put(EFFECTS, "stronger", "strongest");
        put(EFFECTS, "faster", "speed boost");
        put(EFFECTS, "tougher", "harder");
        put(EFFECTS, "weaker");
        put(EFFECTS, "slower");
        put(EFFECTS, "invisible");
        put(EFFECTS, "glowing");

        // ---------------- dimensions ----------------
        put(DIMS, "overworld", "the overworld", "surface world");
        put(DIMS, "nether", "the nether", "hell");
        put(DIMS, "the end", "end", "the end dimension");

        // ---------------- weather ----------------
        put(WEATHER, "sunny", "sunshine", "sunny weather");
        put(WEATHER, "clear", "clear skies");
        put(WEATHER, "rain", "rainy", "raining", "it is raining");
        put(WEATHER, "storm", "stormy", "thunderstorm", "thunder", "thundering");

        // ---------------- day parts ----------------
        put(DAYPARTS, "dawn", "sunrise");
        put(DAYPARTS, "morning");
        put(DAYPARTS, "day", "daytime");
        put(DAYPARTS, "noon");
        put(DAYPARTS, "afternoon");
        put(DAYPARTS, "dusk", "sunset");
        put(DAYPARTS, "evening");
        put(DAYPARTS, "night", "nighttime");
        put(DAYPARTS, "midnight");

        // ---------------- sounds ----------------
        put(SOUNDS, "level up", "level_up", "levelup");
        put(SOUNDS, "explosion", "explode", "tnt");
        put(SOUNDS, "chest open", "chest_open");
        put(SOUNDS, "chest close", "chest_close");
        put(SOUNDS, "pop");
        put(SOUNDS, "click");
        put(SOUNDS, "bell");
        put(SOUNDS, "anvil land", "anvil_land", "anvil");
        put(SOUNDS, "portal", "portal travel", "portal_travel");
        put(SOUNDS, "dragon growl", "dragon_growl", "dragon roar");
        put(SOUNDS, "wither spawn", "wither_spawn", "wither roar");
        put(SOUNDS, "lightning", "thunder bolt");
        put(SOUNDS, "rain");
        put(SOUNDS, "fire", "fire crackle");
        put(SOUNDS, "lava", "lava pop");
        put(SOUNDS, "glass", "glass break");
        put(SOUNDS, "bow", "bow shoot");
        put(SOUNDS, "villager", "villager yes", "villager_yes");
        put(SOUNDS, "villager no", "villager_no");
        put(SOUNDS, "totem", "totem use");
        put(SOUNDS, "drink", "drinking");
        put(SOUNDS, "eat", "eating", "burp");
        put(SOUNDS, "sword", "sword hit");
        put(SOUNDS, "note", "note block", "note_block");

        // ---------------- particles ----------------
        put(PARTICLES, "heart", "hearts");
        put(PARTICLES, "flame", "flames");
        put(PARTICLES, "smoke", "small smoke", "small_smoke");
        put(PARTICLES, "large smoke", "large_smoke");
        put(PARTICLES, "lava", "lava drops", "lava_drip");
        put(PARTICLES, "cloud", "clouds");
        put(PARTICLES, "crit", "critical", "crits");
        put(PARTICLES, "explosion", "explosions");
        put(PARTICLES, "ender", "enderman teleport", "ender_teleport");
        put(PARTICLES, "villager", "angry villager", "angry_villager");
        put(PARTICLES, "happy villager", "happy_villager");
        put(PARTICLES, "note", "music note", "note note");
        put(PARTICLES, "enchant", "enchanting", "enchant_table");
        put(PARTICLES, "portal", "portals");
        put(PARTICLES, "water", "water drops", "water_drip");
        put(PARTICLES, "bubble", "bubbles");
        put(PARTICLES, "snow", "snowflakes", "snowflake");
        put(PARTICLES, "end rod", "endrod");
        put(PARTICLES, "totem", "totem of undying", "totem_of_undying");
        put(PARTICLES, "soul", "soul fire", "soul_fire_flame");

        // ---------------- gamemodes ----------------
        put(GAMEMODES, "survival", "survival mode");
        put(GAMEMODES, "creative", "creative mode");
        put(GAMEMODES, "adventure", "adventure mode");
        put(GAMEMODES, "spectator", "spectator mode");

        // ---------------- biomes ----------------
        String[] biomes = {
            "plains", "forest", "dark forest", "birch forest", "old growth birch forest",
            "flower forest", "meadow", "cherry grove", "taiga", "old growth taiga",
            "snowy taiga", "jungle", "bamboo jungle", "sparse jungle", "swamp", "mangrove swamp",
            "desert", "badlands", "wooded badlands", "savanna", "savanna plateau",
            "ocean", "deep ocean", "warm ocean", "lukewarm ocean", "cold ocean", "frozen ocean",
            "river", "frozen river", "beach", "stony shore", "snowy beach",
            "mushroom fields", "dripstone caves", "lush caves", "deep dark",
            "stony peaks", "jagged peaks", "frozen peaks", "snowy slopes", "grove",
            "windswept hills", "windswept forest", "windswept savanna",
            "ice spikes", "snowy plains", "sunflower plains",
            "the end", "end highlands", "end midlands", "end barrens", "small end islands",
            "nether wastes", "soul sand valley", "crimson forest", "warped forest", "basalt deltas",
        };
        for (String b : biomes) put(BIOMES, b, b.replace(' ', '_'));
    }

    // ---------- resolution ----------

    /** Normalizes any word to the friendly spelling Hermes uses. */
    public static String normalize(String name) {
        return name.trim().toLowerCase().replace('_', ' ').replaceAll("\\s+", " ");
    }

    private static String find(Map<String, String> dict, String name) {
        String n = normalize(name);
        String hit = dict.get(n);
        if (hit != null) return hit;
        if (n.endsWith("s") && n.length() > 3) {
            String singular = n.substring(0, n.length() - 1);
            hit = dict.get(singular);
            if (hit != null) return hit;
        }
        return null;
    }

    public static String findItem(String name) { return find(ITEMS, name); }
    public static String findMob(String name) { return find(MOBS, name); }
    public static String findEffect(String name) { return find(EFFECTS, name); }
    public static String findDim(String name) { return find(DIMS, name); }
    public static String findWeather(String name) { return find(WEATHER, name); }
    public static String findDaypart(String name) { return find(DAYPARTS, name); }
    public static String findSound(String name) { return find(SOUNDS, name); }
    public static String findParticle(String name) { return find(PARTICLES, name); }
    public static String findBiome(String name) { return find(BIOMES, name); }
    public static String findGamemode(String name) { return find(GAMEMODES, name); }

    public static boolean isItem(String name) { return findItem(name) != null; }
    public static boolean isMob(String name) { return findMob(name) != null; }
    public static boolean isEffect(String name) { return findEffect(name) != null; }
    public static boolean isDim(String name) { return findDim(name) != null; }
    public static boolean isDaypart(String name) { return findDaypart(name) != null; }

    /** The friendly name of a raw Minecraft key, e.g. "DIAMOND_ORE" -> "diamond ore". */
    public static String friendlyName(String rawKey) {
        String n = normalize(rawKey);
        String hit = findItem(n);
        if (hit != null) return hit;
        return n;
    }

    // ---------- platform key mapping ----------

    private static final Map<String, String> EFFECT_KEYS = new HashMap<>();
    private static final Map<String, String> SOUND_KEYS = new HashMap<>();
    private static final Map<String, String> PARTICLE_KEYS = new HashMap<>();
    private static final Map<String, String> BIOME_KEYS = new HashMap<>();

    static {
        // adjectives for "make player stronger" resolve to real effects
        String[][] eff = {
            {"stronger", "STRENGTH"}, {"faster", "SPEED"}, {"tougher", "RESISTANCE"},
            {"weaker", "WEAKNESS"}, {"slower", "SLOWNESS"}, {"invisible", "INVISIBILITY"},
            {"glowing", "GLOWING"},
            {"speed", "SPEED"}, {"slowness", "SLOWNESS"}, {"haste", "HASTE"},
            {"mining fatigue", "MINING_FATIGUE"}, {"strength", "STRENGTH"},
            {"instant health", "INSTANT_HEALTH"}, {"instant damage", "INSTANT_DAMAGE"},
            {"jump boost", "JUMP_BOOST"}, {"nausea", "NAUSEA"}, {"regeneration", "REGENERATION"},
            {"resistance", "RESISTANCE"}, {"fire resistance", "FIRE_RESISTANCE"},
            {"water breathing", "WATER_BREATHING"}, {"invisibility", "INVISIBILITY"},
            {"blindness", "BLINDNESS"}, {"night vision", "NIGHT_VISION"}, {"hunger", "HUNGER"},
            {"weakness", "WEAKNESS"}, {"poison", "POISON"}, {"wither", "WITHER"},
            {"glowing", "GLOWING"}, {"levitation", "LEVITATION"}, {"slow falling", "SLOW_FALLING"},
            {"luck", "LUCK"}, {"unluck", "UNLUCK"}, {"darkness", "DARKNESS"},
            {"saturation", "SATURATION"}, {"absorption", "ABSORPTION"},
            {"conduit power", "CONDUIT_POWER"}, {"dolphins grace", "DOLPHINS_GRACE"},
            {"bad omen", "BAD_OMEN"}, {"hero of the village", "HERO_OF_THE_VILLAGE"},
        };
        for (String[] pair : eff) EFFECT_KEYS.put(pair[0], pair[1]);

        String[][] snd = {
            {"level up", "ENTITY_PLAYER_LEVELUP"}, {"explosion", "ENTITY_GENERIC_EXPLODE"},
            {"chest open", "BLOCK_CHEST_OPEN"}, {"chest close", "BLOCK_CHEST_CLOSE"},
            {"pop", "ENTITY_ITEM_PICKUP"}, {"click", "UI_BUTTON_CLICK"},
            {"bell", "BLOCK_BELL_USE"}, {"anvil land", "BLOCK_ANVIL_LAND"},
            {"portal", "ENTITY_ENDERMAN_TELEPORT"}, {"dragon growl", "ENTITY_ENDER_DRAGON_GROWL"},
            {"wither spawn", "ENTITY_WITHER_SPAWN"}, {"lightning", "ENTITY_LIGHTNING_BOLT_THUNDER"},
            {"rain", "WEATHER_RAIN"}, {"fire", "BLOCK_FIRE_AMBIENT"},
            {"lava", "BLOCK_LAVA_AMBIENT"}, {"glass", "BLOCK_GLASS_BREAK"},
            {"bow", "ENTITY_ARROW_SHOOT"}, {"villager", "ENTITY_VILLAGER_YES"},
            {"villager no", "ENTITY_VILLAGER_NO"}, {"totem", "ITEM_TOTEM_USE"},
            {"drink", "ENTITY_GENERIC_DRINK"}, {"eat", "ENTITY_GENERIC_EAT"},
            {"sword", "ENTITY_PLAYER_ATTACK_STRONG"}, {"note", "BLOCK_NOTE_BLOCK_HARP"},
        };
        for (String[] pair : snd) SOUND_KEYS.put(pair[0], pair[1]);

        String[][] par = {
            {"heart", "HEART"}, {"flame", "FLAME"}, {"smoke", "SMOKE"},
            {"large smoke", "LARGE_SMOKE"}, {"lava", "LAVA"}, {"cloud", "CLOUD"},
            {"crit", "CRIT"}, {"explosion", "EXPLOSION"}, {"ender", "PORTAL"},
            {"villager", "VILLAGER_ANGRY"}, {"happy villager", "VILLAGER_HAPPY"},
            {"note", "NOTE"}, {"enchant", "ENCHANT"}, {"portal", "PORTAL"},
            {"water", "WATER_DROP"}, {"bubble", "BUBBLE"}, {"snow", "SNOWFLAKE"},
            {"end rod", "END_ROD"}, {"totem", "TOTEM"}, {"soul", "SOUL_FIRE_FLAME"},
        };
        for (String[] pair : par) PARTICLE_KEYS.put(pair[0], pair[1]);

        String[][] bio = {
            {"plains", "PLAINS"}, {"forest", "FOREST"}, {"dark forest", "DARK_FOREST"},
            {"birch forest", "BIRCH_FOREST"}, {"old growth birch forest", "OLD_GROWTH_BIRCH_FOREST"},
            {"flower forest", "FLOWER_FOREST"}, {"meadow", "MEADOW"}, {"cherry grove", "CHERRY_GROVE"},
            {"taiga", "TAIGA"}, {"old growth taiga", "OLD_GROWTH_TAIGA"},
            {"snowy taiga", "SNOWY_TAIGA"}, {"jungle", "JUNGLE"}, {"bamboo jungle", "BAMBOO_JUNGLE"},
            {"sparse jungle", "SPARSE_JUNGLE"}, {"swamp", "SWAMP"}, {"mangrove swamp", "MANGROVE_SWAMP"},
            {"desert", "DESERT"}, {"badlands", "BADLANDS"}, {"wooded badlands", "WOODED_BADLANDS"},
            {"savanna", "SAVANNA"}, {"savanna plateau", "SAVANNA_PLATEAU"},
            {"ocean", "OCEAN"}, {"deep ocean", "DEEP_OCEAN"}, {"warm ocean", "WARM_OCEAN"},
            {"lukewarm ocean", "LUKEWARM_OCEAN"}, {"cold ocean", "COLD_OCEAN"}, {"frozen ocean", "FROZEN_OCEAN"},
            {"river", "RIVER"}, {"frozen river", "FROZEN_RIVER"}, {"beach", "BEACH"},
            {"stony shore", "STONY_SHORE"}, {"snowy beach", "SNOWY_BEACH"},
            {"mushroom fields", "MUSHROOM_FIELDS"}, {"dripstone caves", "DRIPSTONE_CAVES"},
            {"lush caves", "LUSH_CAVES"}, {"deep dark", "DEEP_DARK"},
            {"stony peaks", "STONY_PEAKS"}, {"jagged peaks", "JAGGED_PEAKS"},
            {"frozen peaks", "FROZEN_PEAKS"}, {"snowy slopes", "SNOWY_SLOPES"}, {"grove", "GROVE"},
            {"windswept hills", "WINDSWEPT_HILLS"}, {"windswept forest", "WINDSWEPT_FOREST"},
            {"windswept savanna", "WINDSWEPT_SAVANNA"}, {"ice spikes", "ICE_SPIKES"},
            {"snowy plains", "SNOWY_PLAINS"}, {"sunflower plains", "SUNFLOWER_PLAINS"},
            {"the end", "THE_END"}, {"end highlands", "END_HIGHLANDS"}, {"end midlands", "END_MIDLANDS"},
            {"end barrens", "END_BARRENS"}, {"small end islands", "SMALL_END_ISLANDS"},
            {"nether wastes", "NETHER_WASTES"}, {"soul sand valley", "SOUL_SAND_VALLEY"},
            {"crimson forest", "CRIMSON_FOREST"}, {"warped forest", "WARPED_FOREST"},
            {"basalt deltas", "BASALT_DELTAS"},
        };
        for (String[] pair : bio) BIOME_KEYS.put(pair[0], pair[1]);
    }

    /** Bukkit PotionEffectType key for a Hermes effect name (including adjectives). */
    public static String effectKey(String canonical) {
        String k = EFFECT_KEYS.get(canonical);
        return k != null ? k : canonical.toUpperCase().replace(' ', '_');
    }

    /** Bukkit Sound key for a Hermes sound name. */
    public static String soundKey(String canonical) {
        String k = SOUND_KEYS.get(canonical);
        return k != null ? k : canonical.toUpperCase().replace(' ', '_');
    }

    /** Bukkit Particle key for a Hermes particle name. */
    public static String particleKey(String canonical) {
        String k = PARTICLE_KEYS.get(canonical);
        return k != null ? k : canonical.toUpperCase().replace(' ', '_');
    }

    /** Bukkit Biome key for a Hermes biome name. */
    public static String biomeKey(String canonical) {
        String k = BIOME_KEYS.get(canonical);
        return k != null ? k : canonical.toUpperCase().replace(' ', '_');
    }

    // ---------- suggestions ----------
    /** Returns the closest known word, or null if nothing is close. */
    public static String suggest(String name, String[] table) {
        String target = normalize(name);
        String best = null;
        int bestDist = 4;
        for (String word : table) {
            int d = levenshtein(target, word);
            if (d < bestDist) {
                bestDist = d;
                best = word;
            }
        }
        return best;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(prev[j] + 1, Math.min(cur[j - 1] + 1, prev[j - 1] + cost));
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }

    public static String[] itemNames() { return ITEMS.keySet().toArray(new String[0]); }
    public static String[] mobNames() { return MOBS.keySet().toArray(new String[0]); }
    public static String[] effectNames() { return EFFECTS.keySet().toArray(new String[0]); }
    public static String[] dimNames() { return DIMS.keySet().toArray(new String[0]); }
    public static String[] weatherNames() { return WEATHER.keySet().toArray(new String[0]); }
    public static String[] daypartNames() { return DAYPARTS.keySet().toArray(new String[0]); }
    public static String[] soundNames() { return SOUNDS.keySet().toArray(new String[0]); }
    public static String[] particleNames() { return PARTICLES.keySet().toArray(new String[0]); }
    public static String[] biomeNames() { return BIOMES.keySet().toArray(new String[0]); }
    public static String[] gamemodeNames() { return GAMEMODES.keySet().toArray(new String[0]); }

    /** All canonical item names (the friendly spellings). */
    public static List<String> canonicalItems() {
        List<String> out = new ArrayList<>();
        for (String v : ITEMS.values()) if (!out.contains(v)) out.add(v);
        return out;
    }
}

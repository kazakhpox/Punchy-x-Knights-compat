package net.punchyknights.devtools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;



/**
 * Builds a flat test world, dresses the player in Knights & Heraldry armour and looks at their first-person arms (Punchy!), saving screenshots along the way to the folder in the system property {@code handson.shots}. Then quits.
 */
@Mod("punchyknights_devtools")
public final class ShotDriver {
    private static final String WORLD = "handson_shots";
    
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    
    private record Step(String name, BooleanSupplier until, Runnable action) {
    }

    public ShotDriver(IEventBus modBus) {
        if (System.getProperty("handson.shots") == null) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        plan();
    }

    private void then(String name, int ticks, Runnable action) {
        steps.add(new Step(name, () -> true, action));
        steps.add(new Step(name + " wait", null, () -> wait = ticks));
    }

    private void when(String name, BooleanSupplier until, Runnable action) {
        steps.add(new Step(name, until, action));
    }

    private static final boolean[] VANILLA = {false};

    private void plan() {
        punchy.api.PunchyApi.useMinecraftHands("punchyknights_devtools", () -> VANILLA[0]);
        when("title", () -> Minecraft.getInstance().screen instanceof TitleScreen, this::createWorld);
        when("in world", () -> Minecraft.getInstance().player != null && Minecraft.getInstance().getSingleplayerServer() != null
                && Minecraft.getInstance().screen == null, () -> wait = 80);
        then("dress", 40, () -> dress(true));
        then("look", 40, () -> server(player -> player.teleportTo(player.serverLevel(), 0.5, -60, 0.5, 0.0F, 10.0F)));
        then("arms shot", 5, () -> shot("01_arms_plate"));
        then("sword", 40, () -> server(player -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(net.minecraft.world.item.Items.IRON_SWORD))));
        then("sword shot", 5, () -> shot("02_sword_plate"));
        then("vanilla hands", 30, () -> {
            VANILLA[0] = true;
            server(player -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY));
        });
        then("vanilla shot", 5, () -> shot("04_minecraft_arm_plate"));
        then("punchy again", 30, () -> VANILLA[0] = false);
        then("plain", 40, () -> dress(false));
        then("plain shot", 5, () -> shot("03_gambeson"));
        then("quit", 5, () -> Minecraft.getInstance().stop());
    }

    private static ItemStack kh(String name) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("knightsheraldry", name)));
    }

    /** A gambeson, with a plate cuirass and gauntlets put on it ({@code plates}), on the player's chest. */
    private static void dress(boolean plates) {
        server(player -> {
            ItemStack gambeson = kh("gambeson");
            if (plates) {
                var contents = new banduty.stoneycore.items.custom.armor.underarmor.UnderArmorContents.Mutable(
                        banduty.stoneycore.items.custom.armor.underarmor.UnderArmorContents.EMPTY);
                for (String part : new String[]{"plate_cuirass", "gauntlet"}) {
                    ItemStack left = contents.tryInsert(kh(part), player, gambeson);
                    System.out.println("[shots] put on " + part + ", left " + left);
                }
                gambeson.set(banduty.stoneycore.data.SCDataComponents.UNDER_ARMOR_CONTENTS.get(), contents.toImmutable());
            }
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, gambeson);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            System.out.println("[shots] chest " + gambeson + " " + gambeson.getComponentsPatch());
        });
    }

    private void tick() {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.peek();
        if (step == null) {
            return;
        }
        if (step.until() == null) {
            steps.poll();
            step.action().run();
            return;
        }
        if (step.until().getAsBoolean()) {
            steps.poll();
            System.out.println("[shots] " + step.name());
            step.action().run();
        }
    }

    private void createWorld() {
        Minecraft mc = Minecraft.getInstance();
        Path saves = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        try {
            if (Files.exists(saves)) {
                try (var walk = Files.walk(saves)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions(), mc.screen);
    }

    /** A right click at what the crosshair is on, with an empty hand: the player uses the anvil as anybody would. */
    private static void use() {
        Minecraft mc = Minecraft.getInstance();
        mc.player.getInventory().selected = 4;
        System.out.println("[shots] looking at " + mc.hitResult);
        try {
            Method method = Minecraft.class.getDeclaredMethod("startUseItem");
            method.setAccessible(true);
            method.invoke(mc);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void server(java.util.function.Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().get(0)));
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        File dir = new File(System.getProperty("handson.shots"));
        dir.mkdirs();
        Screenshot.grab(dir, name + ".png", mc.getMainRenderTarget(), message -> System.out.println("[shots] " + message.getString()));
    }
}

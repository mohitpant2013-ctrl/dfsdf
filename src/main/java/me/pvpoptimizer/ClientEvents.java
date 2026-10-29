package me.pvpoptimizer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.minecart.TNTMinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClientEvents {
    private static final KeyBinding TOGGLE =
            new KeyBinding("key.pvpoptimizer.toggle", GLFW.GLFW_KEY_UNKNOWN, "key.categories.pvpoptimizer");
    private static final List<BlockPos> PENDING_ANCHORS = new ArrayList<>();

    public static void init() {
        ClientRegistry.registerKeyBinding(TOGGLE);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        MinecraftForge.EVENT_BUS.register(new PerformanceEvents());
    }

    private static boolean active() {
        return Config.ENABLED.get();
    }

    // ---------------- Crystal optimizer ----------------
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        PlayerEntity player = event.getPlayer();
        if (!player.level.isClientSide || !active() || !Config.CRYSTAL.get()) return;
        Entity target = event.getTarget();
        if (!(target instanceof EnderCrystalEntity) || !target.isAlive()) return;

        World world = player.level;
        double x = target.getX(), y = target.getY(), z = target.getZ();
        target.remove();
        world.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
        world.playLocalSound(x, y, z, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F,
                (1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F, false);
    }

    // ---------------- Anchor optimizer ----------------
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isClientSide || !active() || !Config.ANCHOR.get()) return;
        if (event.getHand() != Hand.MAIN_HAND) return;
        World world = event.getWorld();
        if (world.dimension() == World.NETHER) return; // anchors only explode outside the Nether
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        if (!state.is(Blocks.RESPAWN_ANCHOR)) return;

        int charges = state.getValue(RespawnAnchorBlock.CHARGE);
        boolean holdingGlowstone = event.getPlayer().getItemInHand(Hand.MAIN_HAND).getItem() == Items.GLOWSTONE;
        boolean willCharge = holdingGlowstone && charges < 4;
        if (charges > 0 && !willCharge) {
            PENDING_ANCHORS.add(pos.immutable());
        }
    }

    @SubscribeEvent
    public void onRenderTick(net.minecraftforge.event.TickEvent.RenderTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START || PENDING_ANCHORS.isEmpty()) return;
        World world = Minecraft.getInstance().level;
        if (world != null) {
            for (BlockPos pos : PENDING_ANCHORS) {
                if (world.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) {
                    world.setBlock(pos, Blocks.FERN.defaultBlockState(), 11);
                }
            }
        }
        PENDING_ANCHORS.clear();
    }

    // ---------------- Cart PVP ----------------
    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        PlayerEntity player = event.getPlayer();
        World world = player.level;
        if (!world.isClientSide || !active() || !Config.CART.get()) return;
        if (BowItem.getPowerForTime(event.getCharge()) < 0.1F) return;
        if (player.getProjectile(event.getBow()).isEmpty() && !player.abilities.instabuild) return;

        Vector3d start = player.getEyePosition(1.0F);
        Vector3d end = start.add(player.getLookAngle().scale(64.0D));

        RayTraceResult block = world.clip(new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
        if (block.getType() != RayTraceResult.Type.MISS) end = block.getLocation();

        TNTMinecartEntity closest = null;
        double best = Double.MAX_VALUE;
        for (TNTMinecartEntity cart : world.getEntitiesOfClass(TNTMinecartEntity.class,
                new AxisAlignedBB(start, end).inflate(1.0D))) {
            java.util.Optional<Vector3d> hit = cart.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(hit.get());
                if (d < best) { best = d; closest = cart; }
            }
        }
        if (closest != null) closest.remove();
    }

    // ---------------- Toggle key ----------------
    @SubscribeEvent
    public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        while (TOGGLE.consumeClick()) {
            boolean now = !Config.ENABLED.get();
            Config.ENABLED.set(now);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(new StringTextComponent("PVP Optimizer: " + (now ? "ON" : "OFF")), true);
            }
        }
    }
}

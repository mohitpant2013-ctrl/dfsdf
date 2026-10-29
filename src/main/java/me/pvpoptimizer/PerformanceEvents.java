package me.pvpoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Entity culling done purely with Forge events (no mixins) so it stays OptiFine-compatible. */
public class PerformanceEvents {
    // entity id -> {lastCheckTick, visible(1/0)}
    private final Map<Integer, long[]> cache = new HashMap<>();

    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        if (!Config.ENABLED.get() || !Config.ENTITY_CULLING.get()) return;
        Minecraft mc = Minecraft.getInstance();
        LivingEntity e = event.getEntity();
        if (mc.level == null || e == mc.player || e == mc.getCameraEntity()) return;

        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        double dsq = e.distanceToSqr(cam);
        if (dsq < 16.0D) return; // never cull anything within 4 blocks

        int max = Config.FAR_ENTITY_DISTANCE.get();
        if (!(e instanceof PlayerEntity) && max > 0 && dsq > (double) max * max) {
            event.setCanceled(true);
            return;
        }
        if (!isVisible(mc.level, e, cam)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onNameplate(RenderNameplateEvent event) {
        int max = Config.NAMEPLATE_DISTANCE.get();
        if (!Config.ENABLED.get() || max <= 0) return;
        Entity e = event.getEntity();
        if (e instanceof PlayerEntity) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return;
        if (e.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > (double) max * max) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { cache.clear(); return; }
        long now = mc.level.getGameTime();
        if (now % 200 == 0) {
            Iterator<long[]> it = cache.values().iterator();
            while (it.hasNext()) if (now - it.next()[0] > 100) it.remove();
        }
    }

    private boolean isVisible(World level, Entity e, Vector3d cam) {
        long now = level.getGameTime();
        long[] c = cache.get(e.getId());
        if (c != null && now - c[0] < Config.CULL_INTERVAL.get()) return c[1] == 1;

        double x = e.getX(), y = e.getY(), z = e.getZ();
        double h = e.getBbHeight(), hw = e.getBbWidth() * 0.5D;
        double[][] pts = {
                {x, y + h * 0.5D, z}, {x, y + e.getEyeHeight(), z}, {x, y + 0.1D, z},
                {x + hw, y + h * 0.5D, z + hw}, {x - hw, y + h * 0.5D, z - hw}
        };
        boolean visible = false;
        for (double[] p : pts) {
            if (!blocked(level, cam, new Vector3d(p[0], p[1], p[2]))) { visible = true; break; }
        }
        if (c == null) { c = new long[2]; cache.put(e.getId(), c); }
        c[0] = now; c[1] = visible ? 1 : 0;
        return visible;
    }

    /** Conservative march: only fully opaque cubes block; thin gaps count as visible. */
    private boolean blocked(World level, Vector3d from, Vector3d to) {
        Vector3d d = to.subtract(from);
        double len = d.length();
        int steps = (int) Math.ceil(len / 0.5D);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 1; i < steps - 1; i++) {
            double t = (double) i / steps;
            pos.set(from.x + d.x * t, from.y + d.y * t, from.z + d.z * t);
            if (level.getBlockState(pos).isSolidRender(level, pos)) return true;
        }
        return false;
    }
}

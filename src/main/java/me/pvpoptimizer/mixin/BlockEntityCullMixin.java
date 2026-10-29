package me.pvpoptimizer.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.pvpoptimizer.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.BeaconTileEntity;
import net.minecraft.tileentity.EndGatewayTileEntity;
import net.minecraft.tileentity.EndPortalTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips rendering of far-away block entities (chests, signs, banners...).
 * Single HEAD inject, require=0: if OptiFine (or anything) changed the target, it is silently skipped.
 */
@Mixin(TileEntityRendererDispatcher.class)
public class BlockEntityCullMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends TileEntity> void pvpopt$cull(E tile, float partialTicks, MatrixStack matrix,
                                                    IRenderTypeBuffer buffer, CallbackInfo ci) {
        try {
            if (!Config.ENABLED.get()) return;
            int max = Config.TILE_ENTITY_DISTANCE.get();
            if (max <= 0) return;
            if (tile instanceof BeaconTileEntity || tile instanceof EndGatewayTileEntity
                    || tile instanceof EndPortalTileEntity) return; // long-range visuals
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer == null) return;
            Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
            BlockPos p = tile.getBlockPos();
            double dx = p.getX() + 0.5D - cam.x, dy = p.getY() + 0.5D - cam.y, dz = p.getZ() + 0.5D - cam.z;
            if (dx * dx + dy * dy + dz * dz > (double) max * max) ci.cancel();
        } catch (Throwable ignored) {
            // never crash rendering
        }
    }
}

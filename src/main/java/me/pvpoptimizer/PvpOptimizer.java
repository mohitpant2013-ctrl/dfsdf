package me.pvpoptimizer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(PvpOptimizer.MOD_ID)
public class PvpOptimizer {
    public static final String MOD_ID = "pvpoptimizer";

    public PvpOptimizer() {
        // Client-only mod: lets you join servers that don't have it
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEvents::init);
    }
}

package com.shulkerfeed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShulkerFeedMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("shulker-feed");
    private static int tickCounter = 0;

    @Override
    public void onInitialize() {
        // 每 20 tick（约 1 秒）检查一次所有在线玩家手持的方块，自动从潜影盒补料
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            tickCounter++;
            if (tickCounter % 20 != 0) return;
            for (var player : server.getPlayerList().getPlayers()) {
                ShulkerFeeder.feed(player);
            }
        });
        LOGGER.info("[Shulker Feed] 已加载：自动从潜影盒补充建造材料");
    }
}

package de.rapha149.clearfog.version;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class Wrapper26_R1 implements VersionWrapper {

    @Override
    public List<ChannelPipeline> getServerPipelines() throws NoSuchFieldException, IllegalAccessException {
        ServerConnectionListener conn = ((CraftServer) Bukkit.getServer()).getServer().getConnection();
        Field field = ServerConnectionListener.class.getDeclaredField("channels");
        field.setAccessible(true);
        List<ChannelFuture> channels = (List<ChannelFuture>) field.get(conn);
        field.setAccessible(false);
        return channels.stream().map(ChannelFuture::channel).map(Channel::pipeline).toList();
    }

    @Override
    public Class<?> getLoginSuccessPacketClass() {
        return ClientboundLoginFinishedPacket.class;
    }

    @Override
    public Class<?> getLoginPlayPacketClass() {
        return ClientboundLoginPacket.class;
    }

    @Override
    public Class<?> getUpdateViewDistanceClass() {
        return ClientboundSetChunkCacheRadiusPacket.class;
    }

    @Override
    public UUID getUUIDFromLoginPacket(Object obj) {
        if (!(obj instanceof ClientboundLoginFinishedPacket packet))
            throw new IllegalArgumentException("Parameter \"obj\" not instance of class " + ClientboundLoginFinishedPacket.class.getName());

        return packet.gameProfile().id();
    }

    @Override
    public int getViewDistanceFromPacket(Object obj) {
        if (obj instanceof ClientboundLoginPacket packet)
            return packet.chunkRadius();
        else if (obj instanceof ClientboundSetChunkCacheRadiusPacket packet)
            return packet.getRadius();
        else {
            throw new IllegalArgumentException("Parameter \"obj\" not instance of class " + ClientboundLoginPacket.class.getName() +
                    " nor instance of class " + ClientboundSetChunkCacheRadiusPacket.class.getName());
        }
    }

    @Override
    public Object replaceViewDistance(Object obj, int viewDistance) {
        if (obj instanceof ClientboundLoginPacket packet) {
            return new ClientboundLoginPacket(packet.playerId(), packet.hardcore(), packet.levels(), packet.maxPlayers(),
                    viewDistance, packet.simulationDistance(), packet.reducedDebugInfo(), packet.showDeathScreen(),
                    packet.doLimitedCrafting(), packet.commonPlayerSpawnInfo(), packet.enforcesSecureChat());
        } else if (obj instanceof ClientboundSetChunkCacheRadiusPacket) {
            return new ClientboundSetChunkCacheRadiusPacket(viewDistance);
        } else {
            throw new IllegalArgumentException("Parameter \"obj\" not instance of class " + ClientboundLoginPacket.class.getName() +
                    " nor instance of class " + ClientboundSetChunkCacheRadiusPacket.class.getName());
        }
    }

    @Override
    public void updateViewDistance(Player player, int viewDistance, boolean directUpdate) {
        ServerPlayer p = ((CraftPlayer) player).getHandle();
        p.connection.send(new ClientboundSetChunkCacheRadiusPacket(viewDistance));

        if (directUpdate) {
            ChunkMap map = p.level().getChunkSource().chunkMap;
            Location loc = player.getLocation();
            double x = loc.getX(), y = loc.getY(), z = loc.getZ();

            p.snapTo(x + 1000, y, z + 1000);
            map.move(p);
            p.snapTo(x, y, z);
            map.move(p);
        }
    }
}

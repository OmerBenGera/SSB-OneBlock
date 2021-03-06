package com.bgsoftware.ssboneblock.nms;

import com.mojang.brigadier.StringReader;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.commands.arguments.ArgumentNBTTag;
import net.minecraft.commands.arguments.blocks.ArgumentBlock;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;

public final class NMSAdapter_v1_17_R1 implements NMSAdapter {

    @Override
    public boolean isLegacy() {
        return false;
    }

    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    @Override
    public void setChestName(Location chest, String name) {
        assert chest.getWorld() != null;
        World world = ((CraftWorld) chest.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(chest.getBlockX(), chest.getBlockY(), chest.getBlockZ());
        TileEntityChest tileEntityChest = (TileEntityChest) world.getTileEntity(blockPosition);
        assert tileEntityChest != null;
        tileEntityChest.setCustomName(CraftChatMessage.fromString(name)[0]);
    }

    @Override
    public void setBlock(Location location, Material type, byte data, String nbt) {
        assert location.getWorld() != null;

        WorldServer worldServer = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        worldServer.removeTileEntity(blockPosition);

        location.getBlock().setType(type);

        if(nbt != null) {
            try {
                ArgumentBlock argumentBlock = new ArgumentBlock(new StringReader(nbt), false).a(true);
                ArgumentTileLocation tileLocation = new ArgumentTileLocation(argumentBlock.getBlockData(), argumentBlock.getStateMap().keySet(), argumentBlock.c());
                tileLocation.a(worldServer, blockPosition, 2);
                worldServer.update(blockPosition, tileLocation.a().getBlock());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void applyNBTToEntity(org.bukkit.entity.LivingEntity bukkitEntity, String nbt) {
        try {
            NBTTagCompound tagCompound = ArgumentNBTTag.a().parse(new StringReader(nbt));
            ((CraftLivingEntity) bukkitEntity).getHandle().loadData(tagCompound);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void simulateToolBreak(Player bukkitPlayer, org.bukkit.block.Block bukkitBlock) {
        EntityPlayer entityPlayer = ((CraftPlayer) bukkitPlayer).getHandle();

        ItemStack itemStack = entityPlayer.getItemInMainHand();

        WorldServer worldServer = ((CraftWorld) bukkitBlock.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ());
        IBlockData blockData = worldServer.getType(blockPosition);

        itemStack.a(worldServer, blockData, blockPosition, entityPlayer);
    }

}


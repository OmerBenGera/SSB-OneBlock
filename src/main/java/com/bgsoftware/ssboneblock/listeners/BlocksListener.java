package com.bgsoftware.ssboneblock.listeners;

import com.bgsoftware.ssboneblock.OneBlockPlugin;
import com.bgsoftware.ssboneblock.utils.LocationUtils;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BlocksListener implements Listener {

    private final Set<Location> brokenBlocks = new HashSet<>();
    private final Set<Location> recentlyBroken = new HashSet<>();
    private final OneBlockPlugin plugin;

    public BlocksListener(OneBlockPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOneBlockBreak(BlockBreakEvent e){
        Island island = SuperiorSkyblockAPI.getIslandAt(e.getBlock().getLocation());

        if(island == null || island.isSpawn() || !LocationUtils.getOneBlock(island).equals(e.getBlock().getLocation()))
            return;

        Block block = e.getBlock();
        Location blockLocation = block.getLocation();

        brokenBlocks.add(blockLocation);
        e.setCancelled(true);

        if(!recentlyBroken.add(blockLocation))
            return;

        Block underBlock = block.getRelative(BlockFace.DOWN);
        boolean barrierPlacement = underBlock.getType() == Material.AIR;

        if(barrierPlacement)
            underBlock.setType(Material.BARRIER);

        ItemStack inHandItem = e.getPlayer().getItemInHand();
        blockLocation.add(0, 1, 0);
        World blockWorld = block.getWorld();

        Collection<ItemStack> drops = block.getDrops(inHandItem);

        if(block.getState() instanceof InventoryHolder)
            Collections.addAll(drops, ((InventoryHolder) block.getState()).getInventory().getContents());

        drops.stream().filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
                .forEach(itemStack -> blockWorld.dropItemNaturally(blockLocation, itemStack));

        if(e.getExpToDrop() > 0) {
            ExperienceOrb orb = blockWorld.spawn(blockLocation, ExperienceOrb.class);
            orb.setExperience(e.getExpToDrop());
        }

        if(inHandItem != null)
            plugin.getNMSAdapter().simulateToolBreak(e.getPlayer(), e.getBlock());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getPhasesHandler().runNextAction(island, e.getPlayer());

            recentlyBroken.remove(blockLocation);

            if(barrierPlacement)
                underBlock.setType(Material.AIR);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOneBlockPhysics(BlockPhysicsEvent e){
        Island island = SuperiorSkyblockAPI.getIslandAt(e.getBlock().getLocation());

        if(island != null && !island.isSpawn() && LocationUtils.getOneBlock(island).equals(e.getBlock().getLocation()) &&
                !brokenBlocks.remove(e.getBlock().getLocation())) {
            if(e.getChangedType() == Material.AIR)
                Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getPhasesHandler().runNextAction(island, null), 20L);
        }
    }

}

package cc.thedudeguy.xpinthejar.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.player.SpoutPlayer;

import cc.thedudeguy.xpinthejar.XPInTheJar;
import cc.thedudeguy.xpinthejar.util.Debug;

public class BottleListener implements Listener {

    /**
     * Handle Bottling Experience when holding a bottle while picking up Experience
     *
     * @param event
     */
    @EventHandler
    public void onBottleExp(PlayerExpChangeEvent event) {
        Debug.debug(event.getPlayer(), "Xp Pickup");
        Debug.debug(event.getPlayer(), "Amount: ", event.getAmount());

        ItemStack item = event.getPlayer().getItemInHand();

        if (!item.getType().equals(Material.GLASS_BOTTLE)) {
            return;
        }
        if (XPInTheJar.instance.getConfig().getBoolean("bottleRequireCrouch") && !event.getPlayer().isSneaking()) {
            return;
        }
        if (item.getAmount() > 1) {
            event.getPlayer().sendMessage("Your holding too many bottles to collect XP, try holding just one.");
            return;
        }

        XPInTheJar.setXpStored(item, XPInTheJar.getXpStored(item) + event.getAmount());

        if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
            ((SpoutPlayer)event.getPlayer()).sendNotification(event.getAmount() + "xp Collected", XPInTheJar.getXpStored(item) + "xp", Material.GLASS_BOTTLE);
        } else {
            event.getPlayer().sendMessage("Collected " + event.getAmount() + " xp for a total of " + XPInTheJar.getXpStored(item));
        }
        event.setAmount(0);
    }

    /**
     * Handle bottle right click to consume xp stored in it
     * @param event
     */
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if(!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }

        ItemStack item = event.getItem();
        if(item == null || !item.getType().equals(Material.GLASS_BOTTLE) || XPInTheJar.getXpStored(item) <= 0) {
            return;
        }
        if (item.getAmount() != 1) {
            event.getPlayer().sendMessage("You are holding too many XP Bottles, try holding just one");
            return;
        }

        event.getPlayer().giveExp(XPInTheJar.getXpStored(item));

        if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
            ((SpoutPlayer) event.getPlayer()).sendNotification( "Exp Bottle Emptied", XPInTheJar.getXpStored(item) + "xp", Material.GLASS_BOTTLE);
        } else {
            event.getPlayer().sendMessage(XPInTheJar.getXpStored(item) + "xp emptied into your gut-hole");
        }

        if (XPInTheJar.instance.getConfig().getBoolean("consumeBottleOnUse")) {
            event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
        } else {
            XPInTheJar.setXpStored(item, 0);
        }
    }

}

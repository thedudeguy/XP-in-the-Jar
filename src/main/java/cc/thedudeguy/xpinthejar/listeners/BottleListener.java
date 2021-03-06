package cc.thedudeguy.xpinthejar.listeners;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.getspout.spoutapi.player.SpoutPlayer;

import cc.thedudeguy.xpinthejar.XPInTheJar;
import cc.thedudeguy.xpinthejar.util.Debug;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketConstructor;

public class BottleListener implements Listener {

    /**
     * Handle Bottling Experience when holding a bottle while picking up Experience
     *
     * @param event
     */
    @EventHandler
    public void onBottleExp(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if(!player.hasPermission("xpjar.bottle.collect")) {
            return;
        }

        Debug.debug(player, "Xp Pickup");
        Debug.debug(player, "Amount: ", event.getAmount());

        // orb must have positive xp value
        // might not be necessary but a failsafe
        if (event.getAmount() < 1) {
            return;
        }

        if (XPInTheJar.instance.getConfig().getBoolean("bottleRequireCrouch") && !player.isSneaking()) {
            return;
        }

        ItemStack item = player.getItemInHand();

        //only glass bottle and potions
        if (!item.getType().equals(Material.GLASS_BOTTLE) && !item.getType().equals(Material.POTION)) {
            return;
        }

        if (item.getAmount() > 1) {
            player.sendMessage("Your holding too many bottles to collect XP, try holding just one.");
            return;
        }

        //only water potion if potion
        if (item.getType().equals(Material.POTION)) {
            if(!XPInTheJar.isItemXPBottle(item)) {
                return;
            }
        }

        // convert glass bottle to water potion. and/or add xp amount to meta
        if (item.getType().equals(Material.GLASS_BOTTLE)) {
            item = new ItemStack(Material.POTION, 1);
            PotionMeta potionMeta = (PotionMeta)item.getItemMeta();
            potionMeta.setDisplayName(XPInTheJar.xpBottleName);
            item.setItemMeta(potionMeta);
            XPInTheJar.setXpStored(item, event.getAmount());
            player.setItemInHand(item);
        } else {
            XPInTheJar.setXpStored(item, XPInTheJar.getXpStored(item) + event.getAmount());
        }

        if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
            ((SpoutPlayer)player).sendNotification(event.getAmount() + "xp Collected", XPInTheJar.getXpStored(item) + "xp", Material.GLASS_BOTTLE);
        } else {
            player.sendMessage("Collected " + event.getAmount() + " xp for a total of " + XPInTheJar.getXpStored(item));
        }

        // play arm swing animation using protocol lib
        if (XPInTheJar.instance.protocolLibEnabled) {
            animatePlayerArm(event.getPlayer());
        }
        event.setAmount(0);
    }

    /**
     * Soak up the EXP when drinking the water bottle
     * @param event
     */
    @EventHandler
    public void onDrinkBottle(PlayerItemConsumeEvent event) {
        //e.getPlayer().sendMessage("Consumed!");
        Player player = event.getPlayer();

        if(!player.hasPermission("xpjar.bottle.use")) {
            return;
        }

        ItemStack item = event.getItem();

        //must be xp bottle - water potion
        if(!XPInTheJar.isItemXPBottle(item)) {
            return;
        }

        //must have xp (failsafe?)
        if (XPInTheJar.getXpStored(item) <= 0) {
            return;
        }

        int amount = XPInTheJar.getXpStored(item);
        //omnomnom
        player.giveExp(amount);

        //cancel original event
        event.setCancelled(true);

        if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
            ((SpoutPlayer) player).sendNotification( "Exp Bottle Emptied", String.valueOf(amount) + "xp", Material.GLASS_BOTTLE);
        } else {
            player.sendMessage(String.valueOf(amount) + "xp emptied into your gut-hole");
        }

        if (XPInTheJar.instance.getConfig().getBoolean("consumeBottleOnUse")) {
            player.setItemInHand(new ItemStack(Material.AIR));
        } else {
            player.setItemInHand(new ItemStack(Material.GLASS_BOTTLE));
        }
    }

    /**
     * Animates a players arm to swing using the ProtocolLib packet sending.
     * @param player
     */
    private void animatePlayerArm(Player player) {
        Debug.debug("playing arm swing animation");

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketConstructor animateConstruct = protocolManager.createPacketConstructor(PacketType.Play.Client.ARM_ANIMATION, player, 1);
        PacketContainer animatePacket = animateConstruct.createPacket(player, 1);
        try {
            protocolManager.sendServerPacket(player, animatePacket);
        } catch (InvocationTargetException e) {
            Debug.debug("Error sending arm animation packet.");
            //e.printStackTrace();
        }
    }

}

package cc.thedudeguy.xpinthejar.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.player.SpoutPlayer;

import cc.thedudeguy.xpinthejar.XPInTheJar;
import cc.thedudeguy.xpinthejar.databases.Bank;
import cc.thedudeguy.xpinthejar.util.Debug;

	public class BankListener implements Listener {

	/**
	 * To fix the issue where enchanting doesnt update a players Total Experience (for getTotalExperience()),
	 * causing an issue the next time you try to deposit Exp into an xp bank.
	 * @param event
	 */
	@EventHandler
	public void onItemEnchant(EnchantItemEvent event) {
		Debug.debug(event.getEnchanter(), "Item Enchanted");
		
		int expCost = XPInTheJar.calculateLevelToExp(event.getExpLevelCost());
		Debug.debug(event.getEnchanter(), "Total Exp cost is ", expCost);
		event.getEnchanter().setTotalExperience(event.getEnchanter().getTotalExperience() - expCost);
	}
	
	/**
	 * Handle returning the xp in the bank block to the user who broke the block, so it is not lost forevor
	 * 
	 * @param event
	 */
	@EventHandler
	public void onBlockDestoy(BlockBreakEvent event) {
		if (event.getBlock().getType().equals(Material.DIAMOND_BLOCK)) {
			Block bankBlock = event.getBlock();
			Bank bank = getBank(bankBlock);
			Player player = event.getPlayer();
			
			if (bank == null || bank.getXp() < 1) return;
			
			
			
			if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
				((SpoutPlayer)event.getPlayer()).sendNotification( "Exp Bank Destroyed", "Retrieved " + String.valueOf(bank.getXp()) + "xp", Material.GLASS_BOTTLE);
			} else {
				player.sendMessage("Retrieved " + String.valueOf(bank.getXp()) + "xp");
			}
			
			player.giveExp(bank.getXp());
			bank.setXp(0);
			XPInTheJar.instance.getDatabase().save(bank);
			
		 }
	 }
	
	/**
	 * Handle Block interaction with a Bank Block or a Deposit Block.
	 * @param event
	 */
	 @EventHandler
	 public void onBlockInteract(PlayerInteractEvent event) {
		 
		 if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			 
			 /*
			  * If we are interacting with a cauldron, we want to deposit xp
			  */
			 if (event.getClickedBlock().getType().equals(Material.CAULDRON)) {
				 
				 //get a connected bank.
				 Block bankBlock = getConnectedBankBlock(event.getClickedBlock());
				 
				 if (bankBlock == null) return;
				 
				 Bank bank = getBank(bankBlock, true);
				 Player player = event.getPlayer();
				 
				 if (bank==null) return;
				 
				 //if we have a bottle than we should empty the contents
				 //of the bottle into the cauldron.
				 if (
						player.getItemInHand() != null &&
						player.getItemInHand().getType().equals(Material.GLASS_BOTTLE) &&
						player.getItemInHand().getDurability() > 0
						) {
					if (player.getItemInHand().getAmount() > 1) {
						player.sendMessage("You are holding too many bottles, try holding just one");
						return;
					}
					int toDeposit = player.getItemInHand().getDurability();
					bank.add(toDeposit);
					 
					if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
						((SpoutPlayer)event.getPlayer()).sendNotification( "Bottle Deposited", String.valueOf(toDeposit) + "xp", Material.GLASS_BOTTLE);
					} else {
						player.sendMessage("Deposited Bottle:" + String.valueOf(toDeposit) + "xp");
					}
					
					if (XPInTheJar.instance.getConfig().getBoolean("consumeBottleOnDeposit")) {
						player.setItemInHand(new ItemStack(Material.AIR));
					} else {
						player.getItemInHand().setDurability((short)0);
					}
				} 
				 
				//no bottle in hand, deposit from player.
				else {
					 
					if (player.getTotalExperience() < 1) return;
					 
					/*
					if getExp() is 0, than the player is exactly at a level
					in which we can sequencially remove levels with precision.
					otherwise, a partial level has been obtained, and we will go ahead and deposit that
					partial amount. If a player has no exp, and is level 0, this part of the
					code cannot be reached since it will have already returned, so we dont have to worry
					about taking more than possible.
					*/
					if ( player.getExp() == 0 ) {
						//here the player is at an exact level, so we can start subtracting exp
						//by the level
						int toDeposit = player.getTotalExperience() - (XPInTheJar.calculateLevelToExp(player.getLevel()-1));
						bank.add(toDeposit);
						player.setTotalExperience(player.getTotalExperience()-toDeposit);
						player.setLevel(player.getLevel()-1);
						player.setExp(0);
						 
						if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
							((SpoutPlayer)event.getPlayer()).sendNotification( "Exp Deposited", String.valueOf(toDeposit) + "xp", Material.GLASS_BOTTLE);
						} else {
							player.sendMessage("Exp Deposited:" + String.valueOf(toDeposit) + "xp");
						}
						
					} else {
						 
						//calculate how much xp into the level they have, and thats what will be deposited
						int toDeposit = player.getTotalExperience() - (XPInTheJar.calculateLevelToExp(player.getLevel()));
						bank.add(toDeposit);
						player.setTotalExperience(XPInTheJar.calculateLevelToExp(player.getLevel()));
						player.setLevel(player.getLevel());
						player.setExp(0);
						 
						if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
							((SpoutPlayer)event.getPlayer()).sendNotification( "Exp Deposited", String.valueOf(toDeposit) + "xp", Material.GLASS_BOTTLE);
						} else {
							player.sendMessage("Exp Deposited:" + String.valueOf(toDeposit) + "xp");
						}
					 }
				 
				 }
				 
				 XPInTheJar.instance.getDatabase().save(bank);
				 updateSigns(bankBlock, bank);
				 return;
				 
			/*
			If we are interacting with a diamond block, we want to withdraw xp
			*/
			} else if (event.getClickedBlock().getType().equals(Material.DIAMOND_BLOCK)) {
				
				Block bankBlock = event.getClickedBlock();
				Player player = event.getPlayer();
				Bank bank = getBank(bankBlock);
				
				if (bank == null || bank.getXp() < 1) return;
				 
				//we will set the toWithdrawel to be what is required for the player to level up.
				int toWithdraw = XPInTheJar.calculateLevelToExp(player.getLevel()+1) - player.getTotalExperience();
				 
				if (bank.getXp() < toWithdraw) {
					toWithdraw = bank.getXp();
					bank.deduct(toWithdraw);
					player.giveExp(toWithdraw);
					 
					if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
						((SpoutPlayer)event.getPlayer()).sendNotification( "Exp Withdrawn", String.valueOf(toWithdraw) + "xp", Material.GLASS_BOTTLE);
					} else {
						player.sendMessage("Exp Withdrawn:" + String.valueOf(toWithdraw) + "xp");
					}
				} else {
					bank.deduct(toWithdraw);
					player.giveExp(toWithdraw);
					if(XPInTheJar.instance.spoutEnabled && ((SpoutPlayer)event.getPlayer()).isSpoutCraftEnabled()) {
						((SpoutPlayer)event.getPlayer()).sendNotification( "Exp Withdrawn", String.valueOf(toWithdraw) + "xp", Material.GLASS_BOTTLE);
					} else {
						player.sendMessage("Exp Withdrawn:" + String.valueOf(toWithdraw) + "xp");
					}
				}
				
				XPInTheJar.instance.getDatabase().save(bank);
				updateSigns(bankBlock, bank);
				return;
			}
		} 
	}
	 
	 public Bank getBank(Block bankBlock) {
		 return getBank(bankBlock, false);
	 }
	 
	 public Bank getBank(Block bankBlock, boolean createIfNone) {
		 
		Bank bank = XPInTheJar.instance.getDatabase().find(Bank.class)
				.where()
					.eq("x", bankBlock.getX())
					.eq("y", bankBlock.getY())
					.eq("z", bankBlock.getZ())
					.ieq("worldName", bankBlock.getWorld().getName())
				.findUnique();
		
		if (bank == null) {
			if (createIfNone == true) {
				bank = new Bank();
				bank.setX(bankBlock.getX());
				bank.setY(bankBlock.getY());
				bank.setZ(bankBlock.getZ());
				bank.setWorldName(bankBlock.getWorld().getName());
				bank.setXp(0);
				return bank;
			}
			
			return null;
		}
		
		return bank;
	 }
	 
	 public void updateSigns(Block bankBlock, Bank bank) {
		 
		 if (bank == null || bankBlock == null) return;
		 
		 BlockFace[] blockFaces = {BlockFace.SELF, BlockFace.DOWN, BlockFace.UP, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH};
		 
		 for(BlockFace bf : blockFaces) {
			 Block relBlock = bankBlock.getRelative(bf);
			 if (
					 relBlock.getType().equals(Material.SIGN) || 
					 relBlock.getType().equals(Material.SIGN_POST) ||
					 relBlock.getType().equals(Material.WALL_SIGN)
					 ) {
				 
				 Sign sign = (Sign)relBlock.getState();
				 sign.setLine(0, "");
				 sign.setLine(1, "XP Bank");
				 sign.setLine(2, String.valueOf(bank.getXp()));
				 sign.setLine(3, "");
				 sign.update(true);
			 }
		 }
	 }
	 
	 public Block getConnectedBankBlock(Block bankInputBlock) {
		 
		 if (bankInputBlock.getRelative(BlockFace.UP).getType().equals(Material.DIAMOND_BLOCK)) {
			 return bankInputBlock.getRelative(BlockFace.UP);
		 }
		 else if (bankInputBlock.getRelative(BlockFace.DOWN).getType().equals(Material.DIAMOND_BLOCK)) {
			 return bankInputBlock.getRelative(BlockFace.DOWN);
		 }
		 else if (bankInputBlock.getRelative(BlockFace.EAST).getType().equals(Material.DIAMOND_BLOCK)) {
			 return bankInputBlock.getRelative(BlockFace.EAST);
		 }
		 else if (bankInputBlock.getRelative(BlockFace.WEST).getType().equals(Material.DIAMOND_BLOCK)) {
			 return bankInputBlock.getRelative(BlockFace.WEST);
		 }
		 else if (bankInputBlock.getRelative(BlockFace.NORTH).getType().equals(Material.DIAMOND_BLOCK)) {
			 return bankInputBlock.getRelative(BlockFace.NORTH);
		 }
		 else if (bankInputBlock.getRelative(BlockFace.SOUTH).getType().equals(Material.DIAMOND_BLOCK)) {
			 return bankInputBlock.getRelative(BlockFace.SOUTH);
		 }
		 return null;
	 }
	
}

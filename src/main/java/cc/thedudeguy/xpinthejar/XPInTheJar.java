/**
 * Copyright (C) 2011  Chris Churchwell
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package cc.thedudeguy.xpinthejar;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.Bukkit;
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
import org.bukkit.plugin.java.JavaPlugin;

import cc.thedudeguy.xpinthejar.databases.Bank;

public class XPInTheJar extends JavaPlugin implements Listener {
	
	public static XPInTheJar instance;
	
	public void onEnable()
	{
		instance = this;
		
		//ResourceManager.preLoginCache();
		setupDatabase();
		
		//register events
		this.getServer().getPluginManager().registerEvents(this, this);
		
		Bukkit.getLogger().log(Level.INFO, "[XP in the Jar] Enabled.");
	}
	
	public void onDisable()
	{
		Bukkit.getLogger().log(Level.INFO, "[XP in the Jar] Disabled.");
	}
	
	private void setupDatabase() {
		try {
            getDatabase().find(Bank.class).findRowCount();
        } catch (PersistenceException ex) {
            Bukkit.getLogger().log(Level.INFO, "[XP in the Jar] Installing database for " + getDescription().getName() + " due to first time usage");
            installDDL();
        }
	}
	
	@Override	
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> list = new ArrayList<Class<?>>();
	    list.add(Bank.class);
	    return list;
	}
		
	@EventHandler
	public void onBlockDestoy(BlockBreakEvent event) {
		if (event.getBlock().getType().equals(Material.DIAMOND_BLOCK)) {
			Block bankBlock = event.getBlock();
			Bank bank = getBank(bankBlock);
			Player player = event.getPlayer();
			
			if (bank == null || bank.getXp() < 1) return;
			
			addPlayerExp(player, bank.getXp());
			player.sendMessage("Retrieved " + String.valueOf(bank.getXp()) + "xp");
			bank.setXp(0);
			XPInTheJar.instance.getDatabase().save(bank);
			
		 }
	 }
	 
	 @EventHandler
	 public void onBlockInteract(PlayerInteractEvent event) {
		 
		 if (event.hasBlock() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			 
			 if (event.getClickedBlock().getType().equals(Material.CAULDRON)) {
				 
				 //get a connected bank.
				 Block bankBlock = getConnectedBankBlock(event.getClickedBlock());
				 
				 if (bankBlock == null) return;
				 
				 Bank bank = getBank(bankBlock, true);
				 Player player = event.getPlayer();
				 
				 if (bank==null || player.getTotalExperience() < 1) return;
				 
				 /*
				 if getExp() is 0, than the player is exactly at a level
				 in which we can sequencially remove levels with precision.
				 otherwise, a partial level has been obtained, and we will go ahead and deposit that
				 partial amount. If a player has no exp, and is level 0, this part of the
				 code cannot be reached since it will have already returned, so we dont have to worry
				 about taking more than possible.
				 */
				 if ( player.getExp() == 0 )
				 {
					 //here the player is at an exact level, so we can start subtracting exp
					 //by the level
					 int toDeposit = player.getTotalExperience() - (calculateLevelToExp(player.getLevel()-1));
					 bank.add(toDeposit);
					 player.setTotalExperience(player.getTotalExperience()-toDeposit);
					 player.setLevel(player.getLevel()-1);
					 player.setExp(0);
					 player.sendMessage("Deposited " + String.valueOf(toDeposit) + "xp to Bank");
				 } else {
					 
					 //calculate how much xp into the level they have, and thats what will be deposited
					 int toDeposit = player.getTotalExperience() - (calculateLevelToExp(player.getLevel()));
					 bank.add(toDeposit);
					 player.setTotalExperience(calculateLevelToExp(player.getLevel()));
					 player.setLevel(player.getLevel());
					 player.setExp(0);
					 player.sendMessage("Deposited " + String.valueOf(toDeposit) + "xp to Bank");
				 }
				 
				 XPInTheJar.instance.getDatabase().save(bank);
				 updateSigns(bankBlock, bank);
				 return;
				 
			 } else if (event.getClickedBlock().getType().equals(Material.DIAMOND_BLOCK)) {
				 
				 Block bankBlock = event.getClickedBlock();
				 Player player = event.getPlayer();
				 Bank bank = getBank(bankBlock);
				 
				 if (bank == null || bank.getXp() < 1) return;
				 
				 //we will set the toWithdrawel to be what is required for the player to level up.
				 int toWithdraw = calculateLevelToExp(player.getLevel()+1) - player.getTotalExperience();
				 player.sendMessage("Required XP to level: " + String.valueOf(toWithdraw));
				 
				 if (bank.getXp() < toWithdraw) {
					 toWithdraw = bank.getXp();
					 bank.deduct(toWithdraw);
					 player.giveExp(toWithdraw);
					 player.sendMessage("Withdrew " + String.valueOf(toWithdraw) + "xp from Bank");
				 } else {
					 bank.deduct(toWithdraw);
					 player.giveExp(toWithdraw);
					 player.sendMessage("Withdrew " + String.valueOf(toWithdraw) + "xp from Bank");
				 }
				 
				 XPInTheJar.instance.getDatabase().save(bank);
				 updateSigns(bankBlock, bank);
				 return;
			 }
		 }
		 
	 }
	 
	 public int calculateLevelToExp(int level) {
		 return calculateLevelToExp((float)level);
	 }
	 
	 public int calculateLevelToExp(float level) {
		 return (int)Math.round( (1.75 * (Math.pow(level, 2)) + ( 5 * level ) ));
	 }
	 
	 public void addPlayerExp(Player player, int amount) {
		 //setPlayerExp(player, player.getTotalExperience() + amount);
		 player.giveExp(amount);
	 }
	 public void deductPlayerExp(Player player, int amount) {
		 setPlayerExp(player, player.getTotalExperience() - amount);
	 }
	 
	 public void setPlayerExp(Player player, int xpTotal) {
		 
		 player.setTotalExperience(0);
		 player.setLevel(0);
		 player.setExp(0);
		 
		 player.giveExp(xpTotal);
		 
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

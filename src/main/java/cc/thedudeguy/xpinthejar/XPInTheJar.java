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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import cc.thedudeguy.xpinthejar.databases.Bank;
import cc.thedudeguy.xpinthejar.listeners.BankListener;
import cc.thedudeguy.xpinthejar.listeners.BottleListener;

public class XPInTheJar extends JavaPlugin {

    public static XPInTheJar instance;
    protected static String STACK_BY_DATA_FN = "a";

    public boolean spoutEnabled = false;

    /**
     * Calculates the exact amount of Experience in a given Level.
     *
     * @param Integer level - the level to get converted into an XP amount
     * @return Integer the Total xp in provided level
     */
    public static final int calculateLevelToExp(float level) {
        return (int)Math.round( (1.75 * (Math.pow(level, 2)) + ( 5 * level ) ));
    }

    /**
     * @see calculateLevelToExp
     * @param level
     * @return
     */
    public static final int calculateLevelToExp(int level) {
        return calculateLevelToExp((float)level);
    }

    /**
     * Actions to enable this plugin
     */
    public void onEnable()
    {
        instance = this;

        //copy the config
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        //ResourceManager.preLoginCache();
        setupDatabase();

        //register events
        if(getConfig().getBoolean("enableExpBank")) {
            getServer().getPluginManager().registerEvents(new BankListener(), this);
        }
        if(getConfig().getBoolean("enableXPBottles")) {
            getServer().getPluginManager().registerEvents(new BottleListener(), this);
        }

        //check for spout
        if (Bukkit.getPluginManager().isPluginEnabled("Spout")) {
            if (getConfig().getBoolean("enableSpout")) {
                Bukkit.getLogger().log(Level.INFO, "[XP In The Jar] Spout present. Enabling Spout features.");
                spoutEnabled = true;
            } else {
                Bukkit.getLogger().log(Level.INFO, "[XP In The Jar] Disabling Spout features even though Spout is present.");
            }
        }

        /* ******************************************************************************************************
         * This section was written by Nisovin, for the BookWorm plugin
         * which can be found at. http://code.google.com/p/nisovin-minecraft-bukkit-plugins/
         * I have modified it for use with XPInTheJar
         *
         */
        // prevent bottle stacking
        try {
            boolean ok = false;
            try {
                // attempt to make books with different data values stack separately
                Method method = net.minecraft.server.Item.class.getDeclaredMethod(STACK_BY_DATA_FN, boolean.class);
                if (method.getReturnType() == net.minecraft.server.Item.class) {
                    method.setAccessible(true);
                    method.invoke(net.minecraft.server.Item.GLASS_BOTTLE, true);
                    ok = true;
                }
            } catch (Exception e) {

            }
            if (!ok) {
                // otherwise limit stack size to 1
                Field field = net.minecraft.server.Item.class.getDeclaredField("maxStackSize");
                field.setAccessible(true);
                field.setInt(net.minecraft.server.Item.GLASS_BOTTLE, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
         * *****************************************************************************************************
         */

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
}

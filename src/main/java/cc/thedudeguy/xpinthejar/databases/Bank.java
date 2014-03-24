package cc.thedudeguy.xpinthejar.databases;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.bukkit.block.Block;

import cc.thedudeguy.xpinthejar.XPInTheJar;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name="xp_bank")
public class Bank {

    @Id
    private int id;

    @NotNull
    private int x;

    @NotNull
    private int y;

    @NotNull
    private int z;

    @NotEmpty
    private String worldName;

    @NotNull
    private int xp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public void add(int amount) {
        setXp(getXp() + amount);
    }

    public void deduct(int amount) {
        setXp(getXp() - amount);
    }

    /**
     * Checks if a block has a bank tied to it.
     * @param bankBlock
     * @return
     */
    public static boolean hasBank(Block bankBlock) {
        Bank bank = getBank(bankBlock, false);
        if (bank == null) {
            return false;
        }
        return true;
    }

    /**
     * Removes block's tied bank from the database
     * @param bankBlock
     */
    public static void destroyBank(Block bankBlock) {
        Bank bank = getBank(bankBlock, false);
        if (bank == null) {
            //nothing to do.
            return;
        }
        XPInTheJar.instance.getDatabase().delete(bank);
    }

    /**
     * Gets the balance of the bank tied to a block
     * @param bankBlock
     * @return
     */
    public static int getBankBalance(Block bankBlock) {
        Bank bank = getBank(bankBlock);
        return bank.getXp();
    }

    /**
     * Deducts an amount from a blocks tied bank
     * @param bankBlock
     * @param amount
     * @return the new amount the bank is holding
     */
    public static int deductFromBankBlock(Block bankBlock, int amount) {
        Bank bank = getBank(bankBlock);
        bank.deduct(amount);
        XPInTheJar.instance.getDatabase().save(bank);

        return bank.getXp();
    }

    /**
     * Adds an amount to a blocks tied bank
     * @param bankBlock
     * @param amount
     * @return - the new amount the bank is holding
     */
    public static int addToBankBlock(Block bankBlock, int amount) {
        Bank bank = getBank(bankBlock);
        bank.add(amount);
        XPInTheJar.instance.getDatabase().save(bank);

        return bank.getXp();
    }

    /**
     * Gets a bank tied to a block
     * Always returns a bank, even if a new blank one
     * @param bankBlock
     * @return
     */
    public static Bank getBank(Block bankBlock) {
        return getBank(bankBlock, true);
    }

    /**
     * Gets a tied bank to a block.
     * @param bankBlock
     * @param createIfNone - whether or not to return a new blank bank if one doesnt exist.
     * @return
     */
    public static Bank getBank(Block bankBlock, boolean createIfNone) {
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
            }
        }
        return bank;
    }
}

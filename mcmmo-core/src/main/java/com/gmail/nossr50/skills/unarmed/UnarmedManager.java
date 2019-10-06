package com.gmail.nossr50.skills.unarmed;

import com.gmail.nossr50.core.MetadataConstants;
import com.gmail.nossr50.datatypes.interactions.NotificationType;
import com.gmail.nossr50.datatypes.player.BukkitMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.skills.SkillActivationType;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UnarmedManager extends SkillManager {
    private long lastAttacked;
    private long attackInterval;
    public double berserkDamageModifier = 1.5;

    public UnarmedManager(mcMMO pluginRef, BukkitMMOPlayer mcMMOPlayer) {
        super(pluginRef, mcMMOPlayer, PrimarySkillType.UNARMED);
        initUnarmedPerPlayerVars();
    }

    /**
     * Inits variables used for each player for unarmed
     */
    private void initUnarmedPerPlayerVars() {
        lastAttacked = 0;
        attackInterval = 750;
    }

    public boolean canActivateAbility() {
        return mcMMOPlayer.getToolPreparationMode(ToolType.FISTS) && pluginRef.getPermissionTools().berserk(getPlayer());
    }

    public boolean canUseIronArm() {
        if (!pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.UNARMED_IRON_ARM_STYLE))
            return false;

        return pluginRef.getPermissionTools().isSubSkillEnabled(getPlayer(), SubSkillType.UNARMED_IRON_ARM_STYLE);
    }

    public boolean canUseBerserk() {
        return mcMMOPlayer.getSuperAbilityMode(SuperAbilityType.BERSERK);
    }

    public boolean canDisarm(LivingEntity target) {
        if (!pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.UNARMED_DISARM))
            return false;

        return target instanceof Player && ((Player) target).getInventory().getItemInMainHand().getType() != Material.AIR && pluginRef.getPermissionTools().isSubSkillEnabled(getPlayer(), SubSkillType.UNARMED_DISARM);
    }

    public boolean canDeflect() {
        if (!pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.UNARMED_ARROW_DEFLECT))
            return false;

        Player player = getPlayer();

        return pluginRef.getItemTools().isUnarmed(player.getInventory().getItemInMainHand()) && pluginRef.getPermissionTools().isSubSkillEnabled(getPlayer(), SubSkillType.UNARMED_ARROW_DEFLECT);
    }

    public boolean canUseBlockCracker() {
        if (!pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.UNARMED_BLOCK_CRACKER))
            return false;

        return pluginRef.getPermissionTools().isSubSkillEnabled(getPlayer(), SubSkillType.UNARMED_BLOCK_CRACKER);
    }

    public boolean blockCrackerCheck(BlockState blockState) {
        if (!pluginRef.getRandomChanceTools().isActivationSuccessful(SkillActivationType.ALWAYS_FIRES, SubSkillType.UNARMED_BLOCK_CRACKER, getPlayer())) {
            return false;
        }

        switch (blockState.getType()) {
            case STONE_BRICKS:
                /*if (!Unarmed.blockCrackerSmoothBrick) {
                    return false;
                }*/

                blockState.setType(Material.CRACKED_STONE_BRICKS);
                return true;

            default:
                return false;
        }
    }

    /**
     * Check for disarm.
     *
     * @param defender The defending player
     */
    public void disarmCheck(Player defender) {
        if (pluginRef.getRandomChanceTools().isActivationSuccessful(SkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkillType.UNARMED_DISARM, getPlayer()) && !hasIronGrip(defender)) {
            if (pluginRef.getEventManager().callDisarmEvent(defender).isCancelled()) {
                return;
            }

            if (pluginRef.getUserManager().getPlayer(defender) == null)
                return;

            Item item = pluginRef.getMiscTools().dropItem(defender.getLocation(), defender.getInventory().getItemInMainHand());

            if (item != null && pluginRef.getConfigManager().getConfigUnarmed().doesDisarmPreventTheft()) {
                item.setMetadata(MetadataConstants.DISARMED_ITEM_METAKEY, pluginRef.getUserManager().getPlayer(defender).getPlayerMetadata());
            }

            defender.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            pluginRef.getNotificationManager().sendPlayerInformation(defender, NotificationType.SUBSKILL_MESSAGE, "Skills.Disarmed");
        }
    }

    /**
     * Check for arrow deflection.
     */
    public boolean deflectCheck() {
        if (pluginRef.getRandomChanceTools().isActivationSuccessful(SkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkillType.UNARMED_ARROW_DEFLECT, getPlayer())) {
            pluginRef.getNotificationManager().sendPlayerInformation(getPlayer(), NotificationType.SUBSKILL_MESSAGE, "Combat.ArrowDeflect");
            return true;
        }

        return false;
    }

    /**
     * Handle the effects of the Berserk ability
     *
     * @param damage The amount of damage initially dealt by the event
     */
    public double berserkDamage(double damage) {
        damage = (damage * berserkDamageModifier) - damage;

        return damage;
    }

    /**
     * Handle the effects of the Iron Arm ability
     */
    public double ironArm() {
        if (!pluginRef.getRandomChanceTools().isActivationSuccessful(SkillActivationType.ALWAYS_FIRES, SubSkillType.UNARMED_IRON_ARM_STYLE, getPlayer())) {
            return 0;
        }

        return getIronArmDamage();
    }

    public boolean isPunchingCooldownOver() {
        return (lastAttacked + attackInterval) <= System.currentTimeMillis();
    }

    public double getIronArmDamage() {
        int rank = pluginRef.getRankTools().getRank(getPlayer(), SubSkillType.UNARMED_IRON_ARM_STYLE);

        if (rank == 1) {
            return 4;
        } else {
            return 3 + (rank * 2);
        }
    }

    /**
     * Check Iron Grip ability success
     *
     * @param defender The defending player
     * @return true if the defender was not disarmed, false otherwise
     */
    private boolean hasIronGrip(Player defender) {
        if (!pluginRef.getMiscTools().isNPCEntityExcludingVillagers(defender)
                && pluginRef.getPermissionTools().isSubSkillEnabled(defender, SubSkillType.UNARMED_IRON_GRIP)
                && pluginRef.getRandomChanceTools().isActivationSuccessful(SkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkillType.UNARMED_IRON_GRIP, defender)) {
            pluginRef.getNotificationManager().sendPlayerInformation(defender, NotificationType.SUBSKILL_MESSAGE, "Unarmed.Ability.IronGrip.Defender");
            pluginRef.getNotificationManager().sendPlayerInformation(getPlayer(), NotificationType.SUBSKILL_MESSAGE, "Unarmed.Ability.IronGrip.Attacker");

            return true;
        }

        return false;
    }

    public long getLastAttacked() {
        return lastAttacked;
    }

    public void setLastAttacked(long lastAttacked) {
        this.lastAttacked = lastAttacked;
    }

    public long getAttackInterval() {
        return attackInterval;
    }

    public void setAttackInterval(long attackInterval) {
        this.attackInterval = attackInterval;
    }
}
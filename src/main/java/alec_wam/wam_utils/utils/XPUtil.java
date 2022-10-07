package alec_wam.wam_utils.utils;

import net.minecraft.world.entity.player.Player;

/**
 * Values taken from OpenMods
 * @see {@link https://github.com/OpenMods/OpenModsLib/blob/master/src/main/java/openmods/utils/EnchantmentUtils.java}
 *
 */
public class XPUtil {

	// Values taken from OpenBlocks to ensure compatibility

	public static final int XP_PER_BOTTLE = 8;
	public static final int RATIO = 20;
	public static final int LIQUID_PER_XP_BOTTLE = XP_PER_BOTTLE * RATIO;


	public static int liquidToExperience(int liquid) {
		return liquid / RATIO;
	}

	public static int experienceToLiquid(int xp) {
		return xp * RATIO;
	}

	public static int getLiquidForLevel(int level) {
		return experienceToLiquid(getExperienceForLevel(level));
	}

	public static int getLevelFromLiquid(int liquid){
		return getLevelForExperience(liquidToExperience(liquid));
	}

	private static final Integer[] xpmap = new Integer[256];

	static {
		for (int i = 0; i < xpmap.length; i++) {
			xpmap[i] = getExperienceForLevelImpl(i);
		}
	}

	public static int getExperienceForLevel(int level) {
		if (level >= 0 && level < xpmap.length) {
			return xpmap[level];
		}
		if (level >= 21863) {
			return Integer.MAX_VALUE;
		}
		return getExperienceForLevelImpl(level);
	}

	private static int getExperienceForLevelImpl(int level) {
		int res = 0;
		for (int i = 0; i < level; i++) {
			res += getXpBarCapacity(i);
			if (res < 0) {
				return Integer.MAX_VALUE;
			}
		}
		return res;
	}

	public static int getXpBarCapacity(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		} else if (level >= 15) {
			return 37 + (level - 15) * 5;
		}
		return 7 + level * 2;
	}

	public static int getLevelForExperience(int experience) {
		for (int i = 0; i < xpmap.length; i++) {
			if (xpmap[i] > experience) {
				return i - 1;
			}
		}
		int i = xpmap.length;
		while (getExperienceForLevel(i) <= experience) {
			i++;
		}
		return i - 1;
	}

	public static int getPlayerXP(Player player) {
		return (int) (getExperienceForLevel(player.experienceLevel) + (player.experienceProgress * player.getXpNeededForNextLevel()));
	}

	public static void addPlayerXP(Player player, int amount) {
		player.giveExperiencePoints(amount);
	}

}

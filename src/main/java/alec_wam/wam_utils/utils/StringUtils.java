package alec_wam.wam_utils.utils;

import net.minecraft.util.RandomSource;

public class StringUtils {

	public static final String[] COLORS = {
		"White",
		"Orange",
		"Magenta",
		"LightBlue",
		"Yellow",
		"Lime",
		"Pink",
		"Gray",
		"LightGray",
		"Cyan",
		"Purple",
		"Blue",
		"Brown",
		"Green",
		"Red",
		"Black",
	};
	
	public static final String[] ANIMALS = {
		"Bee",
		"Cat",
		"Chicken",
		"Cow",
		"Dolphin",
		"Fox",
		"Frog",
		"Ocelot",
		"Panda",
		"Parrot",
		"Pig",
		"Rabbit",
		"Sheep",
		"Squid",
		"Turtle",
		"Wolf"
	};
	
	public static String getRandomName(RandomSource random) {
		String color = COLORS[random.nextInt(0, COLORS.length)];
		String animal = ANIMALS[random.nextInt(0, ANIMALS.length)];
		String number = "" + random.nextInt(1, 17);
		return color+animal+number;
	}
	
}

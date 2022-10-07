package alec_wam.wam_utils.blocks.jar;

import java.util.EnumMap;

import com.google.common.collect.Maps;

import alec_wam.wam_utils.blocks.INBTItemDrop;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.state.BlockState;

public class JarBE extends WAMUtilsBlockEntity implements INBTItemDrop {
	
	public static enum JarContents {
		EMPTY, POTION, HONEY, SHULKER, LIGHTNING;
	}
	
	public static final int BOTTLE_CAPACITY = 4;
	public static final int LIGHTNING_CAPACITY = 8;
	
	private Potion potion = Potions.EMPTY;
	private JarContents jarContents = JarContents.EMPTY;
	private int bottleCount;
	private EnumMap<Direction, Boolean> labelMap = Maps.newEnumMap(Direction.class);
	
	protected int shulkerTick;
	protected float shulkerBobOffset;
	protected final RandomSource random = RandomSource.create();
	
	public JarBE(BlockPos pos, BlockState state) {
		super(BlockInit.JAR_BE.get(), pos, state);
		this.shulkerBobOffset = random.nextFloat() * (float)Math.PI * 2.0F;
	}

	public void tickClient() {
		if(this.jarContents == JarContents.SHULKER) {
			shulkerTick++;
		}
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		potion = Potions.EMPTY;
		if(nbt.contains("Potion")) {
			potion = Potion.byName(nbt.getString("Potion"));
		}
		bottleCount = nbt.getInt("BottleCount");
		jarContents = JarContents.values()[nbt.getInt("JarContents")];
		for(Direction facing : Direction.Plane.HORIZONTAL){
			if(nbt.contains("Label."+facing.getName().toUpperCase())){
				labelMap.put(facing, nbt.getBoolean("Label."+facing.getName().toUpperCase()));
			} else {
				labelMap.put(facing, false);
			}
		}
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		if(potion != Potions.EMPTY){
			@SuppressWarnings("deprecation")
			ResourceLocation resourcelocation = Registry.POTION.getKey(potion);
			nbt.putString("Potion", resourcelocation.toString());
		}
		nbt.putInt("BottleCount", bottleCount);
		nbt.putInt("JarContents", jarContents.ordinal());
		for(Direction facing : Direction.Plane.HORIZONTAL){
			nbt.putBoolean("Label."+facing.getName().toUpperCase(), labelMap.getOrDefault(facing, false));
		}
	}
	
	@Override
	public ItemStack getNBTDrop(Item item) {
		ItemStack stack = new ItemStack(item);
		CompoundTag nbt = new CompoundTag();
		if(potion !=null && potion != Potions.EMPTY){
			@SuppressWarnings("deprecation")
			ResourceLocation resourcelocation = Registry.POTION.getKey(potion);
			nbt.putString("Potion", resourcelocation.toString());
		}
		if(bottleCount > 0)nbt.putInt("BottleCount", bottleCount);
		if(jarContents != JarContents.EMPTY) {
			nbt.putInt("JarContents", jarContents.ordinal());
		}
		for(Direction facing : Direction.Plane.HORIZONTAL){
			if(labelMap.getOrDefault(facing, false))nbt.putBoolean("Label."+facing.getName().toUpperCase(), true);
		}
		stack.setTag(nbt);
		return stack;
	}
	
	@Override
	public void readFromItem(ItemStack stack) {
		if(stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			if(nbt.contains("Potion")) {
				potion = Potion.byName(nbt.getString("Potion"));
			}
			if(nbt.contains("BottleCount")) {
				bottleCount = nbt.getInt("BottleCount");
			}
			if(nbt.contains("JarContents")) {
				jarContents = JarContents.values()[nbt.getInt("JarContents")];
			}
			for(Direction facing : Direction.Plane.HORIZONTAL){
				if(nbt.contains("Label."+facing.getName().toUpperCase())){
					labelMap.put(facing, nbt.getBoolean("Label."+facing.getName().toUpperCase()));
				} else {
					labelMap.put(facing, false);
				}
			}
			setChanged();
			markBlockForUpdate(null);
		}
	}
	
	public Potion getPotion(){
		return potion;
	}
	
	public void setPotionType(Potion type){
		this.potion = type;
	}
	
	public int getBottleCount(){
		return bottleCount;
	}
	
	public void setBottleCount(int count){
		this.bottleCount = count;
	}
	
	public JarContents getContents(){
		return jarContents;
	}
	
	public void setContents(JarContents type){
		this.jarContents = type;
	}
	
	public boolean hasLabel(Direction facing){
		return labelMap.getOrDefault(facing, false);
	}
	
	public void setHasLabel(Direction facing, boolean value){
		labelMap.put(facing, value);
	}

	public EnumMap<Direction, Boolean> getLabelMap() {
		return labelMap;
	}
	
}

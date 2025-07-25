package alec_wam.wam_utils.common.items;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class MagicBonemealItem extends Item {

	public MagicBonemealItem(Properties properties) {
		super(properties);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		boolean isClient = level.isClientSide;
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		
		IntegerProperty[] MAX_PROPERTIES = new IntegerProperty[] {
				BlockStateProperties.LEVEL_HONEY,
				BlockStateProperties.HATCH,
				BlockStateProperties.AGE_1, BlockStateProperties.AGE_2, BlockStateProperties.AGE_3, BlockStateProperties.AGE_4, BlockStateProperties.AGE_5, BlockStateProperties.AGE_7, BlockStateProperties.AGE_15, BlockStateProperties.AGE_25
		};
		
		BlockState newState = state;
		boolean modified = false;
		Collection<Property<?>> stateProps = state.getProperties();
		List<IntegerProperty> maxIntProps = Arrays.stream(MAX_PROPERTIES).filter(stateProps::contains).toList();
		if(!maxIntProps.isEmpty()) {
			for(IntegerProperty prop : maxIntProps) {
				int max = prop.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
				newState = newState.setValue(prop, max);
				modified = true;
			}
		}
		
		if(modified) {
			level.setBlockAndUpdate(pos, newState);
			return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
		}
		
		return super.useOn(context);
	}
	
	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
		boolean isClient = player.level().isClientSide;
		if(interactionTarget instanceof Sheep sheep) {
			if(sheep.isSheared()) {
				if(!isClient) {
					sheep.setSheared(false);
				}
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER; 
			}
		}
		if(interactionTarget instanceof AgeableMob ageable) {
			if(ageable.isBaby()) {
				if(!isClient) {
					ageable.setBaby(false);
				}
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER; 
			}
		}
		if(interactionTarget instanceof Animal animal) {
			if(animal.isInLove()) {
				if(!isClient) {
					animal.resetLove();
				}
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER; 
			}
			if(animal.getAge() != 0) {
				if(!isClient) {
					animal.setAge(0);
				}
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER; 
			}
		}
        return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
    }

}

package alec_wam.wam_utils.common.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class WorkerStaffItem extends Item {
	public static final String NBT_BLOCK_SETTINGS = "BlockSettings";
	public static final String NBT_BLOCK_POS_LIST = "BlockPosList";
	public static final String NBT_BLOCK_AREA = "BlockPosArea";
	public static final String NBT_BLOCK_POS = "BlockPos";
	public static final String NBT_BLOCK_FACE = "BlockSide";
	
	public static final String NBT_SELECTION_TYPE = "SelectionType";	
	public static final String NBT_JOB = "JobType";
	
	public static enum SelectionType implements StringRepresentable {
		SINGLE, AREA, LIST;
		
		public static final StringRepresentable.EnumCodec<SelectionType> CODEC = StringRepresentable.fromEnum(SelectionType::values);
        private static final IntFunction<SelectionType> BY_ID = ByIdMap.continuous(SelectionType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, SelectionType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, SelectionType::ordinal);
		
		public SelectionType getNextType() {
			return SelectionType.values()[(this.ordinal() + 1) % SelectionType.values().length];
		}
		
		@Override
		public String getSerializedName() {
			return name();
		}

        public static SelectionType byId(int id) {
            return BY_ID.apply(id);
        }
	}
	
	public static class WorkerBlockSettings {		
		
		public static final Codec<WorkerBlockSettings> CODEC = RecordCodecBuilder.create(instance ->
		    instance.group(
		        BlockPos.CODEC.optionalFieldOf("singleBlockPos").forGetter((WorkerBlockSettings settings) -> {
		        	return settings.singleBlockPos;
		        }),
		        Direction.CODEC.optionalFieldOf("singleBlockFace").forGetter((WorkerBlockSettings settings) -> {
		        	return settings.singleBlockFace;
		        }),
		        BlockPos.CODEC.optionalFieldOf("areaBlockPos1").forGetter((WorkerBlockSettings settings) -> {
		        	return settings.areaBlockPos1;
		        }),
		        BlockPos.CODEC.optionalFieldOf("areaBlockPos2").forGetter((WorkerBlockSettings settings) -> {
		        	return settings.areaBlockPos2;
		        }),
				BlockPos.CODEC.listOf().optionalFieldOf("blockPosList").forGetter((WorkerBlockSettings settings) -> {
		        	return settings.blockPosList;
		        })
		    ).apply(instance, WorkerBlockSettings::new)
		);
		public static final StreamCodec<ByteBuf, WorkerBlockSettings> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), (WorkerBlockSettings settings) -> {
	        	return settings.singleBlockPos;
	        },
			ByteBufCodecs.optional(Direction.STREAM_CODEC), (WorkerBlockSettings settings) -> {
	        	return settings.singleBlockFace;
	        },
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), (WorkerBlockSettings settings) -> {
	        	return settings.areaBlockPos1;
	        },
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), (WorkerBlockSettings settings) -> {
	        	return settings.areaBlockPos2;
	        },
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())), (WorkerBlockSettings settings) -> {
	        	return settings.blockPosList;
	        },
			WorkerBlockSettings::new
	    );
		
		private Optional<BlockPos> singleBlockPos;
		private Optional<Direction> singleBlockFace;
		
		private Optional<BlockPos> areaBlockPos1;
		private Optional<BlockPos> areaBlockPos2;

		private Optional<List<BlockPos>> blockPosList;
		
		public WorkerBlockSettings(BlockPos blockPos, Direction dir) {
			this(Optional.of(blockPos), Optional.of(dir), Optional.empty(), Optional.empty(), Optional.empty());
		}
		
		public WorkerBlockSettings(BlockPos blockPos1, BlockPos blockPos2) {
			this(Optional.empty(), Optional.empty(), Optional.ofNullable(blockPos1), Optional.ofNullable(blockPos2), Optional.empty());
		}
		
		public WorkerBlockSettings(List<BlockPos> blockPosList) {
			this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(blockPosList));
		}
		
		public WorkerBlockSettings(
			Optional<BlockPos> singleBlockPos, Optional<Direction> singleBlockFace, 
			Optional<BlockPos> areaBlockPos1, Optional<BlockPos> areaBlockPos2,
			Optional<List<BlockPos>> blockPosList
		) {
			this.singleBlockPos = singleBlockPos;
			this.singleBlockFace = singleBlockFace;
			this.areaBlockPos1 = areaBlockPos1;
			this.areaBlockPos2 = areaBlockPos2;
			this.blockPosList = blockPosList;
		}
		
		public Optional<BlockPos> getSingleBlockPos() {
			return singleBlockPos;
		}

		public Optional<Direction> getSingleBlockFace() {
			return singleBlockFace;
		}

		public Optional<BlockPos> getAreaBlockPos1() {
			return areaBlockPos1;
		}

		public Optional<BlockPos> getAreaBlockPos2() {
			return areaBlockPos2;
		}

		public Optional<List<BlockPos>> getBlockPosList() {
			return blockPosList;
		}

		// Add a BlockPos to the list and return a new WorkerBlockSettings
		public WorkerBlockSettings addBlockPosToList(BlockPos pos) {
			List<BlockPos> updatedList = new ArrayList<>(blockPosList.orElse(Collections.emptyList()));
			updatedList.add(pos);
			return new WorkerBlockSettings(updatedList);
		}

		// Remove a BlockPos from the list and return a new WorkerBlockSettings
		public WorkerBlockSettings removeBlockPosFromList(BlockPos pos) {
			List<BlockPos> updatedList = new ArrayList<>(blockPosList.orElse(Collections.emptyList()));
			updatedList.remove(pos);
			return new WorkerBlockSettings(updatedList);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(
				this.singleBlockPos, this.singleBlockFace,
				this.areaBlockPos1, this.areaBlockPos2,
				this.blockPosList
			);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
	            return true;
	        } else if(obj instanceof WorkerBlockSettings settings) {
	        	return Objects.equals(settings.singleBlockPos, this.singleBlockPos) 
	        			&& Objects.equals(settings.singleBlockFace, this.singleBlockFace) 
	        			&& Objects.equals(settings.areaBlockPos1, this.areaBlockPos1) 
	        			&& Objects.equals(settings.areaBlockPos2, this.areaBlockPos2)
	        			&& Objects.equals(settings.blockPosList, this.blockPosList);
    		}
			return false;
		}
	}
	
	public WorkerStaffItem(Properties props) {
		super(props);
	}
	
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);

		if (player.isCrouching()) {
			if (!(level instanceof ServerLevel)) {
				return InteractionResult.SUCCESS;
			}
			if (hand == InteractionHand.MAIN_HAND) {
				JobType type = WorkerStaffItem.getJobType(itemstack);
				final JobType nextType = type.getNextType();
				WorkerStaffItem.setJobType(itemstack, nextType);
				player.displayClientMessage(Component.translatable("wamutils.message.workers.staff.job_type", nextType.name()), true);
				return InteractionResult.SUCCESS_SERVER;
			}		
			if (hand == InteractionHand.OFF_HAND) {
				WorkerStaffItem.clearSelectionSettings(itemstack);
				player.displayClientMessage(Component.translatable("wamutils.message.workers.staff.clear_selection"), true);
				return InteractionResult.SUCCESS_SERVER;
			}
		}
		else {
			if (hand == InteractionHand.OFF_HAND) {
				if (!(level instanceof ServerLevel)) {
					return InteractionResult.SUCCESS;
				}
				itemstack.update(ModInit.WORKER_SELECTION_TYPE_COMPONENT, SelectionType.SINGLE, type -> type.getNextType());
				SelectionType newType = itemstack.get(ModInit.WORKER_SELECTION_TYPE_COMPONENT);
				if(newType !=null) {
					player.displayClientMessage(Component.translatable("wamutils.message.workers.staff.selection_type", newType.name()), true);
				}
				return InteractionResult.SUCCESS_SERVER;
			}
		}
		return super.use(level, player, hand);
	}
	
	public static void clearSelectionSettings(ItemStack stack) {
		if(stack.has(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT)) {
			stack.remove(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT);
		}
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if(context.getPlayer().isCrouching()) {
			if (!(level instanceof ServerLevel)) {
				return InteractionResult.SUCCESS;
			} else {
				BlockPos pos = context.getClickedPos();
				Direction face = context.getClickedFace();
				ItemStack stack = context.getItemInHand();
				Player player = context.getPlayer();
				
				SelectionType selection = stack.getOrDefault(ModInit.WORKER_SELECTION_TYPE_COMPONENT, SelectionType.SINGLE);
				if(selection == SelectionType.SINGLE) {
					stack.set(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT, new WorkerBlockSettings(pos, face));
					player.displayClientMessage(Component.translatable("minions.message.staff.add_selection"), true);
				}				
				else if(selection == SelectionType.AREA) {
					WorkerBlockSettings settings = loadBlockSettings(stack);
					BlockPos currentPos1 = settings == null ? null : settings.areaBlockPos1.orElse(null);
					BlockPos currentPos2 = settings == null ? null : settings.areaBlockPos2.orElse(null);
					
					if(currentPos1 == null || currentPos2 == null) {
						if(currentPos1 == null) {
							currentPos1 = pos;
						}
						else if(currentPos2 == null) {
							currentPos2 = pos;
						}
						
						stack.set(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT, new WorkerBlockSettings(currentPos1, currentPos2));
						player.displayClientMessage(Component.translatable("minions.message.staff.add_selection"), true);
					}
				}
				else if(selection == SelectionType.LIST) {
					WorkerBlockSettings settings = loadBlockSettings(stack);
					if(settings == null) {
						settings = new WorkerBlockSettings(Collections.emptyList());
					}
					List<BlockPos> posList = settings.blockPosList.orElse(Collections.emptyList());				
					
					if(!posList.contains(pos)) {
						stack.set(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT, settings.addBlockPosToList(pos));
						player.displayClientMessage(Component.translatable("minions.message.staff.add_selection"), true);
					}
					else {
						stack.set(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT, settings.removeBlockPosFromList(pos));
						player.displayClientMessage(Component.translatable("minions.message.staff.remove_selection"), true);
					}
				}
				
				return InteractionResult.SUCCESS;
			}
		}
		
		return InteractionResult.PASS;
	}
	
	@Override
	public void appendHoverText(
	        ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag
    ) {	
		JobType job = getJobType(stack);
		if(job !=null) {
			tooltipAdder.accept(Component.literal(job.name()).withStyle(ChatFormatting.DARK_AQUA));
		}
		
		SelectionType selection = stack.get(ModInit.WORKER_SELECTION_TYPE_COMPONENT);
		if(selection !=null) {
			tooltipAdder.accept(Component.literal(selection.name()).withStyle(ChatFormatting.DARK_GREEN));
		}
		
		WorkerBlockSettings settings = loadBlockSettings(stack);		
		if(settings !=null) {			
			if(selection == SelectionType.SINGLE) {
				BlockPos pos = settings.singleBlockPos.orElse(null);
				if(pos !=null) {
					tooltipAdder.accept(Component.literal(pos.toShortString()));
				}
			}
			if(selection == SelectionType.AREA) {
				BlockPos pos1 = settings.areaBlockPos1.orElse(null);
				BlockPos pos2 = settings.areaBlockPos2.orElse(null);
				tooltipAdder.accept(Component.literal(pos1 !=null ? pos1.toShortString() : "Empty"));
				tooltipAdder.accept(Component.literal(pos2 !=null ? pos2.toShortString() : "Empty"));
			}
			if(selection == SelectionType.LIST) {
				List<BlockPos> posList = settings.blockPosList.orElse(null);
				if(posList !=null) {
					// TODO Show this with shift
					posList.forEach((pos) -> {
						tooltipAdder.accept(Component.literal(pos.toShortString()));
					});
				}
			}
		}
	}
	
	
	@Nullable
	public static WorkerBlockSettings loadBlockSettings(ItemStack stack) {
		return stack.get(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT);
	}
	
	public static JobType getJobType(ItemStack stack) {
		return stack.getOrDefault(ModInit.WORKER_JOB_TYPE_COMPONENT, JobType.CROP);
	}
	
	public static void setJobType(ItemStack stack, JobType type) {
		stack.set(ModInit.WORKER_JOB_TYPE_COMPONENT, type);
	}

}


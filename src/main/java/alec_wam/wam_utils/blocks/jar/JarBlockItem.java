package alec_wam.wam_utils.blocks.jar;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.jar.JarBE.JarContents;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.data.loading.DatagenModLoader;

public class JarBlockItem extends BlockItem {

	public JarBlockItem(Block block, Properties props) {
		super(block, props);
	}
	
	@Override
	public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer)
	{
		if (!DatagenModLoader.isRunningDataGen())
			consumer.accept(JarItemRenderer.CLIENT_RENDERER);
	}
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
    	super.appendHoverText(stack, null, tooltip, flags);
    	if(stack.hasTag()) {
    		CompoundTag tag = stack.getTag();
    		boolean showBottleCount = true;
    		if(tag.contains("Potion")) {
				Potion potion = Potion.byName(tag.getString("Potion"));
				for(MobEffectInstance mobeffectinstance : potion.getEffects()) {
					MutableComponent mutablecomponent = Component.translatable(mobeffectinstance.getDescriptionId());
		            MobEffect mobeffect = mobeffectinstance.getEffect();
		            
		            if (mobeffectinstance.getAmplifier() > 0) {
		               mutablecomponent = Component.translatable("potion.withAmplifier", mutablecomponent, Component.translatable("potion.potency." + mobeffectinstance.getAmplifier()));
		            }

		            if (mobeffectinstance.getDuration() > 20) {
		               mutablecomponent = Component.translatable("potion.withDuration", mutablecomponent, MobEffectUtil.formatDuration(mobeffectinstance, 1.0F));
		            }

		            tooltip.add(mutablecomponent.withStyle(mobeffect.getCategory().getTooltipFormatting()));
				}
			}
    		JarContents jarContents = JarContents.values()[tag.getInt("JarContents")];
    		if(jarContents == JarContents.HONEY) {
    			tooltip.add(Component.translatable("gui.wam_utils.jar.honey").withStyle(ChatFormatting.YELLOW));
    		}
    		if(jarContents == JarContents.SHULKER) {
    			tooltip.add(Component.translatable("gui.wam_utils.jar.shulker").withStyle(ChatFormatting.DARK_PURPLE));
    			showBottleCount = false;
    		}
    		if(jarContents == JarContents.LIGHTNING) {
    			tooltip.add(Component.translatable("gui.wam_utils.jar.lightning").withStyle(ChatFormatting.BLUE));
    			showBottleCount = true;
    		}
    		
    		if(showBottleCount) {
    			int bottleCount = tag.getInt("BottleCount");
    			if(bottleCount > 0) {
    				int capacity = jarContents == JarContents.LIGHTNING ? JarBE.LIGHTNING_CAPACITY : JarBE.BOTTLE_CAPACITY;
    				tooltip.add(Component.literal(bottleCount + " / " + capacity));
    			}
    		}
    	}
    }
	
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		InteractionHand hand = context.getHand();
		ItemStack heldStack = context.getItemInHand();

		if(player.isShiftKeyDown()) {
			if(heldStack.hasTag()) {
				CompoundTag tag = heldStack.getTag();
				int jarContents = tag.getInt("JarContents");
				if(jarContents == JarContents.LIGHTNING.ordinal()) {
					if(level.canSeeSky(pos.above())) {
						int bottleCount = tag.getInt("BottleCount");
						if(bottleCount > 0) {
							if(!level.isClientSide) {
								LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
								lightningbolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
								Entity entity = player;
								lightningbolt.setCause(entity instanceof ServerPlayer ? (ServerPlayer)entity : null);
								level.addFreshEntity(lightningbolt);
							}
							player.getCooldowns().addCooldown(this, player.isCreative() ? 20 : 100); //Disable for 5 seconds
							if(!player.getAbilities().instabuild) {
								int newCount = bottleCount - 1;
								tag.putInt("BottleCount", newCount);
								if(newCount <= 0) {
									tag.putInt("JarContents", JarContents.EMPTY.ordinal());									
								}								
								heldStack.setTag(tag);
							}
							return InteractionResult.sidedSuccess(level.isClientSide);
						}
					}
					return InteractionResult.PASS;
				}
			}
		}
		else {
			BlockEntity be = level.getBlockEntity(pos);			
			if(be !=null && be instanceof JarBE jar) {
				if(heldStack.hasTag()) {
					CompoundTag nbt = heldStack.getTag();
					JarContents heldContents = JarContents.values()[nbt.getInt("JarContents")];
					int heldBottleCount = nbt.getInt("BottleCount");
					Potion heldPotion = Potions.EMPTY;
					if(nbt.contains("Potion")) {
						heldPotion = Potion.byName(nbt.getString("Potion"));
					}
					if(heldContents == JarContents.EMPTY) {
						if(jar.getContents() != JarContents.EMPTY) {
							ItemStack returnStack = heldStack.copy();
							returnStack.setCount(1);

							CompoundTag returnNBT = nbt.copy();
							boolean performedAction = false;
							if(jar.getContents() == JarContents.POTION) {
								returnNBT.putInt("JarContents", JarContents.POTION.ordinal());

								@SuppressWarnings("deprecation")
								ResourceLocation resourcelocation = Registry.POTION.getKey(jar.getPotion());
								returnNBT.putString("Potion", resourcelocation.toString());	            						

								int count = jar.getBottleCount();
								returnNBT.putInt("BottleCount", count);

								jar.setPotionType(Potions.EMPTY);
								jar.setContents(JarContents.EMPTY);
								jar.setBottleCount(0);
								jar.setChanged();
								jar.markBlockForUpdate(null);
								level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
								performedAction = true;
							}
							else if(jar.getContents() == JarContents.HONEY || jar.getContents() == JarContents.LIGHTNING) {
								returnNBT.putInt("JarContents", jar.getContents().ordinal());

								int count = jar.getBottleCount();
								returnNBT.putInt("BottleCount", count);

								if(jar.getContents() == JarContents.LIGHTNING) {
									level.getLightEngine().checkBlock(pos);	            							
								}
								jar.setContents(JarContents.EMPTY);
								jar.setBottleCount(0);
								jar.setChanged();
								jar.markBlockForUpdate(null);
								level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
								performedAction = true;
							}
							else if(jar.getContents() == JarContents.SHULKER) {
								returnNBT.putInt("JarContents", JarContents.SHULKER.ordinal());	            						

								jar.setContents(JarContents.EMPTY);
								jar.setChanged();
								jar.markBlockForUpdate(null);
								level.getLightEngine().checkBlock(pos);	  
								level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
								performedAction = true;
							}

							if(performedAction) {
								returnStack.setTag(returnNBT);

								if(!player.getAbilities().instabuild) {
									heldStack.shrink(1);
								}

								if (heldStack.isEmpty()) {
									player.setItemInHand(hand, returnStack);
								} else {
									InventoryUtils.givePlayerItem(player, returnStack);
								}
								return InteractionResult.SUCCESS;
							}
						}
					}
					else if(heldContents == JarContents.POTION && heldBottleCount > 0) {
						if(jar.getContents() == JarContents.EMPTY || jar.getContents() == JarContents.POTION && jar.getPotion() == heldPotion) {
							if(jar.getContents() == JarContents.EMPTY) {
								jar.setContents(JarContents.POTION);
								jar.setPotionType(heldPotion);
							}

							int space = JarBE.BOTTLE_CAPACITY - jar.getBottleCount();
							if(space > 0) {
								int remove = Math.min(space, heldBottleCount);
								if(!player.getAbilities().instabuild) {
									int newCount = Math.max(heldBottleCount - remove, 0);
									nbt.putInt("BottleCount", newCount);
									if(newCount <= 0) {
										nbt.remove("Potion");
										nbt.remove("BottleCount");
										nbt.remove("JarContents");
									}
									heldStack.setTag(nbt);
								}	            						
								jar.setBottleCount(jar.getBottleCount() + remove);
								jar.setChanged();
								jar.markBlockForUpdate(null);
								level.playSound((Player)null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
								return InteractionResult.SUCCESS;
							}
						}
					}
					else if(heldContents == JarContents.HONEY && heldBottleCount > 0) {
						if(jar.getContents() == JarContents.EMPTY || jar.getContents() == JarContents.HONEY) {
							if(jar.getContents() == JarContents.EMPTY) {
								jar.setContents(JarContents.HONEY);
							}

							int space = JarBE.BOTTLE_CAPACITY - jar.getBottleCount();
							if(space > 0) {
								int remove = Math.min(space, heldBottleCount);
								if(!player.getAbilities().instabuild) {
									int newCount = Math.max(heldBottleCount - remove, 0);
									nbt.putInt("BottleCount", newCount);
									if(newCount <= 0) {
										nbt.remove("BottleCount");
										nbt.remove("JarContents");
									}
									heldStack.setTag(nbt);
								}	            						
								jar.setBottleCount(jar.getBottleCount() + remove);
								jar.setChanged();
								jar.markBlockForUpdate(null);
								level.playSound((Player)null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
								return InteractionResult.SUCCESS;
							}
						}
					}
					else if(heldContents == JarContents.SHULKER) {
						if(jar.getContents() == JarContents.EMPTY) {
							if(!player.getAbilities().instabuild) {
								nbt.remove("JarContents");
								heldStack.setTag(nbt);
							}	            						
							jar.setContents(JarContents.SHULKER);	            					
							jar.setChanged();
							jar.markBlockForUpdate(null);
							level.getLightEngine().checkBlock(pos);
							return InteractionResult.SUCCESS;	            					
						}
					}
					else if(heldContents == JarContents.LIGHTNING && heldBottleCount > 0) {
						if(jar.getContents() == JarContents.EMPTY || jar.getContents() == JarContents.LIGHTNING) {
							if(jar.getContents() == JarContents.EMPTY) {
								jar.setContents(JarContents.LIGHTNING);
							}

							int space = JarBE.LIGHTNING_CAPACITY - jar.getBottleCount();
							if(space > 0) {
								int remove = Math.min(space, heldBottleCount);
								if(!player.getAbilities().instabuild) {
									int newCount = Math.max(heldBottleCount - remove, 0);
									nbt.putInt("BottleCount", newCount);
									if(newCount <= 0) {
										nbt.remove("BottleCount");
										nbt.remove("JarContents");
									}
									heldStack.setTag(nbt);
								}	            						
								jar.setBottleCount(jar.getBottleCount() + remove);
								jar.setChanged();
								jar.markBlockForUpdate(null);
								level.getLightEngine().checkBlock(pos);
								level.playSound((Player)null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
								return InteractionResult.SUCCESS;
							}
						}
					}
				}
				else {
					//Empty
					if(jar.getContents() != JarContents.EMPTY) {
						ItemStack returnStack = heldStack.copy();
						returnStack.setCount(1);

						CompoundTag returnNBT = new CompoundTag();
						boolean performedAction = false;
						if(jar.getContents() == JarContents.POTION) {
							returnNBT.putInt("JarContents", JarContents.POTION.ordinal());

							@SuppressWarnings("deprecation")
							ResourceLocation resourcelocation = Registry.POTION.getKey(jar.getPotion());
							returnNBT.putString("Potion", resourcelocation.toString());	            						

							int count = jar.getBottleCount();
							returnNBT.putInt("BottleCount", count);

							jar.setPotionType(Potions.EMPTY);
							jar.setContents(JarContents.EMPTY);
							jar.setBottleCount(0);
							jar.setChanged();
							jar.markBlockForUpdate(null);
							level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
							performedAction = true;
						}
						else if(jar.getContents() == JarContents.HONEY || jar.getContents() == JarContents.LIGHTNING) {
							returnNBT.putInt("JarContents", jar.getContents().ordinal());

							int count = jar.getBottleCount();
							returnNBT.putInt("BottleCount", count);

							if(jar.getContents() == JarContents.LIGHTNING) {
								level.getLightEngine().checkBlock(pos);	            							
							}
							jar.setContents(JarContents.EMPTY);
							jar.setBottleCount(0);
							jar.setChanged();
							jar.markBlockForUpdate(null);
							level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
							performedAction = true;
						}
						else if(jar.getContents() == JarContents.SHULKER) {
							returnNBT.putInt("JarContents", JarContents.SHULKER.ordinal());	            						

							jar.setContents(JarContents.EMPTY);
							jar.setChanged();
							jar.markBlockForUpdate(null);
							level.getLightEngine().checkBlock(pos);	  
							level.playSound((Player)null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
							performedAction = true;
						}

						if(performedAction) {
							returnStack.setTag(returnNBT);

							if(!player.getAbilities().instabuild) {
								heldStack.shrink(1);
							}

							if (heldStack.isEmpty()) {
								player.setItemInHand(hand, returnStack);
							} else {
								InventoryUtils.givePlayerItem(player, returnStack);
							}
							return InteractionResult.SUCCESS;
						}
					}
				}
			}
		}
		
		return super.useOn(context);
	}
	
	
	@Override
	public boolean isBarVisible(ItemStack stack) {
		if(stack.hasTag()) {
			CompoundTag tag = stack.getTag();
			int jarContents = tag.getInt("JarContents");
			if(jarContents == JarContents.LIGHTNING.ordinal()) {
				return true;
			}
		}
		return super.isBarVisible(stack);
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		if(stack.hasTag()) {
			CompoundTag tag = stack.getTag();
			int jarContents = tag.getInt("JarContents");
			if(jarContents == JarContents.LIGHTNING.ordinal()) {
				int bottleCount = JarBE.LIGHTNING_CAPACITY - tag.getInt("BottleCount");
				return Math.round(13.0F - (float)bottleCount * 13.0F / (float)JarBE.LIGHTNING_CAPACITY);
			}
		}
		return super.getBarWidth(stack);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		if(stack.hasTag()) {
			CompoundTag tag = stack.getTag();
			int jarContents = tag.getInt("JarContents");
			if(jarContents == JarContents.LIGHTNING.ordinal()) {
				int bottleCount = JarBE.LIGHTNING_CAPACITY - tag.getInt("BottleCount");
				float stackMaxDamage = JarBE.LIGHTNING_CAPACITY;
				float f = Math.max(0.0F, (stackMaxDamage - (float)bottleCount) / stackMaxDamage);
				return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
			}
		}
		return super.getBarColor(stack);
	}

}

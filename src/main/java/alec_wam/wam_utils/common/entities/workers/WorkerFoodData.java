package alec_wam.wam_utils.common.entities.workers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class WorkerFoodData {
    private int foodLevel = 20;
    private float saturationLevel = 5.0F;
    private float exhaustionLevel;
    private int tickTimer;

    private void add(int foodLevel, float saturationLevel) {
        this.foodLevel = Mth.clamp(foodLevel + this.foodLevel, 0, 20);
        this.saturationLevel = Mth.clamp(saturationLevel + this.saturationLevel, 0.0F, (float)this.foodLevel);
    }

    /**
     * Add food stats.
     */
    public void eat(int foodLevelModifier, float saturationLevelModifier) {
        this.add(foodLevelModifier, FoodConstants.saturationByModifier(foodLevelModifier, saturationLevelModifier));
    }

    public void eat(FoodProperties foodProperties) {
        this.add(foodProperties.nutrition(), foodProperties.saturation());
    }

    public void tick(WorkerEntity worker) {
        if(!(worker.level() instanceof ServerLevel)) return;

        // System.out.println("Worker Food Data: " + this.foodLevel + " " + this.saturationLevel + " " + this.exhaustionLevel);

        ServerLevel serverlevel = (ServerLevel)worker.level();
        Difficulty difficulty = serverlevel.getDifficulty();
        if (this.exhaustionLevel > 4.0F) {
            this.exhaustionLevel -= 4.0F;
            if (this.saturationLevel > 0.0F) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
            } else if (difficulty != Difficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }

        // System.out.println("Food Data: " + worker.isHurt());

        if (this.saturationLevel > 0.0F && worker.isHurt() && this.foodLevel >= 20) {
            this.tickTimer++;
            if (this.tickTimer >= 10) {
                float f = Math.min(this.saturationLevel, 6.0F);
                worker.heal(f / 6.0F);
                this.addExhaustion(f);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel >= 18 && worker.isHurt()) {
            this.tickTimer++;
            if (this.tickTimer >= 80) {
                worker.heal(1.0F);
                this.addExhaustion(6.0F);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            this.tickTimer++;
            if (this.tickTimer >= 80) {
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }
    }

    public void readAdditionalSaveData(ValueInput input) {
        this.foodLevel = input.getIntOr("foodLevel", 20);
        this.tickTimer = input.getIntOr("foodTickTimer", 0);
        this.saturationLevel = input.getFloatOr("foodSaturationLevel", 5.0F);
        this.exhaustionLevel = input.getFloatOr("foodExhaustionLevel", 0.0F);
    }

    public void addAdditionalSaveData(ValueOutput output) {
        output.putInt("foodLevel", this.foodLevel);
        output.putInt("foodTickTimer", this.tickTimer);
        output.putFloat("foodSaturationLevel", this.saturationLevel);
        output.putFloat("foodExhaustionLevel", this.exhaustionLevel);
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public boolean needsFood() {
        return this.foodLevel < 20;
    }

    /**
     * Adds input to {@code foodExhaustionLevel} to a max of 40.
     */
    public void addExhaustion(float exhaustion) {
        this.exhaustionLevel = Math.min(this.exhaustionLevel + exhaustion, 40.0F);
    }

    public float getSaturationLevel() {
        return this.saturationLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public void setSaturation(float saturationLevel) {
        this.saturationLevel = saturationLevel;
    }
}

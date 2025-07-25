package alec_wam.wam_utils.common.entities.workers;

import java.util.Arrays;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

public class DefaultWorkerSkin {
    private static final PlayerSkin[] DEFAULT_SKINS = new PlayerSkin[]{
        create("textures/entity/player/slim/alex.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/ari.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/efe.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/kai.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/makena.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/noor.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/steve.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/sunny.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/slim/zuri.png", PlayerSkin.Model.SLIM),
        create("textures/entity/player/wide/alex.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/ari.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/efe.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/kai.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/makena.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/noor.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/steve.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/sunny.png", PlayerSkin.Model.WIDE),
        create("textures/entity/player/wide/zuri.png", PlayerSkin.Model.WIDE)
    };
    private static final PlayerSkin[] DEFAULT_WIDE_SKINS = Arrays.copyOfRange(DEFAULT_SKINS, 9, 18);

    public static ResourceLocation getDefaultTexture(boolean slim) {
        return getDefaultSkin(slim).texture();
    }

    public static PlayerSkin getDefaultSkin(boolean slim) {
        return DEFAULT_SKINS[slim ? 6 : 15];
    }

    public static PlayerSkin get(UUID uuid) {
        return DEFAULT_SKINS[Math.floorMod(uuid.hashCode(), DEFAULT_SKINS.length)];
    }

    public static PlayerSkin getWide(UUID uuid) {
        return DEFAULT_WIDE_SKINS[Math.floorMod(uuid.hashCode(), DEFAULT_WIDE_SKINS.length)];
    }

    public static PlayerSkin get(GameProfile profile) {
        return get(profile.getId());
    }

    private static PlayerSkin create(String path, PlayerSkin.Model skinModel) {
        return new PlayerSkin(ResourceLocation.withDefaultNamespace(path), null, null, null, skinModel, true);
    }
}

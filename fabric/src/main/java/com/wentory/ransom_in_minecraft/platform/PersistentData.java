package com.wentory.ransom_in_minecraft.platform;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public final class PersistentData {
    private static final AttachmentType<com.wentory.ransom_in_minecraft.network.RansomPlayerData> LEGACY = AttachmentRegistry
            .<com.wentory.ransom_in_minecraft.network.RansomPlayerData>builder()
            .persistent(com.wentory.ransom_in_minecraft.network.RansomPlayerData.CODEC).copyOnDeath()
            .buildAndRegister(Identifier.fromNamespaceAndPath("ransom_in_minecraft", "player_state"));
    private static final AttachmentType<CompoundTag> DATA = AttachmentRegistry.<CompoundTag>builder()
            .initializer(CompoundTag::new).persistent(CompoundTag.CODEC)
            .buildAndRegister(Identifier.fromNamespaceAndPath("ransom_in_minecraft", "persistent_data"));
    public static CompoundTag of(Object owner) {
        AttachmentTarget target=(AttachmentTarget)owner;
        CompoundTag data=target.getAttachedOrCreate(DATA);
        var legacy=target.removeAttached(LEGACY);
        if(legacy!=null && !data.contains("ransom_active")) {
            data.putBoolean("ransom_active",legacy.active());
            data.putInt("ransom_coins",legacy.coins());
            data.putInt("ransom_target",legacy.target());
            data.putInt("ransom_ticks",legacy.ticks());
            data.putBoolean("ransom_failure_armed",legacy.failureArmed());
            data.putBoolean("ransom_failure_delete_hotbar",legacy.deleteHotbar());
        }
        return data;
    }
    public static void initialize() {}
    public static void copy(Object original,Object replacement) {
        ((AttachmentTarget)replacement).setAttached(DATA,of(original).copy());
    }
    private PersistentData() {}
}

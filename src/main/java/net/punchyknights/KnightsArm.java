package net.punchyknights;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import banduty.stoneycore.client.model.UnderArmourArmModel;
import banduty.stoneycore.client.render.armor.ArmorAttachmentRenderManager;
import banduty.stoneycore.client.render.armor.ArmorAttachmentRenderer;
import banduty.stoneycore.client.render.armor.ArmorTextureCache;
import banduty.stoneycore.items.custom.armor.underarmor.SCDyeableUnderArmor;
import banduty.stoneycore.items.custom.armor.underarmor.SCUnderArmor;
import punchy.api.PunchyApi;

/**
 * Draws Knights &amp; Heraldry's armour on Punchy's first-person arm, as StoneyCore draws it on Minecraft's: the chest
 * under-armour's sleeve (its own model and texture, dyed when it is dyeable, with its overlay), then every attachment's own
 * first-person drawing (plate arms, gauntlets...). Punchy has posed the arm it hands over; the sleeve is copied onto it.
 */
final class KnightsArm {
    /** StoneyCore moves the sleeve a pixel outwards from the arm (it is a little wider). */
    private static final float SLEEVE_SHIFT = 1.0F / 16.0F;
    private static UnderArmourArmModel sleeves;

    private KnightsArm() {
    }

    /** Whether the player wears StoneyCore under-armour on the chest: then Punchy's own armour layer has nothing to draw. */
    static boolean wears(Object player) {
        return player instanceof AbstractClientPlayer client && client.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof SCUnderArmor;
    }

    static void draw(PunchyApi.ArmDraw draw) {
        if (draw.throughMinecraft() || !(draw.player() instanceof AbstractClientPlayer player)
                || !(draw.poseStack() instanceof PoseStack pose) || !(draw.sink() instanceof MultiBufferSource buffers)
                || !(draw.armPart() instanceof ModelPart arm)) {
            // Through Minecraft's own arm, StoneyCore draws it itself.
            return;
        }
        boolean left = draw.leftArm();
        int light = draw.light();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof SCUnderArmor armor) {
            sleeve(armor, chest, arm, left, pose, buffers, light);
        }
        if (player instanceof LocalPlayer local) {
            HumanoidArm side = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
            for (ItemStack worn : player.getArmorSlots()) {
                if (worn.isEmpty() || !(worn.getItem() instanceof SCUnderArmor)) {
                    continue;
                }
                for (ItemStack attachment : SCUnderArmor.getArmorAttachments(worn)) {
                    ArmorAttachmentRenderer renderer = ArmorAttachmentRenderManager.getRenderer(attachment.getItem());
                    if (renderer != null) {
                        renderer.onRenderInFirstPerson(local, attachment, pose, buffers, light, side);
                    }
                }
            }
        }
    }

    private static void sleeve(ArmorItem armor, ItemStack chest, ModelPart arm, boolean left, PoseStack pose, MultiBufferSource buffers,
            int light) {
        ResourceKey<ArmorMaterial> material = armor.getMaterial().unwrapKey().orElse(null);
        if (material == null) {
            return;
        }
        ResourceLocation id = material.location();
        if (sleeves == null) {
            sleeves = new UnderArmourArmModel(UnderArmourArmModel.getTexturedModelData().bakeRoot());
        }
        ModelPart sleeve = left ? sleeves.armorLeftArm : sleeves.armorRightArm;
        boolean dyeable = chest.getItem() instanceof SCDyeableUnderArmor;
        int colour = dyeable ? DyedItemColor.getOrDefault(chest, ((SCDyeableUnderArmor) chest.getItem()).getDefaultColor()) : -1;
        render(sleeve, arm, left, pose, buffers.getBuffer(RenderType.armorCutoutNoCull(ArmorTextureCache.getBaseTexture(id.getNamespace(),
                id.getPath()))), light, colour);
        if (dyeable) {
            render(sleeve, arm, left, pose, buffers.getBuffer(RenderType.armorCutoutNoCull(ArmorTextureCache.getOverlayTexture(
                    id.getNamespace(), id.getPath()))), light, -1);
        }
    }

    private static void render(ModelPart sleeve, ModelPart arm, boolean left, PoseStack pose, VertexConsumer out, int light, int colour) {
        pose.pushPose();
        pose.translate(left ? SLEEVE_SHIFT : -SLEEVE_SHIFT, 0.0F, 0.0F);
        sleeve.copyFrom(arm);
        sleeve.render(pose, out, light, OverlayTexture.NO_OVERLAY, colour);
        pose.popPose();
    }
}

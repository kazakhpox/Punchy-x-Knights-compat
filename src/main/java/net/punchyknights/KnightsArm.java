package net.punchyknights;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import banduty.stoneycore.client.render.armor.ArmorAttachmentRenderManager;
import banduty.stoneycore.client.render.armor.ArmorAttachmentRenderer;
import banduty.stoneycore.client.render.armor.ArmorTextureCache;
import banduty.stoneycore.client.render.armor.UnderArmourRenderer;
import banduty.stoneycore.items.custom.armor.underarmor.SCDyeableUnderArmor;
import banduty.stoneycore.items.custom.armor.underarmor.SCUnderArmor;

/** Knights &amp; Heraldry's armour on Punchy's first-person arm, the way StoneyCore dresses the body in it. */
public final class KnightsArm {
    private KnightsArm() {
    }

    /** The chest under-armour the player wears, or null. */
    public static SCUnderArmor worn(AbstractClientPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof SCUnderArmor armor ? armor : null;
    }

    /** The model StoneyCore dresses the body in for this under-armour: its arms are the sleeves. */
    public static HumanoidModel<?> model(SCUnderArmor armor) {
        return UnderArmourRenderer.INSTANCE.getModel(armor);
    }

    private static ResourceLocation material(ItemStack stack) {
        if (!(stack.getItem() instanceof SCUnderArmor armor)) {
            return null;
        }
        ResourceKey<ArmorMaterial> key = armor.getMaterial().unwrapKey().orElse(null);
        return key == null ? null : key.location();
    }

    /**
     * The sleeve of StoneyCore's chest model: not its {@code rightArm} (drawn by Punchy, it has no texture of its own) but the
     * {@code armorRightArm} / {@code armorLeftArm} beside it, mapped on StoneyCore's own 128-pixel layout.
     */
    private static ModelPart sleeve(ItemStack stack, boolean left) {
        if (!(stack.getItem() instanceof SCUnderArmor armor) || !(model(armor) instanceof banduty.stoneycore.client.model.UnderArmourChestplateModel chest)) {
            return null;
        }
        net.punchyknights.mixin.ChestplateModelAccess sleeves = (net.punchyknights.mixin.ChestplateModelAccess) chest;
        return left ? sleeves.punchyknights$leftSleeve() : sleeves.punchyknights$rightSleeve();
    }

    /** The texture StoneyCore paints the under-armour with (its base layer). */
    public static ResourceLocation texture(ItemStack stack) {
        ResourceLocation id = material(stack);
        return id == null ? null : ArmorTextureCache.getBaseTexture(id.getNamespace(), id.getPath());
    }

    /**
     * Where Punchy draws the armour's sleeve ({@code part}, posed as the arm): the sleeve itself in StoneyCore's texture (dyed, with
     * its undyed overlay, when it is dyeable), then every
     * attachment's own first-person drawing. Those pose themselves after the player renderer's own arm: it is posed as the sleeve
     * while they draw, and put back after.
     */
    public static void afterSleeve(ModelPart part, boolean left, ItemStack stack, AbstractClientPlayer player, PoseStack pose,
            MultiBufferSource buffers, int light) {
        ResourceLocation id = material(stack);
        if (id == null) {
            return;
        }
        // StoneyCore's under-armour materials have no vanilla armour layers: Punchy, which draws one pass a layer, drew no sleeve.
        ModelPart sleeve = sleeve(stack, left);
        boolean dyeable = stack.getItem() instanceof SCDyeableUnderArmor;
        int colour = dyeable ? net.minecraft.world.item.component.DyedItemColor.getOrDefault(stack,
                ((SCDyeableUnderArmor) stack.getItem()).getDefaultColor()) : -1;
        if (sleeve != null) {
            PartPose before = sleeve.storePose();
            sleeve.copyFrom(part);
            sleeve.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(ArmorTextureCache.getBaseTexture(id.getNamespace(),
                    id.getPath()))), light, OverlayTexture.NO_OVERLAY, colour);
            if (dyeable) {
                sleeve.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(ArmorTextureCache.getOverlayTexture(
                        id.getNamespace(), id.getPath()))), light, OverlayTexture.NO_OVERLAY, -1);
            }
            sleeve.loadPose(before);
        }
        if (!(player instanceof LocalPlayer local)
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof LivingEntityRenderer<?, ?> renderer)
                || !(renderer.getModel() instanceof HumanoidModel<?> body)) {
            return;
        }
        HumanoidArm side = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        ModelPart own = left ? body.leftArm : body.rightArm;
        PartPose saved = own.storePose();
        own.copyFrom(part);
        try {
            for (ItemStack worn : player.getArmorSlots()) {
                if (worn.isEmpty() || !(worn.getItem() instanceof SCUnderArmor)) {
                    continue;
                }
                for (ItemStack attachment : SCUnderArmor.getArmorAttachments(worn)) {
                    ArmorAttachmentRenderer drawer = ArmorAttachmentRenderManager.getRenderer(attachment.getItem());
                    if (drawer != null) {
                        drawer.onRenderInFirstPerson(local, attachment, pose, buffers, light, side);
                    }
                }
            }
        } finally {
            own.loadPose(saved);
        }
    }
}

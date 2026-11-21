package net.punchyknights.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import banduty.stoneycore.items.custom.armor.underarmor.SCUnderArmor;
import net.punchyknights.KnightsArm;

/**
 * Punchy! draws the chest armour on its first-person arm from the armour's model and its vanilla armour-layer texture. For
 * StoneyCore's under-armour it gets the model and the texture StoneyCore dresses the body in, and after the sleeve the
 * attachments are drawn on it.
 */
@Pseudo
@Mixin(targets = "punchy.client.render.ArmorModels", remap = false)
abstract class ArmorModelsMixin {
    @Inject(method = "modelOf", at = @At("HEAD"), cancellable = true, require = 0)
    private static void punchyknights$model(AbstractClientPlayer player, boolean left, CallbackInfoReturnable<Model> cir) {
        SCUnderArmor armor = KnightsArm.worn(player);
        if (armor != null) {
            cir.setReturnValue(KnightsArm.model(armor));
        }
    }

    @Inject(method = "layerTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private static void punchyknights$texture(Entity entity, ItemStack stack, ArmorMaterial.Layer layer,
            CallbackInfoReturnable<ResourceLocation> cir) {
        if (stack.getItem() instanceof SCUnderArmor) {
            ResourceLocation texture = KnightsArm.texture(stack);
            if (texture != null) {
                cir.setReturnValue(texture);
            }
        }
    }

    @Inject(method = "drawArmPart", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/model/geom/ModelPart;loadPose(Lnet/minecraft/client/model/geom/PartPose;)V"))
    private static void punchyknights$attachments(ModelPart part, boolean left, ArmorItem armor, ItemStack stack,
            AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (armor instanceof SCUnderArmor) {
            KnightsArm.afterSleeve(part, left, stack, player, pose, buffers, light);
        }
    }
}

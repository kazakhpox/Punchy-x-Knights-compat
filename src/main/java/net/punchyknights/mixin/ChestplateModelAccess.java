package net.punchyknights.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import banduty.stoneycore.client.model.UnderArmourChestplateModel;

/** The sleeves of StoneyCore's chest under-armour model, which it keeps to itself. */
@Mixin(value = UnderArmourChestplateModel.class, remap = false)
public interface ChestplateModelAccess {
    @Accessor("armorRightArm")
    ModelPart punchyknights$rightSleeve();

    @Accessor("armorLeftArm")
    ModelPart punchyknights$leftSleeve();
}

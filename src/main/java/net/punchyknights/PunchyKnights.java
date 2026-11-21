package net.punchyknights;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * Punchy x Knights &amp; Heraldry: Punchy! draws the first-person arms itself, and the armour on them the vanilla way, from the
 * chest armour's model and vanilla armour-layer texture. StoneyCore's under-armour (all of Knights &amp; Heraldry's body armour)
 * has neither where Punchy looks, so the arm came out black and purple. {@link net.punchyknights.mixin.ArmorModelsMixin} hands
 * Punchy the model and texture StoneyCore dresses the body in, and draws the attachments (plates, gauntlets) after the sleeve.
 */
@Mod(value = PunchyKnights.MOD_ID, dist = Dist.CLIENT)
public final class PunchyKnights {
    public static final String MOD_ID = "punchyknights";
}

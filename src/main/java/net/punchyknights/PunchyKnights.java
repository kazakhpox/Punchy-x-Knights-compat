package net.punchyknights;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import punchy.api.PunchyApi;

/**
 * Punchy x Knights &amp; Heraldry: Punchy! draws the first-person arms itself, and for armour it looks for a vanilla armour
 * texture that StoneyCore's under-armour (all of Knights &amp; Heraldry's body armour) does not have, so the sleeve came out
 * black and purple. Through Punchy's own API this hides Punchy's armour layer while such armour is worn, and draws on the arm
 * what StoneyCore draws on Minecraft's first-person arm: the under-armour sleeve and the plates and gauntlets put on it.
 */
@Mod(value = PunchyKnights.MOD_ID, dist = Dist.CLIENT)
public final class PunchyKnights {
    public static final String MOD_ID = "punchyknights";

    public PunchyKnights(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(PunchyKnights::setup));
    }

    private static void setup() {
        PunchyApi.hideLayers(MOD_ID, (player, left) -> KnightsArm.wears(player) ? PunchyApi.LAYER_ARMOR : 0);
        PunchyApi.drawOnArm(MOD_ID, KnightsArm::draw);
    }
}

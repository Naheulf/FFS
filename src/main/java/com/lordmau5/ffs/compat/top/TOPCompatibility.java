package com.lordmau5.ffs.compat.top;

import com.lordmau5.ffs.FancyFluidStorage;
import net.minecraftforge.fml.common.event.FMLInterModComms;

/**
 * Created by Lordmau5 on 07.10.2016.
 */
public class TOPCompatibility {
    public static final String modid = "theoneprobe";

    public static void register() {
        FMLInterModComms.sendFunctionMessage(FancyFluidStorage.MODID, "getTheOneProbe", "com.lordmau5.ffs.compat.top.GetTheOneProbe");
    }
}

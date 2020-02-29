package cn.xiaopangxie732.f2c;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("f2c")
public class F2C {
	public F2C() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.HIGHEST, this::a);
	}
	public void a(FMLCommonSetupEvent event) {

	}
}
/*
 *  Copyright (C) 2020  FCWorkgroupMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.fcworkgroupmc.f2c.f2c;

import io.github.fcworkgroupmc.f2c.f2c.fabric.FabricLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("f2c")
public class F2C {
	public F2C() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::setupClient);
		MinecraftForge.EVENT_BUS.addListener(this::setupServer);
	}
	private void setupClient(FMLClientSetupEvent event) {
		if(Metadata.disableFabricLoader) return;
		Minecraft mc = event.getMinecraftSupplier().get();

		FabricLoader.INSTANCE.prepareModInit(mc.gameDir.toPath(), mc);
		EntrypointUtils.invoke("main", ModInitializer.class, ModInitializer::onInitialize);
		EntrypointUtils.invoke("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
	}
	private void setupServer(FMLServerAboutToStartEvent event) {
		if(Metadata.disableFabricLoader) return;
		MinecraftServer s = event.getServer();
		if(!(s instanceof DedicatedServer)) return;
		DedicatedServer server = (DedicatedServer) s;

		FabricLoader.INSTANCE.prepareModInit(FMLPaths.GAMEDIR.get(), server);
		EntrypointUtils.invoke("main", ModInitializer.class, ModInitializer::onInitialize);
		EntrypointUtils.invoke("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
	}
}
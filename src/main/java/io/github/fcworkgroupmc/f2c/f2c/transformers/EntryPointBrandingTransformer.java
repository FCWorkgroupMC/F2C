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

package io.github.fcworkgroupmc.f2c.f2c.transformers;

import com.google.common.collect.Sets;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import java.util.ListIterator;
import java.util.Set;

public class EntryPointBrandingTransformer implements ITransformer<MethodNode> {
	@Nonnull
	@Override
	public MethodNode transform(MethodNode input, ITransformerVotingContext context) {
		ListIterator<AbstractInsnNode> it = input.instructions.iterator();
		while (it.hasNext()) {
			if (it.next().getOpcode() == Opcodes.ARETURN) {
				it.previous();
				it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fabricmc/loader/entrypoint/minecraft/hooks/EntrypointBranding", "brand", "(Ljava/lang/String;)Ljava/lang/String;", false));
				it.next();
			}
		}
		return input;
	}

	@Nonnull
	@Override
	public TransformerVoteResult castVote(ITransformerVotingContext context) {
		return TransformerVoteResult.YES;
	}

	@Nonnull
	@Override
	public Set<Target> targets() {
		return Sets.newHashSet(Target.targetMethod("net.minecraft.client.ClientBrandRetriever", "getClientModName", "()Ljava/lang/String;"),
				Target.targetMethod("net.minecraft.server.MinecraftServer", "getServerModName", "()Ljava/lang/String;"));
	}

	@Override
	public String[] labels() {
		return new String[] {"EntryPointBranding"};
	}
}
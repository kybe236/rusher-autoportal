package org.kybe;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.utils.ChatUtils;

public class KBUtils {
	int packets = 0;
	static Minecraft mc = Minecraft.getInstance();

	public static BlockPos forward(BlockPos pos, int distance) {
		return switch (mc.player.getDirection()) {
			case SOUTH -> pos.south(distance);
			case NORTH -> pos.north(distance);
			case WEST -> pos.west(distance);
			default -> pos.east(distance);
		};
	}

	public static BlockPos backward(BlockPos pos, int distance) {
		return switch (mc.player.getDirection()) {
			case SOUTH -> pos.north(distance);
			case NORTH -> pos.south(distance);
			case WEST -> pos.east(distance);
			default -> pos.west(distance);
		};
	}

	public static BlockPos left(BlockPos pos, int distance) {
		return switch (mc.player.getDirection()) {
			case SOUTH -> pos.east(distance);
			case NORTH -> pos.west(distance);
			case WEST -> pos.south(distance);
			default -> pos.north(distance);
		};
	}

	public static BlockPos right(BlockPos pos, int distance) {
		return switch (mc.player.getDirection()) {
			case SOUTH -> pos.west(distance);
			case NORTH -> pos.east(distance);
			case WEST -> pos.north(distance);
			default -> pos.south(distance);
		};
	}

	public void breakBlock(BlockPos pos) {
		if (mc.player == null || mc.level == null || mc.level.getBlockState(pos).canBeReplaced() || packets >= 130 ) return;

		mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
		mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
		packets += 2;

		mc.player.getInventory().tick();
	}

	public void reset() {
		packets = 0;
	}

	public static void placeBlock(BlockPos pos, boolean grim) {
		if (mc.player == null || mc.gameMode == null) return;

		RusherHackAPI.getRotationManager().updateRotation(pos);

		if (grim) {
			mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
		}

		RusherHackAPI.interactions().useBlock(
				pos,
				grim ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
				false,
				false
		);

		mc.player.swing(grim ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

		if (grim) {
			mc.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
		}
	}
}


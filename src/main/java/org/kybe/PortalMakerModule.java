package org.kybe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;

import java.util.ArrayList;

import static org.kybe.KBUtils.*;

/**
 * Portal Maker
 *
 * @author kybe236
 */
public class PortalMakerModule extends ToggleableModule {
	public static MutableComponent prefix;
	int blocksBroken, blocksPlaced;
	KBUtils kb = new KBUtils();
	int finished = 3;
	int initialSlot = 0;
	int initialItemNewSlot = -1;
	int initalFlintAndSteelSlot = 0;
	int initalFlintAndSteelNewSlot = 0;

	/*
	 * Settings
	 */
	private final NumberSetting<Integer> blockBreaksPerTick = new NumberSetting<>("BreakBlocksPerTick", "How many blocks to break in 1 tick", 5, 1, 10)
			.incremental(1)
			.onChange(newValue -> {
				if (newValue <= 0) {
					ChatUtils.print(Component.empty().append(prefix).append("BlocksPerTick must be greater than 0"));
					this.setToggled(false);
				}
			});
	private final NumberSetting<Integer> blockPlacesPerTick  = new NumberSetting<>("BlocksPerSecond", "How many blocks to place in 1 tick", 5, 1, 10)
			.incremental(1)
			.onChange(newValue -> {
				if (newValue <= 0) {
					ChatUtils.print(Component.empty().append(prefix).append("BlocksPerTick must be greater than 0"));
					this.setToggled(false);
				}
			});
	private final BooleanSetting grim = new BooleanSetting("Grim", "Grim", false);
	private final BooleanSetting light = new BooleanSetting("Auto Light", "Auto Light the portel", false);
	ArrayList<BlockPos> lastPlacedPos = new ArrayList<>();

	public PortalMakerModule() {
		super("PortalMaker", "Makes Portals wow", ModuleCategory.CLIENT);

		this.registerSettings(
				blockBreaksPerTick,
				blockPlacesPerTick,
				grim,
				light
		);

		prefix = Component.literal("[PORTALMAKER] ").withStyle(ChatFormatting.BLACK);
		blocksPlaced = 0;
	}

	/*
	 * Main loop
	 */
	@Subscribe(stage = Stage.PRE)
	public void onTick(EventUpdate e) {
		// Null checks
		if (mc.level == null || mc.player == null) {
			this.getLogger().error("Level or player is null");
			this.setToggled(false);
			return;
		}

		// List of Blocks to place
		ArrayList<BlockPos> breakBlocks = new ArrayList<>();
		addBreakBlocks(breakBlocks);

		// If all blocks are broken, start placing
		if (isBreakingFinished(breakBlocks) || finished == 0) {
			finished = 0;

			ArrayList<BlockPos> placeBlocks = new ArrayList<>();
			addObsBlocks(placeBlocks);

			if (isPlacingFinished(placeBlocks)) {
				if (light.getValue()) {
					if (!switchToFlintAndSteal()) {
						this.setToggled(false);
						return;
					}
					ChatUtils.print(Component.empty().append(prefix).append("Lighting portal"));
					placeBlock(forward(mc.player.blockPosition(), 2), grim.getValue());
				}
				this.setToggled(false);
			} else {
				for (BlockPos pos : placeBlocks) {
					for (BlockPos lastPos : lastPlacedPos) {
						if (pos.equals(lastPos)) {
							return;
						}
					}
				}
				lastPlacedPos.clear();
				for (BlockPos pos : placeBlocks) {
					if (blocksPlaced >= blockPlacesPerTick.getValue()) {
						break;
					}
					if (mc.level.getBlockState(pos).getBlock() == Blocks.OBSIDIAN || !mc.level.getBlockState(pos).canBeReplaced()) {
						continue;
					}
					if (!switchToObsidian()) {
						this.setToggled(false);
						return;
					}
					placeBlock(pos, grim.getValue());
					lastPlacedPos.add(pos);

					blocksPlaced++;
				}
				blocksPlaced = 0;
			}
		} else { // Else, keep breaking blocks
			for (BlockPos pos : breakBlocks) {
				if (!mc.level.getBlockState(pos).canBeReplaced() && mc.level.getBlockState(pos).getBlock() != Blocks.OBSIDIAN) {
					if (blocksBroken >= blockBreaksPerTick.getValue()) {
						break;
					}
					kb.breakBlock(pos);
					blocksBroken++;
				}
			}
			blocksBroken = 0;
		}

		// Reset the tick counters
		kb.reset();
	}


	/*
	 * Nether portal
	 *   ||||
	 * ||    ||
	 * ||    ||
	 * ||    ||
	 *   ||||
	 */
	public void addBreakBlocks (ArrayList<BlockPos> blocks) {
		BlockPos pos = mc.player.blockPosition();

		// below feet 2 blocks
		blocks.add(forward(pos, 1).below());
		blocks.add(forward(left(pos, 1), 1).below());

		// feet level 4 blocks
		blocks.add(forward(pos, 1));
		blocks.add(forward(right(pos, 1), 1));
		blocks.add(forward(left(pos, 1), 1));
		blocks.add(forward(left(pos, 2), 1));

		// feet+1 level 4 blocks
		blocks.add(forward(pos, 1).above());
		blocks.add(forward(right(pos, 1), 1).above());
		blocks.add(forward(left(pos, 1), 1).above());
		blocks.add(forward(left(pos, 2), 1).above());

		// feet+2 level 4 blocks
		blocks.add(forward(pos, 1).above(2));
		blocks.add(forward(right(pos, 1), 1).above(2));
		blocks.add(forward(left(pos, 1), 1).above(2));
		blocks.add(forward(left(pos, 2), 1).above(2));

		// feet+3 level 2 blocks
		blocks.add(forward(pos, 1).above(3));
		blocks.add(forward(left(pos, 1), 1).above(3));
	}

	public boolean switchToObsidian() {
		if (mc.player == null || mc.player.getInventory() == null) return false;
		int slot = InventoryUtils.findItem(Items.OBSIDIAN, true, false);
		int currentSlot = mc.player.getInventory().selected;

		if (slot == -1) {
			ChatUtils.print(Component.empty().append(prefix).append("No obsidian found in inventory"));
			return false;
		}
		if(this.initialItemNewSlot == -1) this.initialItemNewSlot = slot;

		if (slot == currentSlot) {
			return true;
		}

		if (slot < 9) {
			mc.player.getInventory().selected = slot;
		} else {
			// idk if needed
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
			mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, currentSlot, ClickType.SWAP, mc.player);
			mc.setScreen(null);
		}
		return true;
	}

	public boolean switchToFlintAndSteal() {
		if (mc.player == null || mc.player.getInventory() == null) return false;
		int slot = InventoryUtils.findItem(Items.FLINT_AND_STEEL, true, false);
		int currentSlot = mc.player.getInventory().selected;

		if (slot == -1) {
			ChatUtils.print(Component.empty().append(prefix).append("No flintandsteal found in inventory"));
			return false;
		}

		if (slot == currentSlot) {
			return true;
		}

		if (slot < 9) {
			mc.player.getInventory().selected = slot;
		} else {
			// idk if needed
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
			mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, currentSlot, ClickType.SWAP, mc.player);
			mc.setScreen(null);
		}

		this.initalFlintAndSteelNewSlot = currentSlot;
		this.initalFlintAndSteelSlot = slot;

		return true;
	}

	public boolean isBreakingFinished(ArrayList<BlockPos> blocks) {
		for (BlockPos pos : blocks) {
			if (!mc.level.getBlockState(pos).canBeReplaced()) {
				return false;
			}
		}
		return true;
	}


	/*
	 * Nether portal
	 *   ||||
	 * ||    ||
	 * ||    ||
	 * ||    ||
	 *   ||||
	 */
	public void addObsBlocks(ArrayList<BlockPos> placeBlocks) {
		BlockPos pos = mc.player.blockPosition();
		ArrayList<BlockPos> blocks = new ArrayList<BlockPos>();

		// feet level 2 blocks
		blocks.add(forward(pos, 1).below());
		blocks.add(forward(left(pos, 1), 1).below());

		// feet+1 out level 2 blocks
		blocks.add(forward(right(pos, 1), 1));
		blocks.add(forward(left(pos, 2), 1));

		// feet+2 in level 2 blocks
		blocks.add(forward(right(pos, 1), 1).above());
		blocks.add(forward(left(pos, 2), 1).above());

		// feet+3 in level 2 blocks
		blocks.add(forward(right(pos, 1), 1).above(2));
		blocks.add(forward(left(pos, 2), 1).above(2));

		// feet+4 in level 2 blocks
		blocks.add(forward(pos, 1).above(3));
		blocks.add(forward(left(pos, 1), 1).above(3));

		for (BlockPos block : blocks) {
			if (mc.level.getBlockState(block).canBeReplaced()) {
				placeBlocks.add(block);
			}
		}

	}

	public boolean isPlacingFinished(ArrayList<BlockPos> blocks) {
		for (BlockPos pos : blocks) {
			if (mc.level.getBlockState(pos).getBlock() != Blocks.OBSIDIAN) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void onEnable() {
		if (mc.level == null || mc.player == null) {
			this.getLogger().error("Level or player is null");
			this.setToggled(false);
			return;
		}
		ChatUtils.print(Component.empty().append(prefix).append("Started making Portal"));
		initialSlot = mc.player.getInventory().selected;
		finished = 3;
	}

	@Override
	public void onDisable() {
		if(mc.level != null && mc.player != null) {
			ChatUtils.print(Component.empty().append(prefix).append("Finished making Portal"));
			finished = 3;

			ChatUtils.print(Component.empty().append(prefix).append("Switching from " + initialItemNewSlot + " to " + initialSlot));
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
			mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, initialItemNewSlot, initialSlot, ClickType.SWAP, mc.player);
			mc.setScreen(null);
			mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
			ChatUtils.print(Component.empty().append(prefix).append("Switching from " + initalFlintAndSteelNewSlot + " to " + initalFlintAndSteelSlot));
			mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, initalFlintAndSteelNewSlot, initalFlintAndSteelSlot, ClickType.SWAP, mc.player);
			mc.setScreen(null);
		}
	}
}

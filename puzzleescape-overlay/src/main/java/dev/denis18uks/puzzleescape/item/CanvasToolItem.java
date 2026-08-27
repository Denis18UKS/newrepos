package dev.denis18uks.puzzleescape.item;

import dev.denis18uks.puzzleescape.server.DraftSettings;
import dev.denis18uks.puzzleescape.server.PuzzleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;

public final class CanvasToolItem extends Item {
    public CanvasToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) return ActionResult.SUCCESS;
        PlayerEntity playerEntity = context.getPlayer();
        if (!(playerEntity instanceof ServerPlayerEntity player)) return ActionResult.PASS;

        ItemStack stack = context.getStack();
        String puzzleId = "";
        if (stack.hasNbt() && stack.getNbt().contains("PuzzleId")) {
            puzzleId = stack.getNbt().getString("PuzzleId");
        }
        if (puzzleId.isEmpty()) {
            puzzleId = DraftSettings.selected(player.getUuid()).orElse(null);
        }
        if (puzzleId == null || puzzleId.isEmpty()) {
            player.sendMessage(Text.translatable("message.puzzleescape.no_selected"), true);
            return ActionResult.FAIL;
        }

        Direction side = context.getSide();
        if (side.getAxis().isVertical()) {
            player.sendMessage(Text.translatable("message.puzzleescape.canvas_vertical_only"), true);
            return ActionResult.FAIL;
        }

        boolean placed = PuzzleManager.placeCanvas(
                player.getServer(),
                puzzleId,
                context.getBlockPos().offset(side),
                side,
                player
        );
        if (!placed) return ActionResult.FAIL;

        DraftSettings.select(player.getUuid(), puzzleId);
        if (stack.hasNbt() && stack.getNbt().getBoolean("ReadyCanvas") && !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}

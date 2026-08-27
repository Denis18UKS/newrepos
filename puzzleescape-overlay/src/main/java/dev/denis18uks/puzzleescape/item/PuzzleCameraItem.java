package dev.denis18uks.puzzleescape.item;

import dev.denis18uks.puzzleescape.network.PhotoPuzzleGuiPackets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class PuzzleCameraItem extends Item {
    public PuzzleCameraItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            PhotoPuzzleGuiPackets.openStudio(serverPlayer);
            return TypedActionResult.consume(stack);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}

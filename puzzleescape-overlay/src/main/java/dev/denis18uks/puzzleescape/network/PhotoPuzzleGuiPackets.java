package dev.denis18uks.puzzleescape.network;

import dev.denis18uks.puzzleescape.core.PhotoPuzzleKitPlan;
import dev.denis18uks.puzzleescape.item.PuzzlePieceItem;
import dev.denis18uks.puzzleescape.registry.ModItems;
import dev.denis18uks.puzzleescape.server.DraftSettings;
import dev.denis18uks.puzzleescape.server.PuzzleDefinition;
import dev.denis18uks.puzzleescape.server.PuzzleManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PhotoPuzzleGuiPackets {
    private static final int MIN_GRID = 1;
    private static final int MAX_GRID = 32;
    private static final Map<UUID, String> PENDING_KITS = new HashMap<>();

    private PhotoPuzzleGuiPackets() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PhotoPuzzlePacketIds.CREATE_AND_CAPTURE,
                (server, player, handler, buf, responseSender) -> {
                    String id = buf.readString(64).trim();
                    int columns = buf.readInt();
                    int rows = buf.readInt();
                    server.execute(() -> createAndCapture(server, player, id, columns, rows));
                });

        ServerPlayNetworking.registerGlobalReceiver(PhotoPuzzlePacketIds.RECAPTURE,
                (server, player, handler, buf, responseSender) -> {
                    String id = buf.readString(64).trim();
                    server.execute(() -> recapture(server, player, id));
                });

        ServerPlayNetworking.registerGlobalReceiver(PhotoPuzzlePacketIds.GIVE_CANVAS,
                (server, player, handler, buf, responseSender) -> {
                    String id = buf.readString(64).trim();
                    server.execute(() -> giveCanvas(server, player, id));
                });

        ServerPlayNetworking.registerGlobalReceiver(PhotoPuzzlePacketIds.GIVE_PIECES,
                (server, player, handler, buf, responseSender) -> {
                    String id = buf.readString(64).trim();
                    server.execute(() -> givePieces(server, player, id));
                });

        ServerTickEvents.END_SERVER_TICK.register(PhotoPuzzleGuiPackets::tickPendingKits);
    }

    public static void openStudio(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, PhotoPuzzlePacketIds.OPEN_STUDIO, ModPackets.newBuffer());
    }

    private static void createAndCapture(MinecraftServer server, ServerPlayerEntity player,
                                         String id, int columns, int rows) {
        if (!validId(id)) {
            player.sendMessage(Text.translatable("message.puzzleescape.gui_invalid_id"), false);
            return;
        }
        if (columns < MIN_GRID || rows < MIN_GRID || columns > MAX_GRID || rows > MAX_GRID) {
            player.sendMessage(Text.translatable("message.puzzleescape.gui_invalid_size", MAX_GRID), false);
            return;
        }
        if (PuzzleManager.get(server, id).isPresent()) {
            player.sendMessage(Text.translatable("message.puzzleescape.gui_duplicate", id), false);
            return;
        }

        PuzzleDefinition definition;
        try {
            definition = PuzzleManager.create(server, id, columns, rows);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Text.literal(ex.getMessage()), false);
            return;
        }

        DraftSettings.select(player.getUuid(), id);
        PENDING_KITS.put(player.getUuid(), id);
        requestCapture(player, definition);
        player.sendMessage(Text.translatable("message.puzzleescape.gui_created", id, columns, rows), false);
    }

    private static void recapture(MinecraftServer server, ServerPlayerEntity player, String id) {
        PuzzleDefinition definition = PuzzleManager.get(server, id).orElse(null);
        if (definition == null) {
            player.sendMessage(Text.translatable("message.puzzleescape.gui_missing", id), false);
            return;
        }
        DraftSettings.select(player.getUuid(), id);
        requestCapture(player, definition);
        player.sendMessage(Text.translatable("message.puzzleescape.gui_recapture", id), false);
    }

    private static void giveCanvas(MinecraftServer server, ServerPlayerEntity player, String id) {
        PuzzleDefinition definition = readyDefinition(server, player, id);
        if (definition == null) return;
        DraftSettings.select(player.getUuid(), id);
        giveReadyCanvas(player, id);
    }

    private static void givePieces(MinecraftServer server, ServerPlayerEntity player, String id) {
        PuzzleDefinition definition = readyDefinition(server, player, id);
        if (definition == null) return;
        DraftSettings.select(player.getUuid(), id);
        giveAllPieces(player, definition);
    }

    private static PuzzleDefinition readyDefinition(MinecraftServer server, ServerPlayerEntity player, String id) {
        PuzzleDefinition definition = PuzzleManager.get(server, id).orElse(null);
        if (definition == null) {
            player.sendMessage(Text.translatable("message.puzzleescape.gui_missing", id), false);
            return null;
        }
        if (definition.captureWidth() <= 0 || definition.captureHeight() <= 0) {
            player.sendMessage(Text.translatable("message.puzzleescape.gui_no_photo", id), false);
            return null;
        }
        return definition;
    }

    private static void requestCapture(ServerPlayerEntity player, PuzzleDefinition definition) {
        PacketByteBuf out = ModPackets.newBuffer();
        out.writeString(definition.id());
        out.writeInt(definition.columns());
        out.writeInt(definition.rows());
        ServerPlayNetworking.send(player, ModPackets.CAPTURE_REQUEST, out);
    }

    private static void tickPendingKits(MinecraftServer server) {
        Iterator<Map.Entry<UUID, String>> iterator = PENDING_KITS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, String> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;

            PuzzleDefinition definition = PuzzleManager.get(server, entry.getValue()).orElse(null);
            if (definition == null) {
                iterator.remove();
                continue;
            }
            if (definition.captureWidth() > 0 && definition.captureHeight() > 0) {
                giveCreationKit(player, definition);
                iterator.remove();
            }
        }
    }

    private static void giveCreationKit(ServerPlayerEntity player, PuzzleDefinition definition) {
        giveReadyCanvas(player, definition.id());
        giveAllPieces(player, definition);
        player.sendMessage(Text.translatable("message.puzzleescape.gui_kit_given",
                definition.id(), definition.columns() * definition.rows()), false);
    }

    private static void giveReadyCanvas(ServerPlayerEntity player, String id) {
        ItemStack stack = new ItemStack(ModItems.CANVAS_TOOL);
        stack.getOrCreateNbt().putString("PuzzleId", id);
        stack.getOrCreateNbt().putBoolean("ReadyCanvas", true);
        stack.setCustomName(Text.translatable("item.puzzleescape.ready_canvas", id));
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
        player.sendMessage(Text.translatable("message.puzzleescape.gui_canvas_given", id), false);
    }

    private static void giveAllPieces(ServerPlayerEntity player, PuzzleDefinition definition) {
        int dropped = 0;
        int[] pieces = PhotoPuzzleKitPlan.pieceIndices(definition.columns(), definition.rows());
        for (int pieceIndex : pieces) {
            ItemStack stack = PuzzlePieceItem.create(ModItems.PUZZLE_PIECE, definition.id(), pieceIndex, 0);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
                dropped++;
            }
        }
        player.sendMessage(Text.translatable("message.puzzleescape.gui_pieces_given",
                pieces.length, definition.id(), dropped), false);
    }

    private static boolean validId(String id) {
        if (id.isEmpty() || id.length() > 64) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.')) return false;
        }
        return true;
    }
}

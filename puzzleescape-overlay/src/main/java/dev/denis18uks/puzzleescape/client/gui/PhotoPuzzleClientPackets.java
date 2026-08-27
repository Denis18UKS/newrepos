package dev.denis18uks.puzzleescape.client.gui;

import dev.denis18uks.puzzleescape.network.PhotoPuzzlePacketIds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PhotoPuzzleClientPackets {
    private PhotoPuzzleClientPackets() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PhotoPuzzlePacketIds.OPEN_STUDIO,
                (client, handler, buf, responseSender) ->
                        client.execute(() -> client.setScreen(new PhotoPuzzleLibraryScreen())));
    }
}

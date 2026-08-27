package dev.denis18uks.puzzleescape.network;

import dev.denis18uks.puzzleescape.PuzzleEscape;
import net.minecraft.util.Identifier;

public final class PhotoPuzzlePacketIds {
    public static final Identifier OPEN_STUDIO = PuzzleEscape.id("open_photo_studio");
    public static final Identifier CREATE_AND_CAPTURE = PuzzleEscape.id("gui_create_capture");
    public static final Identifier RECAPTURE = PuzzleEscape.id("gui_recapture");
    public static final Identifier GIVE_CANVAS = PuzzleEscape.id("gui_give_canvas");

    private PhotoPuzzlePacketIds() {}
}

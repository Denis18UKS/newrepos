package dev.denis18uks.puzzleescape.core;

public final class PreviewLayout {
    private PreviewLayout() {}

    public static Size fit(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || maxWidth <= 0 || maxHeight <= 0) {
            return new Size(0, 0);
        }
        double scale = Math.min(maxWidth / (double) sourceWidth, maxHeight / (double) sourceHeight);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new Size(width, height);
    }

    public record Size(int width, int height) {}
}

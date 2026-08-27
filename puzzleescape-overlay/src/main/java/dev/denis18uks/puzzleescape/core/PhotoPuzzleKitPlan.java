package dev.denis18uks.puzzleescape.core;

public final class PhotoPuzzleKitPlan {
    private PhotoPuzzleKitPlan() {}

    public static int[] pieceIndices(int columns, int rows) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException("grid must be positive");
        }
        int total = Math.multiplyExact(columns, rows);
        int[] indices = new int[total];
        for (int i = 0; i < total; i++) {
            indices[i] = i;
        }
        return indices;
    }
}

package dev.denis18uks.puzzleescape.client.gui;

import dev.denis18uks.puzzleescape.client.state.ClientPuzzleDefinition;
import dev.denis18uks.puzzleescape.client.state.PuzzleClientState;
import dev.denis18uks.puzzleescape.network.PhotoPuzzlePacketIds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PhotoPuzzleLibraryScreen extends Screen {
    private static final int PAGE_SIZE = 7;
    private final List<ClientPuzzleDefinition> puzzles = new ArrayList<>();
    private int page;
    private int selected = -1;
    private ButtonWidget recaptureButton;
    private ButtonWidget canvasButton;

    public PhotoPuzzleLibraryScreen() {
        super(Text.translatable("screen.puzzleescape.library.title"));
    }

    @Override
    protected void init() {
        reload();
        int center = width / 2;
        int bottom = height - 52;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.puzzleescape.library.new"),
                button -> client.setScreen(new PhotoPuzzleCreateScreen(this)))
                .dimensions(center - 154, bottom, 100, 20).build());

        recaptureButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.puzzleescape.library.recapture"),
                button -> sendSelected(PhotoPuzzlePacketIds.RECAPTURE, true))
                .dimensions(center - 50, bottom, 100, 20).build());

        canvasButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.puzzleescape.library.canvas"),
                button -> sendSelected(PhotoPuzzlePacketIds.GIVE_CANVAS, false))
                .dimensions(center + 54, bottom, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> previousPage())
                .dimensions(center - 65, bottom - 26, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> nextPage())
                .dimensions(center + 35, bottom - 26, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(center - 50, bottom + 24, 100, 20).build());
        updateButtons();
    }

    private void reload() {
        puzzles.clear();
        puzzles.addAll(PuzzleClientState.all());
        puzzles.sort(Comparator.comparing(ClientPuzzleDefinition::id, String.CASE_INSENSITIVE_ORDER));
        int maxPage = Math.max(0, (puzzles.size() - 1) / PAGE_SIZE);
        if (page > maxPage) page = maxPage;
        if (selected >= puzzles.size()) selected = -1;
    }

    private void previousPage() {
        if (page > 0) {
            page--;
            selected = -1;
            updateButtons();
        }
    }

    private void nextPage() {
        if ((page + 1) * PAGE_SIZE < puzzles.size()) {
            page++;
            selected = -1;
            updateButtons();
        }
    }

    private void sendSelected(Identifier packetId, boolean closeAfter) {
        ClientPuzzleDefinition definition = selectedDefinition();
        if (definition == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(definition.id());
        ClientPlayNetworking.send(packetId, buf);
        if (closeAfter) close();
    }

    private ClientPuzzleDefinition selectedDefinition() {
        return selected >= 0 && selected < puzzles.size() ? puzzles.get(selected) : null;
    }

    private void updateButtons() {
        boolean active = selectedDefinition() != null;
        if (recaptureButton != null) recaptureButton.active = active;
        if (canvasButton != null) canvasButton.active = active;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int center = width / 2;
        int left = center - 155;
        int top = 50;
        int rowHeight = 22;
        if (button == 0 && mouseX >= left && mouseX <= left + 310) {
            int local = (int) mouseY - top;
            if (local >= 0) {
                int row = local / rowHeight;
                if (row >= 0 && row < PAGE_SIZE) {
                    int index = page * PAGE_SIZE + row;
                    if (index < puzzles.size()) {
                        selected = index;
                        updateButtons();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int center = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, center, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.puzzleescape.library.help"), center, 32, 0xA0A0A0);

        int left = center - 155;
        int top = 50;
        int rowHeight = 22;
        int start = page * PAGE_SIZE;
        for (int row = 0; row < PAGE_SIZE; row++) {
            int index = start + row;
            if (index >= puzzles.size()) break;
            ClientPuzzleDefinition p = puzzles.get(index);
            int y = top + row * rowHeight;
            int bg = index == selected ? 0xAA4A7A9A : 0x88303030;
            context.fill(left, y, left + 310, y + 19, bg);
            String photo = p.captureWidth() > 0 ? "✓" : "—";
            context.drawTextWithShadow(textRenderer,
                    Text.literal(p.id() + "   " + p.columns() + "×" + p.rows() + "   фото: " + photo),
                    left + 6, y + 6, 0xFFFFFF);
        }

        int pages = Math.max(1, (puzzles.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal((page + 1) + " / " + pages), center, height - 75, 0xB0B0B0);

        ClientPuzzleDefinition selectedPuzzle = selectedDefinition();
        if (selectedPuzzle != null) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("screen.puzzleescape.library.selected", selectedPuzzle.id()),
                    center, height - 94, 0xE0E0E0);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}

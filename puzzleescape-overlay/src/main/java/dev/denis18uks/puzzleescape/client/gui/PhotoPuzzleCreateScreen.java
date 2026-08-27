package dev.denis18uks.puzzleescape.client.gui;

import dev.denis18uks.puzzleescape.network.PhotoPuzzlePacketIds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public final class PhotoPuzzleCreateScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget idField;
    private TextFieldWidget columnsField;
    private TextFieldWidget rowsField;
    private Text error = Text.empty();

    public PhotoPuzzleCreateScreen(Screen parent) {
        super(Text.translatable("screen.puzzleescape.create.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int y = height / 2 - 72;

        idField = new TextFieldWidget(textRenderer, center - 100, y, 200, 20,
                Text.translatable("screen.puzzleescape.create.id"));
        idField.setMaxLength(64);
        idField.setText("photo_puzzle");
        addDrawableChild(idField);

        columnsField = new TextFieldWidget(textRenderer, center - 100, y + 42, 96, 20,
                Text.translatable("screen.puzzleescape.create.columns"));
        columnsField.setText("4");
        addDrawableChild(columnsField);

        rowsField = new TextFieldWidget(textRenderer, center + 4, y + 42, 96, 20,
                Text.translatable("screen.puzzleescape.create.rows"));
        rowsField.setText("3");
        addDrawableChild(rowsField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.puzzleescape.create.capture"),
                button -> create())
                .dimensions(center - 100, y + 78, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"),
                button -> client.setScreen(parent))
                .dimensions(center - 100, y + 104, 200, 20).build());
        setInitialFocus(idField);
    }

    private void create() {
        String id = idField.getText().trim();
        int columns;
        int rows;
        try {
            columns = Integer.parseInt(columnsField.getText().trim());
            rows = Integer.parseInt(rowsField.getText().trim());
        } catch (NumberFormatException ex) {
            error = Text.translatable("screen.puzzleescape.create.error_number");
            return;
        }
        if (!validId(id)) {
            error = Text.translatable("screen.puzzleescape.create.error_id");
            return;
        }
        if (columns < 1 || rows < 1 || columns > 32 || rows > 32) {
            error = Text.translatable("screen.puzzleescape.create.error_size");
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id);
        buf.writeInt(columns);
        buf.writeInt(rows);
        ClientPlayNetworking.send(PhotoPuzzlePacketIds.CREATE_AND_CAPTURE, buf);
        client.setScreen(null);
    }

    private static boolean validId(String id) {
        if (id.isEmpty() || id.length() > 64) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.')) return false;
        }
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int center = width / 2;
        int y = height / 2 - 72;
        context.drawCenteredTextWithShadow(textRenderer, title, center, y - 28, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.puzzleescape.create.id"),
                center - 100, y - 12, 0xB0B0B0);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.puzzleescape.create.columns"),
                center - 100, y + 30, 0xB0B0B0);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.puzzleescape.create.rows"),
                center + 4, y + 30, 0xB0B0B0);
        if (!error.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, error, center, y + 132, 0xFF6060);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}

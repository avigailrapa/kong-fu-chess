package src.view;

import engine.MoveEvent;
import engine.MoveLogger;
import src.model.Position;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Renderer {

    private static final String BOARD_IMAGE_PATH = "assets/board.png";
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_PADDING = 16;
    private static final int LOG_LINE_HEIGHT = 26;
    private static final int ROW_LABEL_WIDTH = 32;
    private static final int COL_LABEL_HEIGHT = 28;
    private static final float COORD_FONT_SIZE = 1.0f;

    private final String piecesRoot;
    private final MoveLogger moveLogger;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    private final Map<String, Integer> frameCountCache = new HashMap<>();
    private final Map<String, AnimationConfig> configCache = new HashMap<>();
    private final Set<String> missingSpriteWarnings = new HashSet<>();

    public Renderer() {
        this("assets/pieces", null);
    }

    public Renderer(String piecesRoot) {
        this(piecesRoot, null);
    }

    public Renderer(String piecesRoot, MoveLogger moveLogger) {
        this.piecesRoot = piecesRoot;
        this.moveLogger = moveLogger;
    }

    public int boardOffsetX() {
        return PANEL_WIDTH + ROW_LABEL_WIDTH;
    }

    public int boardOffsetY() {
        return COL_LABEL_HEIGHT;
    }

    public BufferedImage render(GameSnapshot snapshot) {
        BufferedImage boardImage = cachedImage(BOARD_IMAGE_PATH, null);
        int boardWidth = boardImage.getWidth();
        int boardHeight = boardImage.getHeight();
        int boardOffsetX = boardOffsetX();
        int boardOffsetY = boardOffsetY();
        int fullWidth = boardOffsetX + boardWidth + PANEL_WIDTH;
        int fullHeight = boardOffsetY + boardHeight;

        BufferedImage fullCanvas = new BufferedImage(fullWidth, fullHeight, BufferedImage.TYPE_INT_ARGB);
        Img canvas = new Img(fullCanvas);
        canvas.fillRect(0, 0, fullWidth, fullHeight, new Color(18, 18, 22));

        new Img(boardImage).drawOn(canvas, boardOffsetX, boardOffsetY);
        drawBoardCoordinates(canvas, boardOffsetX, boardOffsetY, boardWidth, boardHeight);

        List<MoveEvent> blackMoves = moveLogger == null ? List.of() : moveLogger.getBlackMoves();
        List<MoveEvent> whiteMoves = moveLogger == null ? List.of() : moveLogger.getWhiteMoves();
        drawSidePanel(canvas, 0, fullHeight, "Black", snapshot.getBlackScore(), blackMoves);
        drawSidePanel(canvas, boardOffsetX + boardWidth, fullHeight, "White", snapshot.getWhiteScore(), whiteMoves);

        drawSelection(canvas, snapshot, boardOffsetX, boardOffsetY);

        for (int row = 0; row < snapshot.height(); row++) {
            for (int col = 0; col < snapshot.width(); col++) {
                PieceSnapshot piece = snapshot.pieceAt(new Position(row, col));
                if (piece == null) {
                    continue;
                }
                drawPiece(piece, canvas, boardOffsetX, boardOffsetY);
            }
        }

        if (snapshot.isGameOver()) {
            int textX = boardOffsetX + boardWidth / 2 - 70;
            canvas.putText(snapshot.winner() + " WINS!", textX, boardOffsetY + boardHeight / 2, 1.8f, new Color(220, 20, 20), 0);
        }

        return canvas.get();
    }

    private void drawSidePanel(Img canvas, int panelX, int fullHeight, String label, int score, List<MoveEvent> moves) {
        canvas.fillRect(panelX, 0, PANEL_WIDTH, fullHeight, new Color(28, 28, 34));

        int textX = panelX + PANEL_PADDING;
        int y = PANEL_PADDING + 18;
        canvas.putText(label + ": " + score, textX, y, 1.6f, new Color(240, 240, 240), 0);
        y += 40;

        int maxVisibleRows = Math.max(0, (fullHeight - y - PANEL_PADDING) / LOG_LINE_HEIGHT);
        int firstVisible = Math.max(0, moves.size() - maxVisibleRows);
        for (int i = firstVisible; i < moves.size(); i++) {
            canvas.putText(formatMoveText(moves.get(i)), textX, y, 1.2f, new Color(220, 220, 220), 0);
            y += LOG_LINE_HEIGHT;
        }
    }

    private String formatMoveText(MoveEvent moveEvent) {
        return moveEvent.formattedRequestTime() + " " + moveEvent.algebraicMove();
    }

    private void drawBoardCoordinates(Img canvas, int boardOffsetX, int boardOffsetY, int boardWidth, int boardHeight) {
        for (int col = 0; col < 8; col++) {
            int x = boardOffsetX + (int) Math.round(col * GameSnapshot.CELL_WIDTH + GameSnapshot.CELL_WIDTH / 2.0) - 6;
            int y = boardOffsetY - 10;
            canvas.putText(String.valueOf((char) ('a' + col)), x, y, COORD_FONT_SIZE, new Color(220, 220, 220), 0);
        }

        for (int row = 0; row < 8; row++) {
            int x = boardOffsetX - ROW_LABEL_WIDTH + 10;
            int y = boardOffsetY + (int) Math.round(row * GameSnapshot.CELL_HEIGHT + GameSnapshot.CELL_HEIGHT / 2.0 + 6);
            canvas.putText(String.valueOf(8 - row), x, y, COORD_FONT_SIZE, new Color(220, 220, 220), 0);
        }
    }

    private void drawSelection(Img canvas, GameSnapshot snapshot, int boardOffsetX, int boardOffsetY) {
        Position selected = snapshot.selectedPosition();
        if (selected == null) {
            return;
        }

        int x = boardOffsetX + (int) Math.round(selected.getCol() * GameSnapshot.CELL_WIDTH);
        int y = boardOffsetY + (int) Math.round(selected.getRow() * GameSnapshot.CELL_HEIGHT);
        int width = (int) Math.round(GameSnapshot.CELL_WIDTH);
        int height = (int) Math.round(GameSnapshot.CELL_HEIGHT);

        canvas.drawRect(x, y, width, height, new Color(255, 215, 0), 4);
    }

    private void drawPiece(PieceSnapshot piece, Img canvas, int boardOffsetX, int boardOffsetY) {
        String path = spritePath(piece);
        if (!new File(path).isFile()) {
            if (missingSpriteWarnings.add(path)) {
                System.err.println("Renderer: missing sprite " + path + " - skipping piece " + piece.id());
            }
            return;
        }

        Dimension cellSize = new Dimension(
                (int) Math.round(GameSnapshot.CELL_WIDTH), (int) Math.round(GameSnapshot.CELL_HEIGHT));
        BufferedImage sprite = cachedImage(path, cellSize);
        new Img(sprite).drawOn(canvas, boardOffsetX + piece.pixelX(), boardOffsetY + piece.pixelY());
    }

    private BufferedImage cachedImage(String path, Dimension size) {
        String key = size == null ? path : path + "@" + size.width + "x" + size.height;
        return imageCache.computeIfAbsent(key, unused -> new Img().read(path, size, true, null).get());
    }

    private String spritePath(PieceSnapshot piece) {
        String code = "" + piece.kind().letter() + piece.color().letter();
        String stateFolder = stateFolder(piece.state());
        String configPath = piecesRoot + "/" + code + "/states/" + stateFolder + "/config.json";
        
        AnimationConfig config = configCache.computeIfAbsent(configPath, 
            AnimationConfig::load);
        
        String spritesDir = piecesRoot + "/" + code + "/states/" + stateFolder + "/sprites";
        int frameCount = frameCount(spritesDir);
        
        long frameDurationMs = 1000 / config.framesPerSecond();
        int frame;
        
        if (config.isLoop()) {
            frame = (int) ((piece.elapsedMillis() / frameDurationMs) % frameCount) + 1;
        } else {
            frame = Math.min((int) (piece.elapsedMillis() / frameDurationMs) + 1, frameCount);
        }
        
        return spritesDir + "/" + frame + ".png";
    }

    private int frameCount(String spritesDir) {
        return frameCountCache.computeIfAbsent(spritesDir, dir -> {
            String[] files = new File(dir).list((d, name) -> name.toLowerCase().endsWith(".png"));
            int count = files == null ? 0 : files.length;
            return Math.max(count, 1);
        });
    }

    private String stateFolder(PieceSnapshot.RenderState state) {
        switch (state) {
            case MOVING: return "move";
            case JUMPING: return "jump";
            case LONG_REST: return "long_rest";
            case SHORT_REST: return "short_rest";
            default: return "idle";
        }
    }
}
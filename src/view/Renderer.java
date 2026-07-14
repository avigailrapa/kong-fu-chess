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
    private static final int SIDE_PANEL_WIDTH = 260;
    private static final int PANEL_PADDING = 16;
    private static final int LOG_LINE_HEIGHT = 26;
    private static final float COORD_FONT_SIZE = 0.9f;

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

    public BufferedImage render(GameSnapshot snapshot) {
        BufferedImage boardImage = copyOf(cachedImage(BOARD_IMAGE_PATH, null));
        int fullWidth = boardImage.getWidth() + SIDE_PANEL_WIDTH;
        int fullHeight = boardImage.getHeight();
        BufferedImage fullCanvas = new BufferedImage(fullWidth, fullHeight, BufferedImage.TYPE_INT_ARGB);
        Img canvas = new Img(fullCanvas);

        new Img(boardImage).drawOn(canvas, 0, 0);
        drawBoardCoordinates(canvas);
        drawSidePanel(canvas, snapshot);

        for (int row = 0; row < snapshot.height(); row++) {
            for (int col = 0; col < snapshot.width(); col++) {
                PieceSnapshot piece = snapshot.pieceAt(new Position(row, col));
                if (piece == null) {
                    continue;
                }
                drawPiece(piece, canvas);
            }
        }

        return canvas.get();
    }

    private void drawSidePanel(Img canvas, GameSnapshot snapshot) {
        int boardWidth = cachedImage(BOARD_IMAGE_PATH, null).getWidth();
        int panelX = boardWidth;
        int panelY = 0;
        int panelWidth = SIDE_PANEL_WIDTH;
        int panelHeight = canvas.get().getHeight();

        canvas.fillRect(panelX, panelY, panelWidth, panelHeight, new Color(28, 28, 34));

        int titleX = panelX + PANEL_PADDING;
        int currentY = PANEL_PADDING + 16;
        canvas.putText("White: " + snapshot.getWhiteScore(), titleX, currentY, 1.5f, new Color(240, 240, 240), 0);
        currentY += 28;
        canvas.putText("Black: " + snapshot.getBlackScore(), titleX, currentY, 1.5f, new Color(240, 240, 240), 0);

        currentY += 40;
        drawMoveLog(canvas, panelX, currentY, panelWidth);

        if (snapshot.isGameOver()) {
            currentY = panelHeight - PANEL_PADDING;
            canvas.putText(snapshot.winner() + " WINS!", titleX, currentY - 10, 1.6f, new Color(220, 20, 20), 0);
        }
    }

    private void drawMoveLog(Img canvas, int panelX, int startY, int panelWidth) {
        if (moveLogger == null) {
            return;
        }

        List<MoveEvent> whiteMoves = moveLogger.getWhiteMoves();
        List<MoveEvent> blackMoves = moveLogger.getBlackMoves();
        int columnWidth = (panelWidth - PANEL_PADDING * 2) / 2;
        int whiteX = panelX + PANEL_PADDING;
        int blackX = panelX + PANEL_PADDING + columnWidth;
        int y = startY;

        canvas.putText("W", whiteX, y, 1.4f, new Color(190, 190, 190), 0);
        canvas.putText("B", blackX, y, 1.4f, new Color(190, 190, 190), 0);
        y += LOG_LINE_HEIGHT;

        int rows = Math.max(whiteMoves.size(), blackMoves.size());
        for (int row = 0; row < rows; row++) {
            if (row < whiteMoves.size()) {
                canvas.putText(formatMoveText(whiteMoves.get(row)), whiteX, y, 1.2f, new Color(220, 220, 220), 0);
            }
            if (row < blackMoves.size()) {
                canvas.putText(formatMoveText(blackMoves.get(row)), blackX, y, 1.2f, new Color(220, 220, 220), 0);
            }
            y += LOG_LINE_HEIGHT;
        }
    }

    private String formatMoveText(MoveEvent moveEvent) {
        return moveEvent.formattedRequestTime() + " " + moveEvent.algebraicMove();
    }

    private void drawBoardCoordinates(Img canvas) {
        int boardWidth = cachedImage(BOARD_IMAGE_PATH, null).getWidth();
        int boardHeight = cachedImage(BOARD_IMAGE_PATH, null).getHeight();
        int cols = 8;
        int rows = 8;

        for (int col = 0; col < cols; col++) {
            int x = (int) Math.round(col * GameSnapshot.CELL_WIDTH + GameSnapshot.CELL_WIDTH / 2.0);
            int y = boardHeight - 6;
            canvas.putText(String.valueOf((char) ('a' + col)), x - 6, y, COORD_FONT_SIZE, new Color(220, 220, 220), 0);
        }

        for (int row = 0; row < rows; row++) {
            int x = 8;
            int y = (int) Math.round(row * GameSnapshot.CELL_HEIGHT + GameSnapshot.CELL_HEIGHT / 2.0 + 6);
            canvas.putText(String.valueOf(8 - row), x, y, COORD_FONT_SIZE, new Color(220, 220, 220), 0);
        }
    }

    private void drawPiece(PieceSnapshot piece, Img canvas) {
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
        new Img(sprite).drawOn(canvas, piece.pixelX(), piece.pixelY());
    }

    private BufferedImage cachedImage(String path, Dimension size) {
        String key = size == null ? path : path + "@" + size.width + "x" + size.height;
        return imageCache.computeIfAbsent(key, unused -> new Img().read(path, size, true, null).get());
    }

    private BufferedImage copyOf(BufferedImage source) {
        BufferedImage blank = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Img copy = new Img(blank);
        new Img(source).drawOn(copy, 0, 0);
        return blank;
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
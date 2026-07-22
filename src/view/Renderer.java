package src.view;

import lombok.RequiredArgsConstructor;
import src.model.Piece;
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

@RequiredArgsConstructor
public class Renderer {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_PADDING = 16;
    private static final int LOG_LINE_HEIGHT = 34;
    private static final float LOG_FONT_SIZE = 1.8f;
    private static final float SCORE_FONT_SIZE = 1.8f;
    private static final int ROW_LABEL_WIDTH = 32;
    private static final int COL_LABEL_HEIGHT = 28;
    private static final int TITLE_HEIGHT = 50;
    private static final String TITLE_TEXT = "♟ KUNG FU CHESS ♟";
    private static final float TITLE_FONT_SIZE = 2.2f;
    private static final Color TITLE_COLOR = new Color(255, 255, 255);
    private static final float COORD_FONT_SIZE = 1.6f;
    private static final Color BOARD_BORDER_COLOR = new Color(94, 61, 28);
    private static final Color LEGAL_MOVE_MARKER_COLOR = new Color(128, 128, 128, 150);
    private static final float GAME_OVER_FONT_SIZE = 3.0f;
    private static final Color GAME_OVER_TEXT_COLOR = new Color(255, 255, 255);
    private static final Color GAME_OVER_BACKGROUND_COLOR = new Color(0, 0, 0, 140);
    private static final int GAME_OVER_BAND_HEIGHT = 100;
    private static final Color LEGAL_CAPTURE_MARKER_COLOR = new Color(220, 40, 40, 160);
    private static final Color REST_COOLDOWN_COLOR = new Color(255, 215, 0);
    private static final int REST_COOLDOWN_MAX_ALPHA = 180;
    private static final String BOARD_IMAGE_FILENAME = "board.png";
    private static final Color BANNER_TEXT_COLOR = new Color(255, 215, 0);
    private static final Color BANNER_BACKGROUND_COLOR = new Color(0, 0, 0, 160);
    private static final float BANNER_FONT_SIZE = 2.4f;
    private static final int BANNER_HEIGHT = 60;

    private final String piecesRoot;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    private final Map<String, Integer> frameCountCache = new HashMap<>();
    private final Map<String, AnimationConfig> configCache = new HashMap<>();
    private final Set<String> missingSpriteWarnings = new HashSet<>();
    private BufferedImage boardBackgroundImage;
    private int boardBackgroundImageWidth = -1;
    private int boardBackgroundImageHeight = -1;

    public int boardOffsetX() {
        return PANEL_WIDTH + ROW_LABEL_WIDTH;
    }

    public int boardOffsetY() {
        return TITLE_HEIGHT + COL_LABEL_HEIGHT;
    }

    public void drawBanner(BufferedImage image, String text) {
        Img canvas = new Img(image);
        int boardOffsetX = boardOffsetX();
        int boardWidth = image.getWidth() - boardOffsetX - PANEL_WIDTH;
        int bandY = boardOffsetY();
        canvas.fillRect(boardOffsetX, bandY, boardWidth, BANNER_HEIGHT, BANNER_BACKGROUND_COLOR);

        int textX = boardOffsetX + boardWidth / 2 - (text.length() * 11);
        int textY = bandY + BANNER_HEIGHT / 2 + 14;
        canvas.putText(text, textX, textY, BANNER_FONT_SIZE, BANNER_TEXT_COLOR, 0);
    }

    public BufferedImage render(GameSnapshot snapshot) {
        double cellWidth = snapshot.cellWidth();
        double cellHeight = snapshot.cellHeight();
        int boardWidth = (int) Math.round(snapshot.width() * cellWidth);
        int boardHeight = (int) Math.round(snapshot.height() * cellHeight);
        int boardOffsetX = boardOffsetX();
        int boardOffsetY = boardOffsetY();
        int fullWidth = boardOffsetX + boardWidth + PANEL_WIDTH;
        int fullHeight = boardOffsetY + boardHeight;

        BufferedImage fullCanvas = new BufferedImage(fullWidth, fullHeight, BufferedImage.TYPE_INT_ARGB);
        Img canvas = new Img(fullCanvas);
        canvas.fillRect(0, 0, fullWidth, fullHeight, new Color(18, 18, 22));

        drawTitle(canvas, boardOffsetX, boardWidth);
        drawBoardBackground(canvas, boardOffsetX, boardOffsetY, boardWidth, boardHeight);
        canvas.drawRect(boardOffsetX - 4, boardOffsetY - 4, boardWidth + 8, boardHeight + 8, BOARD_BORDER_COLOR, 4);
        drawBoardCoordinates(canvas, boardOffsetX, boardOffsetY, cellWidth, cellHeight);

        drawSidePanel(canvas, 0, fullHeight, "Black", snapshot.blackScore(), snapshot.blackMoveLog());
        drawSidePanel(canvas, boardOffsetX + boardWidth, fullHeight, "White", snapshot.whiteScore(), snapshot.whiteMoveLog());

        drawSelection(canvas, snapshot, boardOffsetX, boardOffsetY, cellWidth, cellHeight);
        drawLegalMoves(canvas, snapshot, boardOffsetX, boardOffsetY, cellWidth, cellHeight);

        for (int row = 0; row < snapshot.height(); row++) {
            for (int col = 0; col < snapshot.width(); col++) {
                PieceSnapshot piece = snapshot.pieceAt(new Position(row, col));
                if (piece == null) {
                    continue;
                }
                drawRestCooldown(piece, row, col, canvas, boardOffsetX, boardOffsetY, cellWidth, cellHeight);
                drawPiece(piece, canvas, boardOffsetX, boardOffsetY, cellWidth, cellHeight);
            }
        }

        if (snapshot.gameOver()) {
            drawGameOver(canvas, snapshot, boardOffsetX, boardOffsetY, boardWidth, boardHeight);
        }

        return canvas.img();
    }

    private void drawGameOver(Img canvas, GameSnapshot snapshot, int boardOffsetX, int boardOffsetY, int boardWidth, int boardHeight) {
        String caption = snapshot.winner() + " WINS!";
        int bandY = boardOffsetY + boardHeight / 2 - GAME_OVER_BAND_HEIGHT / 2;
        canvas.fillRect(boardOffsetX, bandY, boardWidth, GAME_OVER_BAND_HEIGHT, GAME_OVER_BACKGROUND_COLOR);

        int textX = boardOffsetX + boardWidth / 2 - (caption.length() * 11);
        int textY = bandY + GAME_OVER_BAND_HEIGHT / 2 + 14;
        canvas.putText(caption, textX, textY, GAME_OVER_FONT_SIZE, GAME_OVER_TEXT_COLOR, 0);
    }

    private void drawTitle(Img canvas, int boardOffsetX, int boardWidth) {
        int textX = boardOffsetX + boardWidth / 2 - (TITLE_TEXT.length() * 8);
        int textY = TITLE_HEIGHT - 12;
        canvas.putText(TITLE_TEXT, textX, textY, TITLE_FONT_SIZE, TITLE_COLOR, 0);
    }

    private void drawBoardBackground(Img canvas, int boardOffsetX, int boardOffsetY, int boardWidth, int boardHeight) {
        BufferedImage background = boardBackgroundImage(boardWidth, boardHeight);
        new Img(background).drawOn(canvas, boardOffsetX, boardOffsetY);
    }

    private BufferedImage boardBackgroundImage(int boardWidth, int boardHeight) {
        if (boardWidth != boardBackgroundImageWidth || boardHeight != boardBackgroundImageHeight) {
            File file = new File(piecesRoot).getParentFile();
            String path = new File(file, BOARD_IMAGE_FILENAME).getPath();
            boardBackgroundImage = new Img().read(path, new Dimension(boardWidth, boardHeight), false, null).img();
            boardBackgroundImageWidth = boardWidth;
            boardBackgroundImageHeight = boardHeight;
        }
        return boardBackgroundImage;
    }

    private void drawSidePanel(Img canvas, int panelX, int fullHeight, String label, int score, List<String> moves) {
        canvas.fillRect(panelX, 0, PANEL_WIDTH, fullHeight, new Color(28, 28, 34));

        int textX = panelX + PANEL_PADDING;
        int y = PANEL_PADDING + 18;
        canvas.putText(label + ": " + score, textX, y, SCORE_FONT_SIZE, new Color(240, 240, 240), 0);
        y += 40;

        int maxVisibleRows = Math.max(0, (fullHeight - y - PANEL_PADDING) / LOG_LINE_HEIGHT);
        int firstVisible = Math.max(0, moves.size() - maxVisibleRows);
        for (int i = firstVisible; i < moves.size(); i++) {
            canvas.putText(moves.get(i), textX, y, LOG_FONT_SIZE, new Color(220, 220, 220), 0);
            y += LOG_LINE_HEIGHT;
        }
    }

    private void drawBoardCoordinates(Img canvas, int boardOffsetX, int boardOffsetY, double cellWidth, double cellHeight) {
        for (int col = 0; col < 8; col++) {
            int x = boardOffsetX + (int) Math.round(col * cellWidth + cellWidth / 2.0) - 6;
            int y = boardOffsetY - 10;
            canvas.putText(String.valueOf((char) ('a' + col)), x, y, COORD_FONT_SIZE, new Color(220, 220, 220), 0);
        }

        for (int row = 0; row < 8; row++) {
            int x = boardOffsetX - ROW_LABEL_WIDTH + 10;
            int y = boardOffsetY + (int) Math.round(row * cellHeight + cellHeight / 2.0) + 6;
            canvas.putText(String.valueOf(8 - row), x, y, COORD_FONT_SIZE, new Color(220, 220, 220), 0);
        }
    }

    private void drawSelection(Img canvas, GameSnapshot snapshot, int boardOffsetX, int boardOffsetY, double cellWidth, double cellHeight) {
        for (SelectionSnapshot selection : snapshot.selections()) {
            Position selected = selection.position();
            int x = boardOffsetX + (int) Math.round(selected.col() * cellWidth);
            int y = boardOffsetY + (int) Math.round(selected.row() * cellHeight);
            int width = (int) Math.round(cellWidth);
            int height = (int) Math.round(cellHeight);

            canvas.drawRect(x, y, width, height, new Color(255, 215, 0), 4);
        }
    }

    private void drawLegalMoves(Img canvas, GameSnapshot snapshot, int boardOffsetX, int boardOffsetY, double cellWidthD, double cellHeightD) {
        int cellWidth = (int) Math.round(cellWidthD);
        int cellHeight = (int) Math.round(cellHeightD);

        for (Position destination : snapshot.legalDestinations()) {
            int x = boardOffsetX + (int) Math.round(destination.col() * cellWidthD);
            int y = boardOffsetY + (int) Math.round(destination.row() * cellHeightD);
            Color markerColor = snapshot.isOccupied(destination) ? LEGAL_CAPTURE_MARKER_COLOR : LEGAL_MOVE_MARKER_COLOR;
            canvas.fillRect(x, y, cellWidth, cellHeight, markerColor);
        }
    }

    private void drawRestCooldown(PieceSnapshot piece, int row, int col, Img canvas, int boardOffsetX, int boardOffsetY, double cellWidthD, double cellHeightD) {
        boolean isResting = piece.state() == Piece.State.LONG_REST
                || piece.state() == Piece.State.SHORT_REST;
        if (!isResting || piece.restDurationMs() <= 0) {
            return;
        }

        double remainingFraction = 1.0 - Math.min(1.0, (double) piece.elapsedMillis() / piece.restDurationMs());
        if (remainingFraction <= 0) {
            return;
        }

        int x = boardOffsetX + (int) Math.round(col * cellWidthD);
        int y = boardOffsetY + (int) Math.round(row * cellHeightD);
        int width = (int) Math.round(cellWidthD);
        int height = (int) Math.round(cellHeightD);
        int fillHeight = (int) Math.round(height * remainingFraction);
        int fillY = y + (height - fillHeight);

        Color fillColor = new Color(REST_COOLDOWN_COLOR.getRed(), REST_COOLDOWN_COLOR.getGreen(),
                REST_COOLDOWN_COLOR.getBlue(), REST_COOLDOWN_MAX_ALPHA);
        canvas.fillRect(x, fillY, width, fillHeight, fillColor);
    }

    private void drawPiece(PieceSnapshot piece, Img canvas, int boardOffsetX, int boardOffsetY, double cellWidth, double cellHeight) {
        String path = spritePath(piece);
        if (!new File(path).isFile()) {
            if (missingSpriteWarnings.add(path)) {
                System.err.println("Renderer: missing sprite " + path + " - skipping piece " + piece.id());
            }
            return;
        }

        Dimension cellSize = new Dimension((int) Math.round(cellWidth), (int) Math.round(cellHeight));
        BufferedImage sprite = cachedImage(path, cellSize);
        new Img(sprite).drawOn(canvas, boardOffsetX + piece.pixelX(), boardOffsetY + piece.pixelY());
    }

    private BufferedImage cachedImage(String path, Dimension size) {
        String key = size == null ? path : path + "@" + size.width + "x" + size.height;
        return imageCache.computeIfAbsent(key, unused -> new Img().read(path, size, true, null).img());
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
        
        if (config.loop()) {
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

    private String stateFolder(Piece.State state) {
        return switch (state) {
            case MOVING -> "move";
            case JUMPING -> "jump";
            case LONG_REST -> "long_rest";
            case SHORT_REST -> "short_rest";
            default -> "idle";
        };
    }
}
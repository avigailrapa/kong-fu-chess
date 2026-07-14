package src.view;

import src.model.Piece;
import src.model.Position;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Renderer {

    private static final String BOARD_IMAGE_PATH = "assets/board.png";
    private static final long FRAME_DURATION_MS = 150;

    private final String piecesRoot;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    private final Map<String, Integer> frameCountCache = new HashMap<>();
    private final Set<String> missingSpriteWarnings = new HashSet<>();

    public Renderer() {
        this("assets/pieces");
    }

    public Renderer(String piecesRoot) {
        this.piecesRoot = piecesRoot;
    }

    public BufferedImage render(GameSnapshot snapshot) {
        Img canvas = new Img(copyOf(cachedImage(BOARD_IMAGE_PATH, null)));

        for (int row = 0; row < snapshot.height(); row++) {
            for (int col = 0; col < snapshot.width(); col++) {
                PieceSnapshot piece = snapshot.pieceAt(new Position(row, col));
                if (piece == null) {
                    continue;
                }
                drawPiece(piece, canvas);
            }
        }

        if (snapshot.isGameOver()) {
            canvas.putText(snapshot.winner() + " WINS!", 20, 50, 2.0f, new Color(220, 20, 20), 0);
        }

        return canvas.get();
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
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private String spritePath(PieceSnapshot piece) {
        String code = "" + kindLetter(piece.kind()) + colorLetter(piece.color());
        String stateFolder = stateFolder(piece.state());
        String spritesDir = piecesRoot + "/" + code + "/states/" + stateFolder + "/sprites";
        int frameCount = frameCount(spritesDir);
        int frame = (int) ((piece.elapsedMillis() / FRAME_DURATION_MS) % frameCount) + 1;
        return spritesDir + "/" + frame + ".png";
    }

    private int frameCount(String spritesDir) {
        return frameCountCache.computeIfAbsent(spritesDir, dir -> {
            String[] files = new File(dir).list((d, name) -> name.toLowerCase().endsWith(".png"));
            int count = files == null ? 0 : files.length;
            return Math.max(count, 1);
        });
    }

    private char kindLetter(Piece.Kind kind) {
        switch (kind) {
            case KING: return 'K';
            case QUEEN: return 'Q';
            case ROOK: return 'R';
            case BISHOP: return 'B';
            case KNIGHT: return 'N';
            case PAWN: return 'P';
            default: throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }

    private char colorLetter(Piece.Color color) {
        return color == Piece.Color.WHITE ? 'W' : 'B';
    }

    private String stateFolder(Piece.State state) {
        switch (state) {
            case MOVING: return "move";
            case JUMPING: return "jump";
            case LONG_REST: return "long_rest";
            case SHORT_REST: return "short_rest";
            default: return "idle";
        }
    }
}

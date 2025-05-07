/**
 * Classe que representa un robot LightBot que es mou en una graella amb bombetes.
 * Admet inicialització amb String[] o String (amb \n)
 * Autor: ChatGPT | Data: 2024
 */
public class LightBot {
    // Graella inicial i actual
    private char[][] initialMap;
    private char[][] map;
    private int width, height;

    // Posició i direcció del robot
    private int startX, startY, robotX, robotY;
    private int startDir, robotDir; // 0:dreta, 1:abaix, 2:esquerra, 3:amunt

    // Vectors de moviment segons la direcció
    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DY = {0, 1, 0, -1};

    /**
     * Constructor que accepta directament String[] (una línia per fila).
     */
    public LightBot(String[] lines) {
        this(String.join("\n", lines));
    }

    /**
     * Constructor que accepta un String amb enters (\n) de separació de línies.
     */
    public LightBot(String mapString) {
        String[] lines = mapString.split("\n");
        height = lines.length;
        width = lines[0].length();
        initialMap = new char[height][width];
        map = new char[height][width];
        startX = startY = -1;
        startDir = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = lines[y].charAt(x);
                initialMap[y][x] = c;
                map[y][x] = c;
                // Busquem el robot
                if ("RLUD".indexOf(c) > -1) {
                    startX = x;
                    startY = y;
                    switch (c) {
                        case 'R': startDir = 0; break;
                        case 'D': startDir = 1; break;
                        case 'L': startDir = 2; break;
                        case 'U': startDir = 3; break;
                    }
                    // Després de guardar la posició inicial, deixem aquella cel·la buida ('.')
                    initialMap[y][x] = '.';
                    map[y][x] = '.';
                }
            }
        }

        if (startX == -1 || startY == -1)
            throw new IllegalArgumentException("No s'ha trobat el robot!");

        reset();
    }

    /**
     * Restaura l'estat inicial del robot i del mapa.
     */
    public void reset() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                map[y][x] = initialMap[y][x];
        robotX = startX;
        robotY = startY;
        robotDir = startDir;
    }

    /**
     * Executa una seqüència d'instruccions.
     */
    public void runProgram(String[] inst) {
        for (String cmd : inst)
            doInstruction(cmd);
    }

    /**
     * Executa una sola instrucció.
     */
    private void doInstruction(String cmd) {
        switch (cmd) {
            case "FORWARD":
                int nx = robotX + DX[robotDir];
                int ny = robotY + DY[robotDir];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height
                        && (map[ny][nx] == '.' || map[ny][nx] == 'O' || map[ny][nx] == 'X')) {
                    robotX = nx;
                    robotY = ny;
                }
                break;
            case "LEFT":
                robotDir = (robotDir + 3) % 4;
                break;
            case "RIGHT":
                robotDir = (robotDir + 1) % 4;
                break;
            case "LIGHT":
                if (map[robotY][robotX] == 'O') {
                    map[robotY][robotX] = 'X';
                } else if (map[robotY][robotX] == '.' || map[robotY][robotX] == 'X') {
                    map[robotY][robotX] = 'x';
                }
                // Si està en una 'x' ja, no canvia
                break;
        }
    }

    /**
     * Retorna la posició actual del robot: [columna, fila]
     */
    public int[] getRobotPosition() {
        return new int[]{robotX, robotY};
    }

    /**
     * Retorna el mapa actualitzat en format String[] (una línia per fila)
     */
    public String[] getMap() {
        String[] result = new String[height];
        for (int y = 0; y < height; y++) {
            result[y] = new String(map[y]);
        }
        return result;
    }
}
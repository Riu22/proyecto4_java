import java.util.*;
;

public class LightBot {
    private char[][] initialMap;
    private char[][] map;
    private int width, height;
    private int startX, startY, robotX, robotY;
    private int startDir, robotDir;
    private static final int[] DX = {1, 0, -1, 0}; // Derecha, Abajo, Izquierda, Arriba
    private static final int[] DY = {0, 1, 0, -1};
    private Map<String, UserFunction> functions = new HashMap<>();

    public LightBot(String[] lines) {
        this(String.join("\n", lines));
    }

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
                if ("RLUD".indexOf(c) > -1) {
                    startX = x;
                    startY = y;
                    switch (c) {
                        case 'R': startDir = 0; break; // Derecha
                        case 'D': startDir = 1; break; // Abajo
                        case 'L': startDir = 2; break; // Izquierda
                        case 'U': startDir = 3; break; // Arriba
                    }
                    initialMap[y][x] = '.';
                    map[y][x] = '.';
                }
            }
        }
        if (startX == -1 || startY == -1) {
            throw new IllegalArgumentException("No s'ha trobat el robot!");
        }
        reset();
    }

    public void reset() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = initialMap[y][x];
            }
        }
        robotX = startX;
        robotY = startY;
        robotDir = startDir;
    }

    public void runProgram(String[] inst) {
        List<String> code = Arrays.asList(inst);
        functions.clear();
        execute(code);
    }

    private void execute(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String cmd = code.get(i).trim();
            if (cmd.startsWith("FUNCTION ")) {
                String funcName = cmd.substring(9).trim();
                UserFunction newFunc = new UserFunction(funcName);
                i++;
                while (i < code.size() && !code.get(i).trim().equals("ENDFUNCTION")) {
                    newFunc.addInstruction(code.get(i).trim());
                    i++;
                }
                functions.put(funcName, newFunc);
            } else if (cmd.startsWith("CALL ")) {
                String funcName = cmd.substring(5).trim();
                UserFunction func = functions.get(funcName);
                if (func != null) {
                    execute(func.getInstructions());
                }
            } else if (cmd.startsWith("REPEAT ")) {
                int n = Integer.parseInt(cmd.substring(7).trim());
                int depth = 1;
                List<String> block = new ArrayList<>();
                i++;
                while (i < code.size() && depth > 0) {
                    String subCmd = code.get(i).trim();
                    if (subCmd.startsWith("REPEAT ")) {
                        depth++;
                    } else if (subCmd.equals("ENDREPEAT")) {
                        depth--;
                    }
                    if (depth > 0) block.add(subCmd);
                    i++;
                }
                for (int rep = 0; rep < n; rep++) {
                    execute(block);
                }
                i--;} else {
                doInstruction(cmd);
            }
        }
    }

    private void doInstruction(String cmd) {
        switch (cmd) {
            case "FORWARD":
                int nx = robotX + DX[robotDir];
                int ny = robotY + DY[robotDir];
                if (nx < 0) nx = width - 1;
                if (nx >= width) nx = 0;
                if (ny < 0) ny = height - 1;
                if (ny >= height) ny = 0;
                if (map[ny][nx] == '.' || map[ny][nx] == 'O' || map[ny][nx] == 'X') {
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
                }
                break;
        }
    }

    public int[] getRobotPosition() {
        return new int[]{robotX, robotY};
    }

    public String[] getMap() {
        String[] result = new String[height];
        for (int y = 0; y < height; y++) {
            result[y] = new String(map[y]);
        }
        return result;
    }

}

class UserFunction {
    private String name;
    private List<String> instructions;

    public UserFunction(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
    }

    public void addInstruction(String instruction) {
        instructions.add(instruction);
    }

    public String getName() {
        return name;
    }

    public List<String> getInstructions() {
        return instructions;
    }
}
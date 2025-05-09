import java.util.*;
public class LightBot {
    // Campos principales para el mapa, posición y dirección del robot
    private char[][] initialMap;  // Mapa inicial
    private char[][] map;         // Mapa actual
    private int width, height;    // Dimensiones del mapa
    private int startX, startY;   // Posición inicial del robot
    private int robotX, robotY;   // Posición actual del robot
    private int startDir, robotDir; // Dirección inicial y actual
    private final Map<String, UserFunction> functions = new HashMap<>();  // Almacena funciones definidas por el usuario
    private static final int[] DX = {1, 0, -1, 0};  // Desplazamientos en X
    private static final int[] DY = {0, 1, 0, -1};  // Desplazamientos en Y

    // Constructor que procesa un array de líneas
    public LightBot(String[] lines) {
        this(String.join("\n", lines));  // Une las líneas y pasa a otro constructor
    }

    // Constructor principal que inicializa el mapa y el robot
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
                        case 'R': startDir = 0; break;
                        case 'D': startDir = 1; break;
                        case 'L': startDir = 2; break;
                        case 'U': startDir = 3; break;
                    }
                    initialMap[y][x] = '.';
                    map[y][x] = '.';
                }
            }
        }
        if (startX == -1 || startY == -1)
            throw new IllegalArgumentException("No s'ha trobat el robot!");
        reset();
    }

    // Método para resetear el estado inicial
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

    // Método para ejecutar el programa
    public void runProgram(String[] programLines) {
        List<String> code = Arrays.asList(programLines);
        functions.clear();
        parseFunctions(code);
        List<Instruction> mainInstructions = parseInstructions(code, 0, code.size());
        for (Instruction instr : mainInstructions) {
            instr.execute(this);
        }
    }

    // Interfaz para instrucciones ejecutables
    interface Instruction {
        void execute(LightBot bot);  // Ejecuta la instrucción en el bot
    }

    // Clase para instrucciones simples
    static class SimpleInstruction implements Instruction {
        private final String cmd;  // Comando a ejecutar
        public SimpleInstruction(String cmd) { this.cmd = cmd; }
        public void execute(LightBot bot) { bot.doInstruction(cmd); }
    }

    // Clase para bloques de repetición
    static class RepeatBlock implements Instruction {
        private final int times;  // Número de repeticiones
        private final List<Instruction> instructions;  // Instrucciones a repetir
        public RepeatBlock(int times, List<Instruction> instructions) { this.times = times; this.instructions = instructions; }
        public void execute(LightBot bot) {
            for (int i = 0; i < times; i++) {
                for (Instruction instr : instructions) {
                    instr.execute(bot);
                }
            }
        }
    }

    // Clase para llamadas a funciones de usuario
    static class UserFunctionInstruction implements Instruction {
        private final String funcName;  // Nombre de la función
        public UserFunctionInstruction(String funcName) { this.funcName = funcName; }
        public void execute(LightBot bot) {
            UserFunction func = bot.functions.get(funcName);
            if (func != null) {
                for (Instruction instr : func.getInstructions()) {
                    instr.execute(bot);
                }
            }
        }
    }

    // Clase para representar una función de usuario
    static class UserFunction {
        private final String name;  // Nombre de la función
        private final List<Instruction> instructions;  // Instrucciones de la función
        public UserFunction(String name, List<Instruction> instructions) { this.name = name; this.instructions = instructions; }
        public String getName() { return name; }
        public List<Instruction> getInstructions() { return instructions; }
    }

    // Método para parsear funciones del código
    private void parseFunctions(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i).trim();
            if (line.startsWith("FUNCTION ")) {
                String funcName = line.substring(9).trim();
                int start = i + 1;
                int end = start;
                while (end < code.size() && !code.get(end).trim().equals("ENDFUNCTION"))
                    end++;
                List<Instruction> funcInstructions = parseInstructions(code, start, end);
                functions.put(funcName, new UserFunction(funcName, funcInstructions));
                i = end;
            }
        }
    }

    // Método para parsear instrucciones
    private List<Instruction> parseInstructions(List<String> code, int from, int to) {
        List<Instruction> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            String cmd = code.get(i).trim();
            if (cmd.isEmpty()) continue;
            if (cmd.startsWith("FUNCTION ")) {
                while (i < to && !code.get(i).trim().equals("ENDFUNCTION")) i++;
                continue;
            }
            if (cmd.startsWith("REPEAT ")) {
                int times = Integer.parseInt(cmd.substring(7).trim());
                int depth = 1;
                int blockStart = ++i;
                while (i < to && depth > 0) {
                    String line = code.get(i).trim();
                    if (line.startsWith("REPEAT ")) depth++;
                    else if (line.equals("ENDREPEAT")) depth--;
                    if (depth > 0) i++;
                }
                List<Instruction> repeatBlock = parseInstructions(code, blockStart, i);
                result.add(new RepeatBlock(times, repeatBlock));
                continue;
            }
            if (cmd.equals("ENDREPEAT") || cmd.equals("ENDFUNCTION"))
                continue;
            if (cmd.startsWith("CALL ")) {
                String funcName = cmd.substring(5).trim();
                result.add(new UserFunctionInstruction(funcName));
                continue;
            }
            result.add(new SimpleInstruction(cmd));
        }
        return result;
    }

    // Método para ejecutar una instrucción simple
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

    // Método para obtener la posición del robot
    public int[] getRobotPosition() {
        return new int[]{robotX, robotY};
    }

    // Método para obtener el mapa actual
    public String[] getMap() {
        String[] result = new String[height];
        for (int y = 0; y < height; y++) {
            result[y] = new String(map[y]);
        }
        return result;
    }
}
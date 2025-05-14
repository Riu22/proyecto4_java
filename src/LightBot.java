import java.util.*;

// Interfaz para instrucciones ejecutables
interface Instruction {
    void execute(LightBot bot);
}

// Representa una función (con o sin parámetros)
class UserFunction {
    private final String name;
    private final List<String> paramNames; // Nombres de los parámetros
    private final List<Instruction> instructions;

    public UserFunction(String name, List<String> paramNames, List<Instruction> instructions) {
        this.name = name;
        this.paramNames = paramNames;
        this.instructions = instructions;
    }

    public String getName() { return name; }
    public List<String> getParamNames() { return paramNames; }
    public List<Instruction> getInstructions() { return instructions; }
}

// Instrucción simple: FORWARD, LEFT, RIGHT, LIGHT
class SimpleInstruction implements Instruction {
    private final String cmd;
    public SimpleInstruction(String cmd) { this.cmd = cmd; }
    public void execute(LightBot bot) { bot.doInstruction(cmd); }
}

// Llamada a función parametrizada (CALL FOO(3,5))
class UserFunctionCall implements Instruction {
    private final String funcName;
    private final List<String> argExprs;

    public UserFunctionCall(String funcName, List<String> argExprs) {
        this.funcName = funcName;
        this.argExprs = argExprs;
    }
    public void execute(LightBot bot) {
        UserFunction func = bot.getFunction(funcName);
        if (func == null) return;
        List<Integer> argValues = new ArrayList<>();
        for (String expr : argExprs)
            argValues.add(bot.evalExpr(expr));
        bot.pushFrame(func.getParamNames(), argValues);
        for (Instruction instr : func.getInstructions())
            instr.execute(bot);
        bot.popFrame();
    }
}

// Bloque REPEAT parametrizado (REPEAT N)
class ParamRepeatBlock implements Instruction {
    private final String timesExpr;
    private final List<Instruction> instructions;

    public ParamRepeatBlock(String timesExpr, List<Instruction> instructions) {
        this.timesExpr = timesExpr;
        this.instructions = instructions;
    }
    public void execute(LightBot bot) {
        int n = bot.evalExpr(timesExpr);
        for (int i = 0; i < n; i++)
            for (Instruction instr : instructions)
                instr.execute(bot);
    }
}

public class LightBot {
    private char[][] initialMap;
    private char[][] map;
    private int width, height;
    private int startX, startY;
    private int robotX, robotY;
    private int startDir, robotDir;
    private final Map<String, UserFunction> functions = new HashMap<>();
    private final Deque<Map<String,Integer>> locals = new ArrayDeque<>();
    private static final int[] DX = {1, 0, -1, 0};
    private static final int[] DY = {0, 1, 0, -1};

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

    public void reset() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                map[y][x] = initialMap[y][x];
        robotX = startX;
        robotY = startY;
        robotDir = startDir;
        locals.clear();
    }

    public void runProgram(String[] programLines) {
        List<String> code = Arrays.asList(programLines);
        functions.clear();
        locals.clear();
        parseFunctions(code);
        locals.clear();
        List<Instruction> mainInstructions = parseInstructions(code, 0, code.size());
        for (Instruction instr : mainInstructions) {
            instr.execute(this);
        }
    }

    // Analiza las funciones primero
    private void parseFunctions(List<String> code) {
        for (int i = 0; i < code.size(); i++) {
            String line = code.get(i).trim();
            if (line.startsWith("FUNCTION ")) {
                String rest = line.substring(9).trim();
                String funcName;
                List<String> paramNames = new ArrayList<>();
                int parIdx = rest.indexOf('(');
                if (parIdx >= 0 && rest.endsWith(")")) {
                    funcName = rest.substring(0, parIdx).trim();
                    String paramList = rest.substring(parIdx + 1, rest.length() - 1).trim();
                    if (!paramList.isEmpty())
                        for (String p : paramList.split(","))
                            paramNames.add(p.trim());
                } else {
                    funcName = rest;
                }
                int start = i + 1;
                int end = start;
                while (end < code.size() && !code.get(end).trim().equals("ENDFUNCTION"))
                    end++;
                List<Instruction> funcInstructions = parseInstructions(code, start, end);
                functions.put(funcName, new UserFunction(funcName, paramNames, funcInstructions));
                i = end;
            }
        }
    }

    // Parser de instrucciones
    private List<Instruction> parseInstructions(List<String> code, int from, int to) {
        List<Instruction> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            String cmd = code.get(i).trim();
            if (cmd.isEmpty()) continue;
            if (cmd.startsWith("FUNCTION")) {
                while (i < to && !code.get(i).trim().equals("ENDFUNCTION")) i++;
                continue;
            }
            if (cmd.startsWith("REPEAT ")) {
                String countExpr = cmd.substring(7).trim();
                int depth = 1;
                int blockStart = ++i;
                while (i < to && depth > 0) {
                    String line = code.get(i).trim();
                    if (line.startsWith("REPEAT ")) depth++;
                    else if (line.equals("ENDREPEAT")) depth--;
                    if (depth > 0) i++;
                }
                List<Instruction> repeatBlock = parseInstructions(code, blockStart, i);
                result.add(new ParamRepeatBlock(countExpr, repeatBlock));
                continue;
            }
            if (cmd.equals("ENDREPEAT") || cmd.equals("ENDFUNCTION"))
                continue;
            if (cmd.startsWith("CALL ")) {
                String tail = cmd.substring(5).trim();
                String funcName;
                List<String> args = new ArrayList<>();
                int parIdx = tail.indexOf('(');
                if (parIdx >= 0 && tail.endsWith(")")) {
                    funcName = tail.substring(0, parIdx).trim();
                    String argList = tail.substring(parIdx+1, tail.length()-1).trim();
                    if (!argList.isEmpty())
                        for (String arg : argList.split(",")) args.add(arg.trim());
                } else {
                    funcName = tail;
                }
                result.add(new UserFunctionCall(funcName, args));
                continue;
            }
            result.add(new SimpleInstruction(cmd));
        }
        return result;
    }

    // Ejecuta una instrucción simple
    void doInstruction(String cmd) {
        switch (cmd) {
            case "FORWARD":
                int nx = robotX + DX[robotDir];
                int ny = robotY + DY[robotDir];
                if (nx < 0) nx = width - 1;
                if (nx >= width) nx = 0;
                if (ny < 0) ny = height - 1;
                if (ny >= height) ny = 0;
                if (map[ny][nx] == '.' || map[ny][nx] == 'O' || map[ny][nx] == 'X')
                {
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
                if (map[robotY][robotX] == '.') {
                    map[robotY][robotX] = 'x';
                } else if (map[robotY][robotX] == 'O') {
                    map[robotY][robotX] = 'X';
                }
                break;
        }
    }

    // Obtener función por nombre
    public UserFunction getFunction(String name) {
        return functions.get(name);
    }

    void pushFrame(List<String> paramNames, List<Integer> argValues) {
        Map<String,Integer> frame = new HashMap<>();
        for (int i = 0; i < paramNames.size(); i++)
            frame.put(paramNames.get(i), argValues.get(i));
        locals.push(frame);
    }
    void popFrame() {
        if (!locals.isEmpty())
            locals.pop();
    }
    int evalExpr(String expr) {
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException nfe) {
            for (Map<String,Integer> frame : locals)
                if (frame.containsKey(expr))
                    return frame.get(expr);
            throw new IllegalArgumentException("Parámetro o valor inválido: " + expr);
        }
    }

    public int[] getRobotPosition() {
        return new int[]{robotX, robotY};
    }

    public String[] getMap() {
        String[] result = new String[height];
        for (int y = 0; y < height; y++)
            result[y] = new String(map[y]);
        return result;
    }
}
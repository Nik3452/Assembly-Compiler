// Run with `java Main.java test`
// Version 2024-11-18
// Changes:
//  2024-12-04 Explicitly close output stream after file was assembled.
//  2024-11-18 Fix splitting of lines to be independent of OS
//  2024-11-18 First version
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static boolean iUsedAi() {
        return false;
    }

    public static String aiExplanation() {
        return "Relied on lecture PowerPoints and the course textbook: The Elements of Computing Systems";
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Main test | $file.nha");
            return;
        }

        String inputFile = args[0];
        if (!inputFile.endsWith(".nha") && !inputFile.equals("test")) {
            System.err.println("Unrecognized command or file type: " + inputFile);
            return;
        }

        if (inputFile.equals("test")) {
            test();
            return;
        }

        // looks like inputFile is indeed a file with an .nha ending
        int suffix = inputFile.lastIndexOf(".");
        String outputFile = inputFile.substring(0, suffix) + ".bin";
        try {
            Assembler asm = new Assembler(inputFile, outputFile);
            asm.assemble();
            asm.close();
        } catch (IOException ex) {
            System.err.println("Exception parsing: " + inputFile);
            System.err.println(ex.toString());
        }
    }

    private static void test() {
        String[] testNames = new String[]{"AInst21", "CInst", "Add"};
        String[] testInput = new String[]{
                TestInput.AInst21, TestInput.CInstAsm,
                TestInput.AddAsm};
        String[] testOutput = new String[]{
                TestOutput.AInst21Bin, TestOutput.CInstBin,
                TestOutput.AddBin};

        for (int i = 0; i < testNames.length; i += 1) {
            runTest(testNames[i], testInput[i].trim(), testOutput[i].trim());
        }

        System.out.println("\n");

        try {
            boolean usedAi = iUsedAi();
            System.out.println("I used AI: " + usedAi);
        } catch (RuntimeException ex) {
            System.err.println("Main.iUsedAi() method not yet adapted");
            System.err.println(ex.getMessage());
        }

        try {
            String reasoning = aiExplanation();
            System.out.println("My reasoning: " + reasoning);
        } catch (RuntimeException ex) {
            System.err.println("Main.aiExplanation() method not yet adapted");
            System.err.println(ex.getMessage());
        }
    }

    private static void runTest(String name, String input, String expected) {
        StringWriter output = new StringWriter();
        Assembler asm = new Assembler(input, output);

        try {
            asm.assemble();
        } catch (IOException ex) {
            System.err.println("Exception parsing test input for " + name);
            return;
        } catch (Throwable t) {
            System.err.println("Test failed with exception: " + name);
            System.err.println(t.toString());
            return;
        }

        String outputStr = output.toString().trim();

        if (expected.equals(outputStr)) {
            System.out.println("Test " + name + " passed.");
        } else {
            System.out.println("Test " + name + " failed.");
            printDiff(expected, outputStr, asm.getInput());
        }
    }

    private static void printDiff(String expected, String actual, List<String> input) {
        String[] expectedLines = expected.split("\n");
        String[] actualLines = actual.split("\n");

        int inputLine = 0;
        int i = 0;
        for (; i < expectedLines.length; i += 1, inputLine += 1) {
            while (inputLine < input.size() && input.get(inputLine).isEmpty()) {
                inputLine += 1;
            }

            String instruction = inputLine < input.size() ? input.get(inputLine) : "";

            if (actualLines.length <= i) {
                System.err.printf("line %3d: %s\t", i + 1, instruction);
                System.err.println(expectedLines[i] + " != missing");
                continue;
            }

            if (!expectedLines[i].equals(actualLines[i])) {
                System.err.printf("line %3d: %s\t", i + 1, instruction);
                System.err.println(expectedLines[i] + " != " + actualLines[i]);
            }
        }

        for (; i < actualLines.length; i += 1) {
            while (inputLine < input.size() && input.get(inputLine).isEmpty()) {
                inputLine += 1;
            }

            String instruction = inputLine < input.size() ? input.get(inputLine) : "";

            System.err.printf("line %3d: %s\t", i + 1, instruction);
            System.err.println(" != " + actualLines[i]);
        }
    }
}

class TestInput {
    public final static String AInst21 = "ldr A, $21";

    public final static String CInstAsm = """
            ldr D, (A)
            sub D, D, (A)
            jgt D
            ldr D, (A)
            jmp
            str (A), D
            """;

    public final static String AddAsm = """
            ldr A, $2
            ldr D, A
            ldr A, $3
            add D, D, A
            ldr A, $0
            str (A), D
            """;
}

class TestOutput {
    public static final String AInst21Bin = "0000000000010101";

    public final static String CInstBin = """
            1111110000010000
            1111010011010000
            1110001100000001
            1111110000010000
            1110101010000111
            1110001100001000
            """;

    public static final String AddBin = """
            0000000000000010
            1110110000010000
            0000000000000011
            1110000010010000
            0000000000000000
            1110001100001000""";
}

class Assembler {
    private final List<String> input;
    private final Writer output;


    private String currentInstruction;
    private String[] parts;
    private String binary = "";
    private int nextVariableAddress = 16;

    public Assembler(String inputFile, String outputFile) throws IOException {
        input = Files.readAllLines(Paths.get(inputFile));
        output = new PrintWriter(new FileWriter(outputFile));
    }

    public Assembler(String input, StringWriter output) {
        this.input = Arrays.asList(input.split("(\n)|(\r\n)|(\r)"));
        this.output = output;
    }

    private void output(String bin) throws IOException {
        output.write(bin);
        output.write('\n');
    }

    public void close() throws IOException {
        output.close();
    }

    public void assemble() throws IOException {
        Set<String> processedCommands = new HashSet<>();

        for (String line : input) {
            int commentStart = line.indexOf("//");
            if (commentStart >= 0) {
                line = line.substring(0, commentStart);
            }

            if (line.isEmpty()) continue;

            line = line.trim().toLowerCase();
            parts = line.split("\\s*,\\s*|\\s+");
            currentInstruction = parts[0];

            // Skip if the command has already been processed
            if (processedCommands.contains(line)) {
                continue;
            }

            executeInstructions();
            if (!binary.isEmpty()) {
                output(binary);
                processedCommands.add(line); // Add the command to the set of processed commands
            }
        }
        output.close();
    }

    private Map<String, String> getRegisters() {
        Map<String, String> registerMap = new HashMap<>();

        registerMap.put("a", "100");
        registerMap.put("d", "010");

        return registerMap;
    }

    private Map<String, String> getInstructions() {
        Map<String, String> instrMap = new HashMap<>();

        return instrMap;
    }


    private void setArray() throws IOException {
        for (int i = 0; i < input.size(); i++) {
            String line = input.get(i);

            int commentStart = line.indexOf("//");
            if (commentStart >= 0) {
                line = line.substring(0, commentStart);
            }

            if (line.isEmpty()) continue;

            line = line.trim().toLowerCase();
            input.set(i, line);

            parts = line.trim().split("\\s*,\\s*|\\s+");
            currentInstruction = parts[0];

            executeInstructions();
            if (!binary.isEmpty()) {
                output(binary);
            }

        }
    }

    private void executeInstructions() throws IOException {
        binary = ""; // Reset binary string
        switch (currentInstruction) {
            case "ldr":
                handleLdr();
                break;

            case "str":
                handleStr();
                break;

            case "add":
            case "sub":
                handleAddSub();
                break;

            case "jmp":
                binary = "1110101010000111";
                break;

            case "jgt":
            case "jeq":
            case "jge":
            case "jlt":
            case "jne":
            case "jle":
                handleJmp();
                break;

            default:
                return;
        }
    }

    private void handleLdr() {
        System.out.println(Arrays.toString(parts));
        System.out.println(parts[1].startsWith("("));
        System.out.println(parts[1]);
        if (parts[1].startsWith("(") || (parts[1].startsWith("d") && parts[2].startsWith("$"))) {
            return;
        }

        if (isInteger(parts[2])) {
            return;
        }

        if (parts[2].startsWith("$")) {
            // Handle A-instruction
            int value = Integer.parseInt(parts[2].substring(1));
            if (value < 0 || value > 32767) {
                return;
            }
            // Binary representation
            binary = String.format("%016d", Long.parseLong(Integer.toBinaryString(value)));
        } else {
            // C-instruction for ldr
            String target = getRegisters().get(parts[1]);
            String src = parts[2];

            if (src.startsWith("(") && src.endsWith(")")) {
                src = src.substring(1, src.length() - 1);
                binary = "1111";
            } else {
                binary = "1110";
            }
            if (src.equals("d")) {
                binary += "001100";
            } else {
                binary += "110000";
            }
            binary += target;
            binary += "000";
        }
    }

    private void handleStr() {
        if (parts[1].startsWith("(") && parts[1].endsWith(")")) {
            String register = parts[1].substring(1, parts[1].length() - 1);
            if (!register.equals("a") && !register.equals("d")) {
                return; // Invalid register, do nothing
            }
        } else {
            return; // Invalid format, do nothing
        }

        binary = "1110"; // Base binary for str instruction

        if (parts[2].equals("d")) {
            binary += "0011";
        } else {
            binary += "1100";
        }

        // destination
        binary += "0000";
        // jump
        binary += "1000";
    }

    private void handleAddSub() {
        String target = getRegisters().get(parts[1]);
        String src2 = parts[3];
        if (src2.startsWith("(") && src2.endsWith(")")) {
            binary = "1111";
        } else {
            binary = "1110";
        }
        binary += currentInstruction.equals("add") ? "000010" : "010011";
        binary += target;
        binary += "000";
    }

    private void handleJmp() {
        String src2 = parts[1];
        String jBits = getInstBits().get(currentInstruction);

        if (src2.startsWith("(") && src2.endsWith(")")) {
            src2 = src2.substring(1, src2.length() - 1);
            binary = "1111";
        } else {
            binary = "1110";
        }

        if (src2.contains("d")) {
            binary += "0011";
        } else {
            binary += "1100";
        }
        binary += "0000";
        binary += jBits;
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }


    static Map<String, String> getInstBits() {
        Map<String, String> instBits = new HashMap<>();
        instBits.put("ldr", "1110");
        instBits.put("str", "0000");
        instBits.put("add", "0010");
        instBits.put("sub", "0110");
        instBits.put("jgt", "0001");
        instBits.put("jeq", "0010");
        instBits.put("jge", "0011");
        instBits.put("jlt", "0100");
        instBits.put("jne", "0101");
        instBits.put("jle", "0110");

        return instBits;
    }


    public List<String> getInput() {
        return input;
    }
}
package ru.fizteh.fivt.students.yushkevichAnton;

import java.io.*;
import java.util.*;
 
public class WordCounter {
    public static void main(String[] args) {
        Mode mode = Mode.WORD_COUNTER;
        Persnicketiness persnicketiness = Persnicketiness.ALL;
        boolean appendResult = false;
 
        int start = 0;
        for (; start < args.length && args[start].startsWith("-"); start++) {
            String s = args[start];
            for (int j = 1; j < s.length(); j++) {
                char c = s.charAt(j);
                switch (c) {
                    case 'l':
                        mode = Mode.LINE_COUNTER;
                        break;
                    case 'w':
                        mode = Mode.WORD_COUNTER;
                        break;
                    case 'u':
                        persnicketiness = Persnicketiness.UNIQUE;
                        break;
                    case 'U':
                        persnicketiness = Persnicketiness.UNIQUE_ANYCASE;
                        break;
                    case 'a':
                        appendResult = true;
                        break;
                    default:
                        System.err.println("Unknown key " + c + ".");
                }
            }
        }
 
        if (start == args.length) {
            System.err.println("At least one file argument required.");
            return;
        }
 
        if (appendResult) {
            process(Arrays.copyOfRange(args, start, args.length), mode, persnicketiness);
        } else {
            for (int j = start; j < args.length; j++) {
                process(new String[] {args[j]}, mode, persnicketiness);
            }
        }
    }
 
    private static void process(String[] filenames, Mode mode, Persnicketiness persnicketiness) {
        if (filenames.length == 0) {
            System.err.println("At least one file argument required.");
            return;
        }
        if (filenames.length == 1) {
            System.out.println(filenames[0] + ":");
        }
 
        Counter counter = new Counter(persnicketiness);
        for (String filename : filenames) {
            SuperScanner superScanner;
            try {
                superScanner = new SuperScanner(filename);
            } catch (FileNotFoundException e) {
                System.err.println("Could not open file.");
                continue;
            }
            while (true) {
                String s = null;
                switch (mode) {
                    case WORD_COUNTER:
                        s = superScanner.nextWord();
                        break;
                    case LINE_COUNTER:
                        s = superScanner.nextLine();
                        break;
                }
                if (s == null) {
                    break;
                }
                counter.add(s);
            }
        }
        System.out.println(counter);
    }
 
}
 
class SuperScanner {
    private BufferedReader  bufferedReader;
    private StringTokenizer stringTokenizer;
    private static String delimiters = " \t\n\r\f,.!?:;";
 
    public SuperScanner(String filename) throws FileNotFoundException {
        bufferedReader = new BufferedReader(new FileReader(filename));
    }
 
    public String nextWord() {
        return hasMoreTokens() ? stringTokenizer.nextToken() : null;
    }
 
    public String nextLine() {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            return null;
        }
    }
 
    boolean hasMoreTokens() {
        while (stringTokenizer == null || !stringTokenizer.hasMoreTokens()) {
            String s = nextLine();
            if (s == null) {
                return false;
            }
            stringTokenizer = new StringTokenizer(s, delimiters);
        }
        return true;
    }
}
 
enum Mode {
    WORD_COUNTER,
    LINE_COUNTER
}
 
enum Persnicketiness {
    ALL,
    UNIQUE,
    UNIQUE_ANYCASE
}
 
class Counter {
    private final Persnicketiness persnicketiness;
 
    public Counter(Persnicketiness persnicketiness) {
        this.persnicketiness = persnicketiness;
    }
 
    private final HashMap<String, Integer> map = new HashMap<String, Integer>();
    private int count;
 
    @Override
    public String toString() {
        if (persnicketiness == Persnicketiness.ALL) {
            return Integer.toString(count);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            stringBuilder.append(entry.getKey());
            stringBuilder.append(" ");
            stringBuilder.append(Integer.toString(entry.getValue()));
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
 
    public void add(String string) {
        if (persnicketiness == Persnicketiness.ALL) {
            count++;
            return;
        }
        if (persnicketiness == Persnicketiness.UNIQUE_ANYCASE) {
            string = string.toLowerCase();
        }
        Integer res = map.get(string);
        if (res == null) {
            map.put(string, 1);
        } else {
            map.put(string, res + 1);
        }
    }
}
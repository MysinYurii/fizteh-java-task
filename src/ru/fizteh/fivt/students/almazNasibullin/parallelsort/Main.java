package ru.fizteh.fivt.students.almazNasibullin.parallelsort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * 23.10.12
 * @author almaz
 */

public class Main {

    public static void main(String[] args) {
        // соответствует ключу -i
        WrapperPrimitive<Boolean> withoutReg = new WrapperPrimitive<Boolean>(false);

        // соответствует ключу -u
        WrapperPrimitive<Boolean> uniqLines = new WrapperPrimitive<Boolean>(false);

        // кол-во потоков
        WrapperPrimitive<Integer> countThreads = new WrapperPrimitive<Integer>(0);

        // имя файла, в который необходимо вывести результат
        WrapperPrimitive<String> outputFile = new WrapperPrimitive<String>("");

        // файлы для считывания
        List<String> files = new ArrayList<String>();

        // строки из входных файлов или из stdin
        List<String> lines;
        
        if (args.length > 0) {
            readArguments(args, withoutReg, uniqLines, countThreads, outputFile, files);
        }
        
        // считывание строк
        lines = readLines(files, uniqLines, outputFile);
        
        files.clear();

        int size = lines.size();
        if (size == 0) {
            System.exit(0);
        }

        int countOfLinesForOneThread = 2500;

        if (countThreads.t == 0) {
            if (size > countOfLinesForOneThread) {
                // каждый поток сортирует примерно countOfLinesForOneThread строк
                countThreads.t = size / countOfLinesForOneThread + 1;
            }
        } else {
            if (size / countThreads.t < countOfLinesForOneThread) {
                // если заданное число потоков слишком большое, то используем меньшее кол-во
                countThreads.t = size / countOfLinesForOneThread + 1;
            }
        }

        if (countThreads.t > 1) {
            // каждый поток сортирует свою часть и  записывает результат в result
            List<List<String> > result;

            // работа с потоками: запуск и ожидание завершения их работы

            result = startingThreads(lines, countThreads, withoutReg);

            // результат слияния полученных результатов всех потоков
            List<String> res = MergeResult(result, withoutReg);

            printResult(uniqLines, outputFile, res);
            res.clear();
        } else {
            // работа без запуска дополнительных потоков по причине малого
            // количества входных строчек

            if (withoutReg.t) {
                Collections.sort(lines, String.CASE_INSENSITIVE_ORDER);
            } else {
                Collections.sort(lines);
            }

            printResult(uniqLines, outputFile, lines);
            lines.clear();
        }
    }
    
    public static void checkOrderArguments(WrapperPrimitive<Boolean> key,
            List<String> files) {
        if (files.isEmpty()) {
            // предпологается, что все ключи должны стоять перед 
            // именами файлов, из которых будет считываться информация 
            key.t = true;
        } else {
            LoUtils.printErrorAndExit("Bad order. Usage: [-iu] [-t THREAD_COUNT] [-o OUTPUT] [FILES...]");
        }
    }

    public static void readArguments(String[] args, WrapperPrimitive<Boolean> withoutReg,
            WrapperPrimitive<Boolean> uniqLines, WrapperPrimitive<Integer> countThreads,
            WrapperPrimitive<String> outputFile, List<String> files) {
        StringBuilder sb = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; ++i) {
            sb.append(" ");
            sb.append(args[i]);
        }
        StringTokenizer st = new StringTokenizer(sb.toString(), " \t");
        while (st.hasMoreTokens()) {
            String str = st.nextToken();
            if (str.equals("-i")) {
                checkOrderArguments(withoutReg, files);
            } else if (str.equals("-u")) {
                checkOrderArguments(uniqLines, files);
            } else if (str.equals("-ui") || str.equals("-iu")) {
                checkOrderArguments(withoutReg, files);
                checkOrderArguments(uniqLines, files);
            } else if (str.equals("-t")) {
                if (files.isEmpty()) {
                    if (st.hasMoreTokens()) {
                        try {
                            countThreads.t = Integer.parseInt(st.nextToken());
                        } catch (Exception e) {
                            LoUtils.printErrorAndExit("Usage: [-t THREAD_COUNT]." + e.getMessage());
                        }
                    } else {
                        LoUtils.printErrorAndExit("Usage: [-t THREAD_COUNT]");
                    }
                } else {
                    LoUtils.printErrorAndExit("Bad order. Usage: [-iu] [-t THREAD_COUNT]"
                        + "[-o OUTPUT] [FILES...]");
                }
            } else if (str.equals("-o")) {
                if (files.isEmpty()) {
                    if (st.hasMoreTokens()) {
                        outputFile.t = st.nextToken();
                    } else {
                        LoUtils.printErrorAndExit("Usage: [-o OUTPUT]");
                    }
                } else {
                    LoUtils.printErrorAndExit("Bad order. Usage: [-iu] [-t THREAD_COUNT]"
                        + "[-o OUTPUT] [FILES...]");
                }
            } else {
                files.add(str);
            }
        }
    }

    public static void readFromBufferedReader(BufferedReader br, boolean fromConsole,
            FileReader fr, List <String> lines, WrapperPrimitive<Boolean> uniqLines,
            WrapperPrimitive<String> outputFile) {
        try {
            String str;
            boolean allLinesEmpty = true;
            while ((str = br.readLine()) != null) {
                lines.add(str);
                if (allLinesEmpty && !str.isEmpty()) {
                    allLinesEmpty = false;
                }
            }
            if (allLinesEmpty) {
                lines.add("");
                printResult(uniqLines, outputFile, lines);
            }
        } catch (Exception e) {
            LoUtils.printErrorAndExit("Bad reading from BufferedReader: " + e.getMessage());
        } finally {
            if (!fromConsole) {
                LoUtils.closeOrExit(fr);
            }
            LoUtils.closeOrExit(br);
        }
    }

    public static List<String> readLines(List<String> files,
            WrapperPrimitive<Boolean> uniqLines,
            WrapperPrimitive<String> outputFile) {
        BufferedReader br = null;
        List<String> lines = new ArrayList<String>();
        if (files.isEmpty()) { // считывание из stdin
            try {
                 br = new BufferedReader(new InputStreamReader(System.in));
                 readFromBufferedReader(br, true, null, lines, uniqLines, outputFile);
            } catch (Exception e) {
                LoUtils.printErrorAndExit("Bad opening BufferedReader: " + e.getMessage());
            } finally {
                LoUtils.closeOrExit(br);
            }
        } else { // считывание из файлов
            for (int i = 0; i < files.size(); ++i) {
                FileReader fr = null;
                try {
                    fr = new FileReader(new File(files.get(i)));
                    br = new BufferedReader(fr);
                    readFromBufferedReader(br, false, fr, lines, uniqLines, outputFile);
                } catch (Exception e) {
                    LoUtils.printErrorAndExit("Bad opening BufferedReader: " +
                            e.getMessage());
                } finally {
                    LoUtils.closeOrExit(br);
                }
            }
        }
        return lines;
    }

    public static List<List<String> > startingThreads(List<String> lines,
            WrapperPrimitive<Integer> countThreads, WrapperPrimitive<Boolean> withoutReg) {
        List<List<String> > result = new ArrayList<List<String> >();

        for (int i = 0; i < countThreads.t - 1; ++i) {
            result.add(new ArrayList<String>());
        }
        try{
            List<Thread> threads = new ArrayList<Thread>(countThreads.t);
            int size = lines.size();
            int start = 0;
            int end = size / (countThreads.t - 1) - 1;
            
            for (int i = 0; i < countThreads.t - 1; ++i) {
                Sorter s = new Sorter(lines, start, end, result.get(i), withoutReg.t);
                threads.add(new Thread(s));
                threads.get(i).start();
                start = end + 1;
                end += size / (countThreads.t - 1);
            }
            if (start != size) { // последний поток сортирует оставшуюся последнюю
                // часть массива line, ее размер может быть меньше size / (countThreads.t - 1)
                result.add(new ArrayList<String>());
                Sorter s = new Sorter(lines, start, size - 1, result.get(countThreads.t - 1),
                        withoutReg.t);
                threads.add(new Thread(s));
                threads.get(countThreads.t - 1).start();
            }

            for (int i = 0; i < threads.size(); ++i) {
                try {
                    threads.get(i).join();
                } catch (Exception e) {
                    LoUtils.printErrorAndExit("Bad joining: " + e.getMessage());
                }
            }
            lines.clear();
        } catch (Exception e) {
            LoUtils.printErrorAndExit("Smth bad happened while threads were working: " +
                    e.getMessage());
        }
        return result;
    }

    public static void removeAndAdd(List<List<String> > result, List<String> res,
            TreeMap<String, List<Pair> > tm) {
        // достаем строчку наименьшую из tm и добавляем в res
        String cur = tm.firstKey();
        res.add(cur);
        Pair p = tm.get(cur).get(0);
        tm.get(cur).remove(p);
        if (tm.get(cur).isEmpty()) {
            tm.remove(cur);
        }
        if (p.second + 1 < result.get(p.first).size()) {
            String toAdd = result.get(p.first).get(p.second + 1);
            if (!tm.containsKey(toAdd)) {
                tm.put(toAdd, new ArrayList<Pair>());
            }
            tm.get(toAdd).add(new Pair(p.first, p.second + 1));
        }
    }

    public static List<String> MergeResult(List<List<String> > result,
            WrapperPrimitive<Boolean> withoutReg) {
        // res - результат слияния result
        List<String> res = new ArrayList<String>();
        
        // хранит в качестве ключа входные слова, в качестве значения массив
        // пар координаты в двумерном массиве result
        TreeMap<String, List<Pair> > tm;
        if (withoutReg.t) {
            tm = new TreeMap<String, List<Pair> >(new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    String s1 = (String)o1;
                    String s2 = (String)o2;
                    if (s1.equals(s2)) {
                        return 0;
                    }
                    if (s1.compareToIgnoreCase(s2) == 0) {
                        return s1.compareTo(s2);
                    }
                    return s1.compareToIgnoreCase(s2);
                }
            });
        } else {
            tm = new TreeMap<String, List<Pair> >();
        }
        
        for (int i = 0; i < result.size(); ++i) {
            String cur = result.get(i).get(0);
            if (!tm.containsKey(cur)) {
                tm.put(cur, new ArrayList<Pair>());
            }
            tm.get(cur).add(new Pair(i, 0));
        }
        while (!tm.isEmpty()) {
            removeAndAdd(result, res, tm);
        }
        tm.clear();
        return res;
    }

    public static void writeLineToBufferedWriter(BufferedWriter bw,
            List<String> lines, int i, boolean withLineBreak) {
        try {
            if (withLineBreak) {
                bw.newLine();
                bw.write(lines.get(i), 0, lines.get(i).length());
            } else {
                bw.write(lines.get(i), 0, lines.get(i).length());
            }
            bw.flush();
        } catch (IOException e) {
            LoUtils.printErrorAndExit("Bad writing to BufferedWriter: " + e.getMessage());
        }
    }

    public static void printResult(WrapperPrimitive<Boolean> uniqLines,
             WrapperPrimitive<String> outputFile, List<String> lines) {
        int size = lines.size();
        if (outputFile.t.equals("")) { // вывод в stdout
            if (uniqLines.t) { // вывод уникальных строк
                String prev = lines.get(0).toLowerCase();
                System.out.println(lines.get(0));
                for (int i = 1; i < size; ++i) {
                    if (!lines.get(i).toLowerCase().equals(prev)) {
                        System.out.println(lines.get(i));
                        prev = lines.get(i).toLowerCase();
                    }
                }
             } else {
                for (int i = 0; i < size; ++i) {
                    System.out.println(lines.get(i));
                }
            }
        } else { // вывод в файл
            BufferedWriter bw = null;
            FileWriter fw = null;
            try {
                fw = new FileWriter(new File(outputFile.t));
                bw = new BufferedWriter(fw);
                if (uniqLines.t) { // вывод уникальных строк
                    String prev = lines.get(0).toLowerCase();
                    bw.write(lines.get(0), 0, lines.get(0).length());
                    bw.flush();
                    for (int i = 1; i < size; ++i) {
                        if (!lines.get(i).toLowerCase().equals(prev)) {
                            writeLineToBufferedWriter(bw, lines, i, true);
                            prev = lines.get(i).toLowerCase();
                        }
                    }
                } else {
                    writeLineToBufferedWriter(bw, lines, 0,false);
                    for (int i = 1; i < size; ++i) {
                        writeLineToBufferedWriter(bw, lines, i, true);
                    }
                }
            } catch (Exception e) {
                LoUtils.printErrorAndExit(e.getMessage());
            } finally {
                LoUtils.closeOrExit(fw);
                LoUtils.closeOrExit(bw);
            }
        }
    }
}

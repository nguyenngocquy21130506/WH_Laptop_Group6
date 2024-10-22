package com.group6.warehouse.script;

import java.io.*;

public class ExecutePython {
    public static void main(String[] args) {
        try {
            String pythonFilePath = "path/to/your/script.py";

            ProcessBuilder processBuilder = new ProcessBuilder("python", pythonFilePath);
            processBuilder.redirectErrorStream(true); // Gộp luồng lỗi vào luồng đầu ra

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
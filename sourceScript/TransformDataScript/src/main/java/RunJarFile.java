import java.io.IOException;

public class RunJarFile {
    public static void main(String[] args) {
        // Đường dẫn tới file JAR bạn muốn thực thi
        String jarFilePath = "D:/workspace/DataWare/TransformDataScript/target/TransformDataScript-1.0-SNAPSHOT.jar";

        // Lệnh để chạy file JAR
        String command = "java -jar " + jarFilePath;

        try {
            // Sử dụng ProcessBuilder để thực thi lệnh
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.inheritIO(); // Để kế thừa các đầu vào và đầu ra của tiến trình (tùy chọn)
            Process process = processBuilder.start(); // Bắt đầu tiến trình

            // Chờ tiến trình kết thúc
            int exitCode = process.waitFor();
            System.out.println("Exit Code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

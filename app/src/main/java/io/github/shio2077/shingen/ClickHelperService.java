package io.github.shio2077.shingen;

import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClickHelperService extends IAdbClickService.Stub {

    @Override
    public void destroy() throws RemoteException {
        // Since the service is running in a separate process, we exit the process when the service is destroyed.
        System.exit(0);
    }

    @Override
    public void exit() throws RemoteException {
        destroy();
    }

    @Override
    public String execArr(String[] command) throws RemoteException {
        try {
            // 执行shell命令
            Process process = Runtime.getRuntime().exec(command);
            // 读取执行结果
            return readResult(process);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取执行结果，如果有异常会向上抛
     */
    private String readResult(Process process) throws IOException, InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        inputStreamReader.close();
        process.waitFor();
        return stringBuilder.toString();
    }
}

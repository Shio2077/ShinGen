package io.github.shio2077.shingen;

import android.os.RemoteException;

import io.github.shio2077.shingen.IAdbClickService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClickHelperService extends IAdbClickService.Stub {

    @Override
    public void destroy() throws RemoteException {
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
    public String readResult(Process process) throws IOException, InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        // 读取执行结果
        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        inputStreamReader.close();
        process.waitFor();
        return stringBuilder.toString();
    }
}

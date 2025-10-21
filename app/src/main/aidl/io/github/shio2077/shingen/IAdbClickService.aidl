package io.github.shio2077.shingen;

interface IAdbClickService {
    String execArr(in String[] command);
    void destroy();
    void exit();
}

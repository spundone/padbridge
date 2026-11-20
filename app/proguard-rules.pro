# Started by app_process as root ("Control this phone"), not by Android: keep its entry point by name.
-keep class dev.padbridge.RootInputServer {
    public static void main(java.lang.String[]);
}

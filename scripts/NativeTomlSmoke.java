import fun.bm.mili.rust.RustBridge;
import java.nio.file.Files;
import java.nio.file.Path;

// 由 Java 25 直接运行，不依赖开发工作区或系统预装的原生库。
class NativeTomlSmoke {
    public static void main(String[] args) throws Exception {
        String file = args[0];
        RustBridge.load();
        Files.writeString(Path.of(file), "# retained\nnumber = 1\n");
        if (!RustBridge.configSaveMerge(file, "{\"number\":42}")) {
            throw new AssertionError("JNI 写入失败");
        }
        String value = RustBridge.configGetValue(file, "number");
        if (!"42".equals(value) || !Files.readString(Path.of(file)).contains("# retained")) {
            throw new AssertionError("TOML 往返校验失败：" + value);
        }
        System.out.println("通过：" + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
    }
}

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.Properties;
import java.util.stream.Stream;


public class Main {
    public static void main(String[] args) {

        Properties properties = new Properties();
        try {
            String patchProperties = new File("data.properties").getAbsolutePath();
            properties.load(new FileReader(patchProperties));
        } catch (IOException e) {
            System.out.println("\"data.properties\" не найден");
            return;
        }
        System.out.println("data.properties Найден");

        String unpackingDirectory = properties.getProperty("unpackingDirectory");
        String clearDirectoryBeforeLoad = properties.getProperty("clearDirectoryBeforeLoad");
        System.out.println("unpackingDirectory = " + unpackingDirectory);
        System.out.println("clearDirectoryBeforeLoad = " + clearDirectoryBeforeLoad);

        if (unpackingDirectory.isEmpty() || unpackingDirectory.equals(" ")) {
            System.out.println("неверно указан unpackingDirectory в \"data.properties\"");
            return;
        }
        if (Boolean.parseBoolean(clearDirectoryBeforeLoad)) {
            try {
                recursiveDelete(Path.of(unpackingDirectory));
            } catch (IOException e) {
                System.out.println("неверно указан unpackingDirectory в \"data.properties\"");
                return;
            }
            System.out.println("директория очищена");
        }

        URL url = getURL();
        if (url == null) {
            System.out.println("URL не найден");
            return;
        }
        System.out.println("URL найден = " + url);

        String[] elementURL = url.getPath().split("/");
        String nameArchive = elementURL[elementURL.length - 1];
        Path allPathArchive = Path.of(unpackingDirectory + "\\" + nameArchive);
        System.out.println("Имя архива = " + nameArchive);

        System.out.println("Начало скачивания, ожидайте...");
        try {
            downloadFile(url, allPathArchive);
        } catch (IOException e){
            System.out.println("URL не отвечает");
            return;
        }
        System.out.println("Архив скачан, распаковываем в " + unpackingDirectory);

        unpackZip(allPathArchive.toString(), unpackingDirectory);

        System.out.println("Разархивация прошла успешно");
    }

    public static void recursiveDelete(Path path) throws IOException {
            Stream<Path> pathStream = Files.list(path);
            for (Path p : pathStream.toList()) {
                if (Files.isDirectory(p))
                    recursiveDelete(p);
                Files.delete(p);
            }

    }

    static URL getURL() {
        try {
            Document document = null;
            document = Jsoup.connect("http://btep.org.ru/").get();
            Elements elements = document.select("body > table > tbody > tr:nth-child(1) > td > div:nth-child(4) > div");
            String[] strings = elements.toString().split("<br>");
            return new URL(strings[7]);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println("Нет подключения к сайту");
            return null;
        }
    }

    public static void downloadFile(URL url, Path path) throws IOException {
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            fileChannel.transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    public static void unpackZip(String zipPath, String beginPatch) {
        if (Files.exists(Path.of(zipPath)))
            ZipUtil.unpack(new File(zipPath), new File(beginPatch));
        else System.out.println("Архив не найден");
    }

}




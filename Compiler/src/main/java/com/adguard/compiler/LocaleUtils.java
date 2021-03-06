/**
 * This file is part of Adguard Browser Extension (https://github.com/AdguardTeam/AdguardBrowserExtension).
 * <p>
 * Adguard Browser Extension is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Adguard Browser Extension is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with Adguard Browser Extension.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.adguard.compiler;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for working with locales
 */
public class LocaleUtils {

    public enum SupportedLocales {

        EN("en"), // English
        RU("ru"), // Russian
        DE("de"), // German
        TR("tr"), // Turkish
        UK("uk"), // Ukrainian
        PL("pl"), // Polish
        PT_BR("pt_BR"), // Portuguese (Brazil)
        PT_PT("pt_PT"), // Portuguese (Portugal)
        KO("ko"), // Korean
        zh_CN("zh_CN"), // Chinese (China)
        SR("sr"), // Serbian
        FR("fr"), // French
        SK("sk"), // Slovak
        HY("hy"), // Armenian
        ES_419("es_419"), // Spanish (Latin American)
        ES("es"), // Spanish
        IT("it"), // Italian
        ID("id"); // Indonesian

        private String code;

        SupportedLocales(String code) {
            this.code = code;
        }

        public static boolean supported(String code) {
            for (SupportedLocales locale : values()) {
                if (locale.code.equalsIgnoreCase(code)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void updateExtensionNameForChromeLocales(File dest, String extensionNamePostfix) throws IOException {

        if (StringUtils.isEmpty(extensionNamePostfix)) {
            return;
        }

        File chromeLocalesDir = new File(dest, "_locales");

        File[] files = chromeLocalesDir.listFiles();
        if (files == null) {
            throw new IOException("Unable to fetch list files from " + chromeLocalesDir);
        }

        for (File file : files) {

            File chromeLocaleFile = new File(file, "messages.json");

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(chromeLocaleFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\r\n");
                    if (line.trim().startsWith("\"name\":") || line.trim().startsWith("\"short_name\":")) {
                        line = reader.readLine();
                        String[] parts = StringUtils.split(line, ":");
                        String message = StringUtils.removeEnd(parts[1].trim(), "\"") + extensionNamePostfix + "\"";
                        sb.append("\t\"message\": ").append(message).append("\r\n");
                    }
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }

            FileUtils.writeStringToFile(chromeLocaleFile, sb.toString(), "utf-8");
        }
    }

    public static void convertFromChromeToFirefoxLocales(File chromeLocalesDir) throws IOException {

        File[] files = chromeLocalesDir.listFiles();
        if (files == null) {
            throw new IOException("Unable to fetch list files from " + chromeLocalesDir);
        }

        // Populate collection of messages for EN locale
        Map<String, String> enLocaleMessages = readMessagesToMap(new File(chromeLocalesDir, "en/messages.json"));

        for (File file : files) {

            File chromeLocaleFile = new File(file, "messages.json");
            if (!SupportedLocales.supported(file.getName())) {
                FileUtils.deleteQuietly(file);
                continue;
            }

            File firefoxLocalFile = new File(chromeLocalesDir, StringUtils.replace(file.getName(), "_", "-") + "/");
            firefoxLocalFile.mkdirs();
            firefoxLocalFile = new File(firefoxLocalFile, "messages.properties");

            Map<String, String> localeMessages = readMessagesToMap(chromeLocaleFile);

            Set<String> msgIds = enLocaleMessages.keySet();

            StringBuilder sb = new StringBuilder();
            for (String msgId : msgIds) {
                String message = localeMessages.get(msgId);
                if (message == null) {
                    // Use en message
                    message = enLocaleMessages.get(msgId);
                }
                message = message.replaceAll("\n", "\\\\n");
                sb.append(msgId).append("=").append(message);
                sb.append(System.lineSeparator());
            }

            FileUtils.writeStringToFile(firefoxLocalFile, sb.toString(), "utf-8");
            FileUtils.deleteQuietly(chromeLocaleFile);
            if (StringUtils.contains(file.getName(), "_")) {
                FileUtils.deleteQuietly(file);
            }
        }
    }

    public static void writeLocalesToChromeManifest(File dest) throws IOException {

        File chromeManifest = new File(dest, "chrome.manifest");

        StringBuilder sb = new StringBuilder("\n");
        for (SupportedLocales locale : SupportedLocales.values()) {
            sb.append("\nlocale adguard ").append(locale.code.replace("_", "-")).append(" ./chrome/locale/").append(locale.code.replace("_", "-")).append("/");
        }

        String content = FileUtils.readFileToString(chromeManifest, "utf-8");
        content += sb.toString();
        FileUtils.writeStringToFile(chromeManifest, content, "utf-8");
    }

    public static void writeLocalesToFirefoxInstallRdf(File source, File dest, String extensionNamePostfix) throws IOException {
//		<em:localized>
//		<Description>
//		<em:locale>en</em:locale>
//		<em:name>Adguard AdBlocker</em:name>
//		<em:description>Adguard AdBlocker</em:description>
//		</Description>
//		</em:localized>
        File installManifest = new File(dest, "install.rdf");
        if (!installManifest.exists()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (SupportedLocales locale : SupportedLocales.values()) {
            File localeFile = new File(source, "_locales/" + locale.code + "/messages.json");
            Map<String, String> messages = readMessagesToMap(localeFile);
            String name = messages.get("name") + extensionNamePostfix;
            String description = messages.get("description");
            sb.append("<em:localized>").append(System.lineSeparator());
            sb.append("\t<Description>").append(System.lineSeparator());
            sb.append("\t\t<em:locale>").append(locale.code).append("</em:locale>").append(System.lineSeparator());
            sb.append("\t\t<em:name>").append(name).append("</em:name>").append(System.lineSeparator());
            sb.append("\t\t<em:description>").append(description).append("</em:description>").append(System.lineSeparator());
            sb.append("\t</Description>").append(System.lineSeparator());
            sb.append("</em:localized>").append(System.lineSeparator());
        }

        String content = FileUtils.readFileToString(installManifest, "utf-8");
        content = StringUtils.replace(content, "${localised}", sb.toString());
        FileUtils.writeStringToFile(installManifest, content, "utf-8");
    }

    private static Map<String, String> readMessagesToMap(File localeFile) throws IOException {
        @SuppressWarnings("unchecked") Map<String, Map> m = objectMapper.readValue(FileUtils.readFileToByteArray(localeFile), Map.class);
        Map<String, String> messages = new LinkedHashMap<String, String>();
        for (String msgId : m.keySet()) {
            String message = (String) m.get(msgId).get("message");
            messages.put(msgId, message);
        }
        return messages;
    }

}

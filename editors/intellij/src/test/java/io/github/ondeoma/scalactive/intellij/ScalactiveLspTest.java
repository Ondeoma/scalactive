package io.github.ondeoma.scalactive.intellij;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.platform.lsp.api.LspServerSupportProvider;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * 言語サーバーの登録と起動のテスト.
 */
public class ScalactiveLspTest extends BasePlatformTestCase {

    public void testServerSupportProviderIsRegistered() {
        ExtensionPointName<LspServerSupportProvider> ep =
                ExtensionPointName.create("com.intellij.platform.lsp.serverSupportProvider");
        assertTrue(
                "LSP extension is not registered",
                ep.getExtensionList().stream().anyMatch(p -> p instanceof ScalactiveLspServerSupportProvider)
        );
    }

    public void testSupportedFiles() {
        ScalactiveLspServerDescriptor d = new ScalactiveLspServerDescriptor(getProject());
        assertTrue(d.isSupportedFile(new LightVirtualFile("Counter.st", "")));
        assertFalse(d.isSupportedFile(new LightVirtualFile("Counter.scala", "")));
    }

    /** 同梱したjarを、IDEのJavaで起動し、LSPのinitializeに応答することを確認します. */
    public void testServerStartsAndInitializes() throws Exception {
        GeneralCommandLine cl = new ScalactiveLspServerDescriptor(getProject()).createCommandLine();
        assertTrue(Files.isRegularFile(ScalactiveLspServerDescriptor.serverJar()));
        Process p = cl.createProcess();
        try {
            String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"capabilities\":{}}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            OutputStream out = p.getOutputStream();
            out.write(("Content-Length: " + bytes.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.write(bytes);
            out.flush();
            String response = readMessage(p.getInputStream());
            assertTrue(response, response.contains("\"completionProvider\""));
            assertTrue(response, response.contains("scalactive-language-server"));
        } finally {
            p.destroy();
            p.waitFor(10, TimeUnit.SECONDS);
        }
    }

    private static String readMessage(InputStream in) throws Exception {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        while (!header.toString(StandardCharsets.US_ASCII).endsWith("\r\n\r\n")) {
            int b = in.read();
            if (b < 0) fail("server closed the stream: " + header);
            header.write(b);
        }
        String h = header.toString(StandardCharsets.US_ASCII);
        int length = Integer.parseInt(h.replaceAll("(?s).*Content-Length: *(\\d+).*", "$1"));
        return new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }

}
